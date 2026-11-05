import { type FormEvent, useEffect, useState } from 'react'
import { api, assetUrl, clearSession, saveSession, sessionToken } from './api.ts'
import { FinancePie } from './FinancePie.tsx'
import { PaymentReceipt } from './receipt.tsx'

type Config = {
  kiborPercent: number
  downPaymentOptions: number[]
  tenureMonths: number[]
  quotationMaxAgeDays: number
}
type Choice = {
  id: number
  name?: string
  title?: string
  price?: number
  city?: string
  partyId?: number
  description?: string
  imageIds?: number[]
}
type Plan = {
  tenureMonths: number
  annualRatePercent: number
  monthlyInstallment: number
  totalPayable: number
  downPaymentAmount: number
  financedAmount: number
}
type Rule = { documentCode: string; documentLabel: string }
type Application = {
  referenceNo: string
  fullName?: string
  status: string
  displayStatus?: string
  paymentStatus: string
  paymentReference?: string
  paymentAmount?: number
  paidAt?: string
  paymentFromWallet?: string
  paymentToWallet?: string
  collectionWallet?: string
  payerWallet?: string
  merchantName?: string
  productImageId?: number
  employmentType?: string
  productTitle?: string
  productPrice?: number
  tenureMonths?: number
  requestedDownPaymentPercent?: number
  indicativeKiborPercent?: number
  indicativeMonthlyInstallment?: number
  approvedAnnualRate?: number
  approvedMonthlyInstallment?: number
  approvedDownPaymentAmount?: number
  approvedFinancedAmount?: number
  approvedTotalPayable?: number
  biometricRef?: string
  dbrNote?: string
  documents?: { documentCode: string; originalName: string }[]
  events?: { eventType: string; actorLabel?: string; remark?: string; createdAt?: string }[]
}
type Notice = { title: string; body: string; createdAt?: string }
type Step = 'identity' | 'category' | 'product' | 'plan' | 'papers' | 'status'

const EDITABLE = new Set(['DRAFT', 'ELIGIBILITY_FAILED', 'DISCREPANT'])
const money = (value?: number) => value == null ? '—' : `Rs ${Number(value).toLocaleString('en-PK', { maximumFractionDigits: 2 })}`
const statusLabel = (app: Application) => (app.displayStatus || (app.paymentStatus === 'PAID' ? 'PAID' : app.status)).replaceAll('_', ' ')

export default function App() {
  const [session, setSession] = useState<string | null>(() => sessionToken())
  const [step, setStep] = useState<Step>(sessionToken() ? 'category' : 'identity')
  const [config, setConfig] = useState<Config | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [name, setName] = useState('')
  const [cnic, setCnic] = useState('')
  const [mobile, setMobile] = useState('')
  const [mpin, setMpin] = useState('')
  const [otp, setOtp] = useState('')
  const [otpCode, setOtpCode] = useState('')
  const [categories, setCategories] = useState<Choice[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [merchants, setMerchants] = useState<Choice[]>([])
  const [partyId, setPartyId] = useState('')
  const [products, setProducts] = useState<Choice[]>([])
  const [productId, setProductId] = useState('')
  const [employmentType, setEmploymentType] = useState('SALARY')
  const [income, setIncome] = useState('')
  const [existing, setExisting] = useState('0')
  const [down, setDown] = useState('')
  const [tenure, setTenure] = useState('')
  const [quotationDate, setQuotationDate] = useState('')
  const [plans, setPlans] = useState<Plan[]>([])
  const [rules, setRules] = useState<Rule[]>([])
  const [application, setApplication] = useState<Application | null>(null)
  const [notices, setNotices] = useState<Notice[]>([])
  const [confirmDown, setConfirmDown] = useState('')

  useEffect(() => {
    api<Config>('/api/public/financing/config').then(setConfig).catch((err: Error) => setError(err.message))
  }, [])

  useEffect(() => {
    if (!session) return
    api<Choice[]>('/api/public/financing/categories').then(setCategories).catch(() => undefined)
    api<Application[]>('/api/public/financing/applications')
      .then(async (rows) => {
        const current = rows[0]
        if (!current) {
          setStep('category')
          return
        }
        setApplication(current)
        if (current.employmentType) {
          setEmploymentType(current.employmentType)
          setRules(await api<Rule[]>(`/api/public/financing/document-rules?employmentType=${current.employmentType}`))
        }
        setStep(EDITABLE.has(current.status) ? 'papers' : 'status')
      })
      .catch(() => {
        clearSession()
        setSession(null)
        setStep('identity')
      })
    api<Notice[]>('/api/public/financing/notifications').then(setNotices).catch(() => undefined)
  }, [session])

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setError('')
    try {
      await action()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed')
    } finally {
      setBusy(false)
    }
  }

  const editable = !!application && EDITABLE.has(application.status)
  const selectedProduct = products.find((item) => String(item.id) === productId)
  const selectedMerchant = merchants.find((item) => String(item.partyId) === partyId)
  const steps: Step[] = ['identity', 'category', 'product', 'plan', 'papers', 'status']
  const stepIndex = steps.indexOf(step) + 1
  const showApproved = !!application && (
    application.status === 'APPROVED'
    || application.status === 'DOWN_PAYMENT_SELECTED'
    || application.paymentStatus === 'PAID'
  )

  return (
    <main className="app">
      <header className="mast">
        <div className="brand">Zindigi</div>
        <h1>EV bike financing</h1>
        <p>A financing application against your existing Zindigi wallet.</p>
      </header>
      <div className="sheet">
      <div className="progress">
        <div className="progress-meta">
          <span>Step {stepIndex} of {steps.length}</span>
          <strong>{label(step)}</strong>
        </div>
        <div className="track" aria-hidden="true"><i style={{ width: `${(stepIndex / steps.length) * 100}%` }} /></div>
      </div>
      {error && <p className="error">{error}</p>}

      {step === 'identity' && (
        <form className="card" onSubmit={(e) => submitIdentity(e)}>
          <p className="kicker">Applicant</p>
          <h2>Your details</h2>
          <p className="lede">Confirm this mobile with the code. The down payment is paid later with the wallet MPIN.</p>
          <label>Full name</label>
          <input required value={name} onChange={(e) => setName(e.target.value)} disabled={!!otpCode} />
          <label>CNIC</label>
          <input required value={cnic} onChange={(e) => setCnic(e.target.value)} placeholder="13 digits" inputMode="numeric" disabled={!!otpCode} />
          <label>Mobile</label>
          <input required value={mobile} onChange={(e) => setMobile(e.target.value)} placeholder="03XXXXXXXXX" inputMode="tel" disabled={!!otpCode} />
          {otpCode && (
            <>
              <div className="otp-show">
                <span>Your code</span>
                <strong>{otpCode}</strong>
              </div>
              <label>Type the code</label>
              <input required value={otp} onChange={(e) => setOtp(e.target.value)} inputMode="numeric" />
            </>
          )}
          <div className="actions">
            <button disabled={busy} type="submit">{otpCode ? 'Continue' : 'Send code'}</button>
            {otpCode && (
              <button className="ghost" type="button" onClick={() => { setOtpCode(''); setOtp('') }}>Edit details</button>
            )}
          </div>
        </form>
      )}

      {step === 'category' && (
        <section className="card">
          <p className="kicker">Catalogue</p>
          <h2>Choose a category</h2>
          <p className="lede">Pick the bike category and the dealer first. Products open on the next page.</p>
          <label>Category</label>
          <select required value={categoryId} onChange={(e) => pickCategory(e.target.value)}>
            <option value="">Select</option>
            {categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
          <label>Dealer</label>
          <select required value={partyId} onChange={(e) => void pickDealer(e.target.value)}>
            <option value="">Select</option>
            {merchants.map((item) => (
              <option key={item.partyId} value={item.partyId}>{item.name}{item.city ? ` · ${item.city}` : ''}</option>
            ))}
          </select>
          <div className="actions">
            <button type="button" disabled={!categoryId || !partyId || products.length === 0} onClick={() => setStep('product')}>
              See products
            </button>
          </div>
        </section>
      )}

      {step === 'product' && (
        <section className="card">
          <p className="kicker">{selectedMerchant?.name || 'Dealer'}</p>
          <h2>Choose a product</h2>
          <p className="lede">Select one bike from this dealer.</p>
          <div className="product-grid">
            {products.map((item) => {
              const imageId = item.imageIds?.[0]
              const selected = String(item.id) === productId
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`product-card ${selected ? 'selected' : ''}`}
                  onClick={() => setProductId(String(item.id))}
                >
                  {imageId ? (
                    <img src={assetUrl(`/api/public/financing/images/${imageId}`)} alt={item.title || 'Bike'} />
                  ) : (
                    <div className="product-card-ph">EV</div>
                  )}
                  <strong>{item.title}</strong>
                  <span>{money(item.price)}</span>
                  {item.description && <em>{item.description}</em>}
                </button>
              )
            })}
          </div>
          {selectedProduct && (
            <div className="price-banner">
              <span>{selectedProduct.title}</span>
              <strong>{money(selectedProduct.price)}</strong>
            </div>
          )}
          <div className="actions">
            <button className="ghost" type="button" onClick={() => setStep('category')}>Back</button>
            <button type="button" disabled={!productId} onClick={() => setStep('plan')}>See plans</button>
          </div>
        </section>
      )}

      {step === 'plan' && (
        <form className="card" onSubmit={(e) => saveApplication(e)}>
          <p className="kicker">Indicative terms</p>
          <h2>Your plan</h2>
          <label>Employment</label>
          <select value={employmentType} onChange={(e) => setEmploymentType(e.target.value)}>
            <option value="SALARY">Salary</option>
            <option value="CORPORATE_REFERRED">Corporate referred</option>
            <option value="SELF_EMPLOYED">Self employed</option>
          </select>
          <label>Monthly income</label>
          <input required value={income} onChange={(e) => setIncome(e.target.value)} inputMode="decimal" />
          <label>Existing monthly installments</label>
          <input required value={existing} onChange={(e) => setExisting(e.target.value)} inputMode="decimal" />
          <label>Down payment</label>
          <select required value={down} onChange={(e) => setDown(e.target.value)}>
            <option value="">Select</option>
            {(config?.downPaymentOptions ?? []).map((value) => <option key={value} value={value}>{value}%</option>)}
          </select>
          <label>Tenure</label>
          <select required value={tenure} onChange={(e) => setTenure(e.target.value)}>
            <option value="">Select</option>
            {(config?.tenureMonths ?? []).map((value) => <option key={value} value={value}>{value} months</option>)}
          </select>
          <label>Quotation date</label>
          <input required type="date" value={quotationDate} onChange={(e) => setQuotationDate(e.target.value)} />
          <p className="muted">Within {config?.quotationMaxAgeDays ?? 7} days. Plans below use KIBOR {config?.kiborPercent}% only.</p>
          <div className="actions">
            <button className="ghost" type="button" onClick={() => setStep('product')}>Back</button>
            <button className="ghost" type="button" disabled={busy || !productId || !down} onClick={() => run(loadPlans)}>Show plans</button>
            <button disabled={busy} type="submit">Save</button>
          </div>
          {plans.map((plan) => (
            <article className="offer" key={plan.tenureMonths}>
              <div>
                <span>Monthly installment</span>
                <strong>{money(plan.monthlyInstallment)}</strong>
                <em>{plan.tenureMonths} months · {plan.annualRatePercent}% a year</em>
              </div>
              <dl>
                <div><dt>Down payment</dt><dd>{money(plan.downPaymentAmount)}</dd></div>
                <div><dt>Financed</dt><dd>{money(plan.financedAmount)}</dd></div>
                <div><dt>Installments total</dt><dd>{money(plan.totalPayable)}</dd></div>
              </dl>
            </article>
          ))}
        </form>
      )}

      {step === 'papers' && application && (
        <section className="card">
          <p className="kicker">{application.referenceNo}</p>
          <h2>Documents</h2>
          <div className="pills"><span>{statusLabel(application)}</span></div>
          {application.dbrNote && <p className="muted">{application.dbrNote}</p>}
          <div className="doc-board">
            {rules.map((rule) => {
              const uploaded = application.documents?.some((doc) => doc.documentCode === rule.documentCode)
              return (
                <div className={`doc-tile ${uploaded ? 'done' : ''}`} key={rule.documentCode}>
                  <div>
                    <strong>{rule.documentLabel}</strong>
                    <span>{uploaded ? 'Uploaded' : 'Required'}</span>
                  </div>
                  <input
                    type="file"
                    accept="application/pdf,image/*"
                    title={`Upload ${rule.documentLabel}`}
                    onChange={(e) => {
                      const file = e.target.files?.[0]
                      if (file) void run(() => upload(rule.documentCode, file))
                    }}
                  />
                </div>
              )
            })}
          </div>
          <div className="actions">
            {editable && <button className="ghost" type="button" onClick={() => setStep('plan')} title="Go back and change income or tenure">Edit plan</button>}
            <button type="button" disabled={busy} onClick={() => run(submitApplication)} title="Send this application to operations">
              Submit
            </button>
          </div>
        </section>
      )}

      {step === 'status' && application && (
        <section className="card">
          <p className="kicker">{application.referenceNo}</p>
          <h2>Application</h2>
          <div className="pills">
            <span className={application.paymentStatus === 'PAID' ? 'good' : ''}>{statusLabel(application)}</span>
            {application.paymentStatus !== 'PAID' && (
              <span>{application.paymentStatus.replaceAll('_', ' ')}</span>
            )}
          </div>
          <div className="figures">
            <div><span>Bike</span><strong>{application.productTitle}</strong></div>
            <div><span>Dealer</span><strong>{application.merchantName || '—'}</strong></div>
            <div><span>Price</span><strong>{money(application.productPrice)}</strong></div>
            {!showApproved && (
              <div><span>KIBOR installment</span><strong>{money(application.indicativeMonthlyInstallment)}</strong></div>
            )}
            {showApproved && (
              <>
                <div><span>Approved rate</span><strong>{application.approvedAnnualRate}%</strong></div>
                <div><span>Down payment</span><strong>{money(application.approvedDownPaymentAmount)}</strong></div>
                <div><span>Financed</span><strong>{money(application.approvedFinancedAmount)}</strong></div>
                <div><span>Monthly</span><strong>{money(application.approvedMonthlyInstallment)}</strong></div>
                <div><span>Installments total</span><strong>{money(application.approvedTotalPayable)}</strong></div>
              </>
            )}
          </div>
          {showApproved && (
            <FinancePie downPayment={application.approvedDownPaymentAmount} financed={application.approvedFinancedAmount} />
          )}
          {(application.events || []).length > 0 && (
            <ol className="app-timeline">
              {(application.events || []).map((event) => (
                <li key={`${event.eventType}-${event.createdAt}`}>
                  <time>{event.createdAt ? new Date(event.createdAt).toLocaleString() : ''}</time>
                  <strong>{(event.eventType || '').replaceAll('_', ' ')}</strong>
                  {event.remark && <p>{event.remark}</p>}
                </li>
              ))}
            </ol>
          )}
          {application.status === 'APPROVED' && application.paymentStatus !== 'PAID' && (
            <form onSubmit={(e) => confirmPayment(e)}>
              <label>Down payment</label>
              <select required value={confirmDown} onChange={(e) => setConfirmDown(e.target.value)}>
                <option value="">Select</option>
                {(config?.downPaymentOptions ?? []).map((value) => <option key={value} value={value}>{value}%</option>)}
              </select>
              <div className="actions">
                <button disabled={busy} type="submit">Confirm plan</button>
              </div>
              <p className="muted">This confirms the plan. It does not pay the bank.</p>
            </form>
          )}
          {application.status === 'DOWN_PAYMENT_SELECTED' && application.paymentStatus !== 'PAID' && (
            <form onSubmit={(e) => payDownPayment(e)}>
              <p className="muted">From {application.payerWallet || '••••8666'} to {application.collectionWallet || '••••0917'}</p>
              <label>Wallet MPIN</label>
              <input required type="password" inputMode="numeric" autoComplete="off" value={mpin} onChange={(e) => setMpin(e.target.value)} />
              <div className="actions">
                <button disabled={busy} type="submit">Pay down payment</button>
              </div>
              <p className="muted">This sends only the down payment to the bank collection account. The MPIN is not saved.</p>
            </form>
          )}
          {application.paymentStatus === 'PAID' && (
            <PaymentReceipt data={{
              receiptNo: application.paymentReference || application.referenceNo,
              applicationNo: application.referenceNo,
              customerName: application.fullName || '',
              merchantName: application.merchantName,
              productTitle: application.productTitle || '',
              productImageUrl: application.productImageId
                ? assetUrl(`/api/public/financing/images/${application.productImageId}`)
                : undefined,
              amount: application.paymentAmount,
              fromWallet: application.paymentFromWallet,
              toWallet: application.paymentToWallet,
              paidAt: application.paidAt,
            }} />
          )}
        </section>
      )}

      {notices.length > 0 && step !== 'identity' && (
        <section className="card">
          <p className="kicker">Activity</p>
          <h2>Updates</h2>
          {notices.map((note) => (
            <div className="note" key={`${note.title}-${note.createdAt}`}>
              <strong>{note.title}</strong>
              <div>{note.body}</div>
            </div>
          ))}
        </section>
      )}

      {session && (
        <div className="actions">
          <button className="ghost" type="button" onClick={() => {
            clearSession()
            setSession(null)
            setApplication(null)
            setStep('identity')
            setOtpCode('')
            setOtp('')
          }}>
            Sign out
          </button>
        </div>
      )}
      </div>
    </main>
  )

  function submitIdentity(e: FormEvent) {
    e.preventDefault()
    void run(async () => {
      if (!otpCode) {
        const sent = await api<{ otp?: string }>('/api/public/financing/identify', {
          method: 'POST',
          body: JSON.stringify({ fullName: name, cnic, mobile }),
        }, null)
        setOtpCode(sent.otp || '')
        if (!sent.otp) setError('The code was not returned. Restart the app against the updated API.')
        return
      }
      const verified = await api<{ sessionToken: string }>('/api/public/financing/otp/verify', {
        method: 'POST',
        body: JSON.stringify({ cnic, mobile, code: otp }),
      }, null)
      saveSession(verified.sessionToken)
      setSession(verified.sessionToken)
      setStep('category')
    })
  }

  async function pickCategory(id: string) {
    setCategoryId(id)
    setPartyId('')
    setProductId('')
    setProducts([])
    setMerchants([])
    if (!id) return
    setMerchants(await api<Choice[]>(`/api/public/financing/categories/${id}/merchants`))
  }

  async function pickDealer(id: string) {
    setPartyId(id)
    setProductId('')
    setProducts([])
    if (!id || !categoryId) return
    setProducts(await api<Choice[]>(`/api/public/financing/merchants/${id}/products?categoryId=${categoryId}`))
  }

  async function loadPlans() {
    const quoted = await api<Plan[]>('/api/public/financing/quote', {
      method: 'POST',
      body: JSON.stringify({ productId, downPaymentPercent: down }),
    })
    const prefer = new Set([12, 24])
    const dual = quoted.filter((plan) => prefer.has(plan.tenureMonths))
    setPlans(dual.length >= 2 ? dual : quoted)
  }

  function saveApplication(e: FormEvent) {
    e.preventDefault()
    void run(async () => {
      const saved = await api<Application>('/api/public/financing/applications', {
        method: 'POST',
        body: JSON.stringify({
          productId,
          employmentType,
          monthlyIncome: income,
          existingInstallments: existing,
          downPaymentPercent: down,
          tenureMonths: tenure,
          quotationDate,
        }),
      })
      setApplication(saved)
      setRules(await api<Rule[]>(`/api/public/financing/document-rules?employmentType=${employmentType}`))
      setStep('papers')
    })
  }

  async function upload(documentCode: string, file: File) {
    if (!application) return
    const body = new FormData()
    body.append('documentCode', documentCode)
    body.append('file', file)
    setApplication(await api<Application>(`/api/public/financing/applications/${application.referenceNo}/documents`, {
      method: 'POST',
      body,
    }))
  }

  async function submitApplication() {
    if (!application) return
    const submitted = await api<Application>(`/api/public/financing/applications/${application.referenceNo}/submit`, {
      method: 'POST',
    })
    setApplication(submitted)
    setNotices(await api<Notice[]>('/api/public/financing/notifications'))
    setStep(EDITABLE.has(submitted.status) ? 'papers' : 'status')
  }

  function confirmPayment(e: FormEvent) {
    e.preventDefault()
    if (!application) return
    void run(async () => {
      setApplication(await api<Application>(`/api/public/financing/applications/${application.referenceNo}/down-payment`, {
        method: 'POST',
        body: JSON.stringify({ percent: confirmDown }),
      }))
    })
  }

  function payDownPayment(e: FormEvent) {
    e.preventDefault()
    if (!application) return
    void run(async () => {
      const paid = await api<Application>(`/api/public/financing/applications/${application.referenceNo}/pay`, {
        method: 'POST',
        body: JSON.stringify({ mpin }),
      })
      setMpin('')
      setApplication(paid)
      setNotices(await api<Notice[]>('/api/public/financing/notifications'))
    })
  }
}

function label(step: Step) {
  if (step === 'identity') return 'You'
  if (step === 'category') return 'Category'
  if (step === 'product') return 'Product'
  if (step === 'plan') return 'Plan'
  if (step === 'papers') return 'Papers'
  return 'Status'
}
