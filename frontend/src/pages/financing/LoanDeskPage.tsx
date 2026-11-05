import { Fragment, useEffect, useState } from 'react';
import { Navigate, useParams } from 'react-router-dom';
import { api, apiUrl } from '../../lib/api';
import { useAuth } from '../../auth/AuthContext';
import { FinancePie } from '../../components/FinancePie';
import { Tip } from '../../components/Tip';
import { PaymentReceipt } from './receipt';

type EventRow = { eventType: string; actorLabel: string; remark?: string; createdAt?: string };
type Doc = { id: number; documentCode: string; originalName: string };
type Application = {
  referenceNo: string;
  status: string;
  fullName?: string;
  cnic?: string;
  mobile?: string;
  merchantName?: string;
  productTitle?: string;
  productPrice?: number;
  tenureMonths?: number;
  requestedDownPaymentPercent?: number;
  indicativeKiborPercent?: number;
  indicativeMonthlyInstallment?: number;
  bankCommissionPercent?: number;
  approvedAnnualRate?: number;
  approvedMonthlyInstallment?: number;
  approvedDownPaymentAmount?: number;
  approvedFinancedAmount?: number;
  approvedTotalPayable?: number;
  biometricRef?: string;
  biometricSource?: string;
  otpVerifiedAt?: string;
  ecibStatus?: string;
  riskRatingStatus?: string;
  dbrPercent?: number;
  dbrPassed?: boolean;
  dbrNote?: string;
  opsRemarks?: string;
  checkerRemarks?: string;
  paymentStatus?: string;
  displayStatus?: string;
  paymentReference?: string;
  paymentAmount?: number;
  paymentFromWallet?: string;
  paymentToWallet?: string;
  paidAt?: string;
  productImageId?: number;
  documents?: Doc[];
  events?: EventRow[];
  createdAt?: string;
};

const money = (value?: number) => value == null
  ? '—'
  : `Rs ${Number(value).toLocaleString('en-PK', { maximumFractionDigits: 2 })}`;

export function LoanDeskPage() {
  const { session } = useAuth();
  const params = useParams();
  const [rows, setRows] = useState<Application[]>([]);
  const [selected, setSelected] = useState<Application | null>(null);
  const [suggested, setSuggested] = useState('');
  const [remarks, setRemarks] = useState('');
  const [error, setError] = useState('');
  const roles = session?.portalRoles || [];
  const ops = roles.includes('FINANCING_OPS');
  const checker = roles.includes('FINANCING_CHECKER');

  async function load(reference?: string) {
    if (!session?.token) return;
    const list = await api<Application[]>('/api/financing/desk/applications', { token: session.token });
    setRows(list);
    const ref = reference || params.reference;
    if (ref) {
      setSelected(await api<Application>(`/api/financing/desk/applications/${ref}`, { token: session.token }));
    }
    const cfg = await api<{ suggestedBankCommissionPercent?: number }>('/api/financing/desk/config', { token: session.token });
    if (cfg.suggestedBankCommissionPercent != null) setSuggested(String(cfg.suggestedBankCommissionPercent));
  }

  useEffect(() => {
    load().catch((err) => setError(err.message));
  }, [session?.token, params.reference]);

  if (!session) return <Navigate to="/login" replace />;
  if (!ops && !checker && session.role !== 'PLATFORM_ADMIN') return <Navigate to="/dashboard" replace />;

  async function act(path: string, body: unknown) {
    if (!session?.token || !selected) return;
    setError('');
    try {
      const updated = await api<Application>(path, {
        method: 'POST',
        token: session.token,
        body: JSON.stringify(body),
      });
      setSelected(updated);
      setRemarks('');
      await load(updated.referenceNo);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed');
    }
  }

  async function openDoc(id: number) {
    if (!session?.token) return;
    const res = await fetch(apiUrl(`/api/financing/desk/documents/${id}/file`), {
      headers: { Authorization: `Bearer ${session.token}` },
    });
    if (!res.ok) {
      setError('Could not open the document');
      return;
    }
    const blob = await res.blob();
    window.open(URL.createObjectURL(blob));
  }

  const waiting = rows.filter((row) => row.status === 'SUBMITTED' || row.status === 'WITH_CHECKER').length;
  const paid = rows.filter((row) => row.paymentStatus === 'PAID').length;
  const story = (selected?.events || []).filter((event) => event.eventType !== 'DOCUMENT');

  return (
    <div className="page fin-shell">
      <div className="container dash">
        <div className="panel panel--wide desk">
          <header className="desk-top">
            <div>
              <p className="desk-kicker">Loan desk</p>
              <h2>Applications</h2>
              <p className="desk-caption">Company commission {suggested || 'from setup'}% applies to every case. Operations forwards. A different checker approves.</p>
            </div>
            <div className="desk-counts">
              <div><strong>{rows.length}</strong><span>In queue</span></div>
              <div><strong>{waiting}</strong><span>Need a decision</span></div>
              <div><strong>{paid}</strong><span>Paid</span></div>
            </div>
          </header>
          {error && <p className="error">{error}</p>}
          <div className="table-wrap">
            <table className="data-table desk-queue">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Customer</th>
                  <th>Product</th>
                  <th>Price</th>
                  <th>Tenure</th>
                  <th>Status</th>
                  <th>Payment</th>
                  <th>Case</th>
                </tr>
              </thead>
              <tbody>
                {rows.length === 0 && (
                  <tr><td colSpan={8}>No applications yet.</td></tr>
                )}
                {rows.map((row) => (
                  <Fragment key={row.referenceNo}>
                    <tr className={selected?.referenceNo === row.referenceNo ? 'selected' : ''}>
                      <td className="desk-mono">{row.referenceNo}</td>
                      <td>{row.fullName}</td>
                      <td>{row.productTitle}</td>
                      <td>{money(row.productPrice)}</td>
                      <td>{row.tenureMonths ? `${row.tenureMonths} mo` : '—'}</td>
                      <td><StatusPill value={row.displayStatus || row.status} paymentStatus={row.paymentStatus} /></td>
                      <td>{row.paymentStatus === 'PAID' ? <PayPill value="PAID" /> : <PayPill value={row.paymentStatus} />}</td>
                      <td>
                        <Tip label={selected?.referenceNo === row.referenceNo ? 'Hide the case file' : 'Open the case file below this row'}>
                          <button
                            className="btn btn-ghost btn-sm"
                            type="button"
                            onClick={() => {
                              if (selected?.referenceNo === row.referenceNo) {
                                setSelected(null);
                              } else {
                                load(row.referenceNo).catch((err) => setError(err.message));
                              }
                            }}
                          >
                            {selected?.referenceNo === row.referenceNo ? 'Hide' : 'View'}
                          </button>
                        </Tip>
                      </td>
                    </tr>
                    {selected?.referenceNo === row.referenceNo && (
                      <tr className="desk-expand-row">
                        <td colSpan={8}>
            <article className="desk-case desk-case--inline">
              <header className="desk-case-top">
                <div>
                  <p className="desk-kicker">{selected.referenceNo}</p>
                  <h3>{selected.fullName || 'Customer'}</h3>
                  <dl className="desk-id">
                    <div><dt>CNIC</dt><dd>{selected.cnic || '—'}</dd></div>
                    <div><dt>Mobile</dt><dd>{localPhone(selected.mobile)}</dd></div>
                    <div><dt>Dealer</dt><dd>{selected.merchantName || '—'}</dd></div>
                  </dl>
                </div>
                <div className="desk-status">
                  <StatusPill value={selected.displayStatus || selected.status} paymentStatus={selected.paymentStatus} />
                  {selected.paymentStatus !== 'PAID' && <PayPill value={selected.paymentStatus} />}
                  <Tip label="Download the approved plan as a PDF">
                    <button className="btn btn-ghost btn-sm" type="button" onClick={() => downloadPlanPdf(selected)}>Plan PDF</button>
                  </Tip>
                </div>
              </header>

              <section className="desk-block desk-offer-row">
                <div>
                  <h4>Offer</h4>
                  <div className="fin-metrics">
                    <Metric label="Bike" value={selected.productTitle || '—'} />
                    <Metric label="Price" value={money(selected.productPrice)} />
                    <Metric label="Tenure" value={selected.tenureMonths ? `${selected.tenureMonths} months` : '—'} />
                    <Metric label="Down payment" value={selected.requestedDownPaymentPercent != null ? `${selected.requestedDownPaymentPercent}%` : '—'} />
                    <Metric label="KIBOR installment" value={money(selected.indicativeMonthlyInstallment)} />
                    <Metric label="Approved rate" value={selected.approvedAnnualRate != null ? `${selected.approvedAnnualRate}%` : '—'} />
                    <Metric label="Monthly" value={money(selected.approvedMonthlyInstallment)} />
                    <Metric label="Due now" value={money(selected.approvedDownPaymentAmount)} />
                    <Metric label="Financed" value={money(selected.approvedFinancedAmount)} />
                    <Metric label="Installments total" value={money(selected.approvedTotalPayable)} />
                  </div>
                </div>
                <FinancePie downPayment={selected.approvedDownPaymentAmount} financed={selected.approvedFinancedAmount} />
              </section>

              <section className="desk-block">
                <h4>Checks</h4>
                <div className="desk-facts">
                  <Fact label="DBR" value={selected.dbrPercent != null ? `${Number(selected.dbrPercent).toLocaleString('en-PK', { maximumFractionDigits: 2 })}%` : '—'} tone={selected.dbrPassed == null ? undefined : selected.dbrPassed ? 'ok' : 'bad'} />
                  <Fact label="eCIB" value={labelize(selected.ecibStatus)} />
                  <Fact label="Risk rating" value={labelize(selected.riskRatingStatus)} />
                  <Fact label="OTP" value={selected.otpVerifiedAt ? 'Verified' : 'Missing'} tone={selected.otpVerifiedAt ? 'ok' : 'wait'} />
                  <Fact label="Identity" value={selected.biometricSource ? labelize(selected.biometricSource) : 'Missing'} tone={selected.biometricRef ? 'ok' : 'wait'} />
                </div>
                {selected.dbrNote && <p className="desk-note">{selected.dbrNote}</p>}
              </section>

              <div className="desk-split">
                <section className="desk-block">
                  <h4>Documents</h4>
                  {(selected.documents || []).length === 0 && <p className="desk-empty">No documents on this case.</p>}
                  <ul className="desk-docs">
                    {(selected.documents || []).map((doc) => (
                      <li key={doc.id}>
                        <button type="button" onClick={() => openDoc(doc.id)}>
                          <strong>{labelize(doc.documentCode)}</strong>
                          <span>{doc.originalName}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </section>
                <section className="desk-block">
                  <h4>History</h4>
                  {story.length === 0 && <p className="desk-empty">No events yet.</p>}
                  <ol className="desk-timeline">
                    {story.map((event) => (
                      <li key={`${event.eventType}-${event.createdAt}`}>
                        <time>{event.createdAt ? new Date(event.createdAt).toLocaleString() : ''}</time>
                        <strong>{labelize(event.eventType)}</strong>
                        <span>{prettyActor(event.actorLabel)}</span>
                        {event.remark && <p>{prettyRemark(event.remark)}</p>}
                      </li>
                    ))}
                  </ol>
                </section>
              </div>

              {(selected.opsRemarks || selected.checkerRemarks) && (
                <section className="desk-block">
                  <h4>Remarks on file</h4>
                  <div className="desk-quotes">
                    {selected.opsRemarks && <blockquote><span>Operations</span>{selected.opsRemarks}</blockquote>}
                    {selected.checkerRemarks && <blockquote><span>Checker</span>{selected.checkerRemarks}</blockquote>}
                  </div>
                </section>
              )}

              <PaidVsCurrentDesk app={selected} />

              {selected.paymentStatus === 'PAID' && (
                <PaymentReceipt data={{
                  receiptNo: selected.paymentReference || selected.referenceNo,
                  applicationNo: selected.referenceNo,
                  customerName: selected.fullName || '',
                  merchantName: selected.merchantName,
                  productTitle: selected.productTitle || '',
                  productImageUrl: selected.productImageId
                    ? apiUrl(`/api/public/financing/images/${selected.productImageId}`)
                    : undefined,
                  amount: selected.paymentAmount,
                  fromWallet: selected.paymentFromWallet,
                  toWallet: selected.paymentToWallet,
                  paidAt: selected.paidAt,
                }} />
              )}

              {ops && selected.status === 'SUBMITTED' && (
                <Decision
                  remarks={remarks}
                  setRemarks={setRemarks}
                  onPrimary={() => act(`/api/financing/desk/applications/${selected.referenceNo}/forward`, { remarks })}
                  primary="Forward to checker"
                  onSecond={() => act(`/api/financing/desk/applications/${selected.referenceNo}/ops`, { decision: 'DISCREPANT', remarks })}
                  second="Send back"
                  onThird={() => act(`/api/financing/desk/applications/${selected.referenceNo}/ops`, { decision: 'REJECT', remarks })}
                  third="Reject"
                />
              )}
              {checker && selected.status === 'WITH_CHECKER' && (
                <Decision
                  remarks={remarks}
                  setRemarks={setRemarks}
                  onPrimary={() => act(`/api/financing/desk/applications/${selected.referenceNo}/checker`, { decision: 'APPROVE', remarks })}
                  primary="Approve"
                  onThird={() => act(`/api/financing/desk/applications/${selected.referenceNo}/checker`, { decision: 'REJECT', remarks })}
                  third="Reject"
                />
              )}
            </article>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

function downloadPlanPdf(app: Application) {
  const lines = [
    ['Reference', app.referenceNo],
    ['Customer', app.fullName || '-'],
    ['Dealer', app.merchantName || '-'],
    ['Product', app.productTitle || '-'],
    ['Price', money(app.productPrice)],
    ['Tenure', app.tenureMonths ? `${app.tenureMonths} months` : '-'],
    ['Approved rate', app.approvedAnnualRate != null ? `${app.approvedAnnualRate}%` : '-'],
    ['Down payment', money(app.approvedDownPaymentAmount)],
    ['Financed', money(app.approvedFinancedAmount)],
    ['Monthly', money(app.approvedMonthlyInstallment)],
    ['Installments total', money(app.approvedTotalPayable)],
    ['Status', app.displayStatus || app.status],
  ];
  const body = lines.map(([k, v]) => `${k}: ${v}`).join('\n');
  const stream = [
    'BT /F1 16 Tf 40 780 Td (Financing plan) Tj ET',
    'BT /F1 10 Tf 40 750 Td (Zindigi Corporate loan desk) Tj ET',
    ...body.split('\n').map((line, index) => `BT /F1 11 Tf 40 ${720 - index * 22} Td (${pdfEscape(line)}) Tj ET`),
  ].join('\n');
  const encoder = new TextEncoder();
  const objects = [
    '1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n',
    '2 0 obj << /Type /Pages /Count 1 /Kids [3 0 R] >> endobj\n',
    '3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 420 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n',
    `4 0 obj << /Length ${encoder.encode(stream).length} >> stream\n${stream}\nendstream endobj\n`,
    '5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n',
  ];
  let pdf = '%PDF-1.4\n';
  const offsets = [0];
  for (const object of objects) {
    offsets.push(encoder.encode(pdf).length);
    pdf += object;
  }
  const xrefAt = encoder.encode(pdf).length;
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  for (let i = 1; i < offsets.length; i += 1) pdf += `${String(offsets[i]).padStart(10, '0')} 00000 n \n`;
  pdf += `trailer << /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefAt}\n%%EOF`;
  const blob = new Blob([encoder.encode(pdf)], { type: 'application/pdf' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = `plan-${app.referenceNo}.pdf`;
  link.click();
  URL.revokeObjectURL(link.href);
}

function pdfEscape(value: string) {
  return value.replace(/[^\x20-\x7E]/g, '*').replace(/[\\()]/g, (char) => `\\${char}`);
}

function PaidVsCurrentDesk({ app }: { app: Application }) {
  if (app.paymentStatus !== 'PAID') return null;
  return (
    <section className="desk-block">
      <h4>Paid plan vs current plan</h4>
      <div className="fin-metrics">
        <Metric label="Paid amount" value={money(app.paymentAmount)} />
        <Metric label="Paid at" value={app.paidAt ? new Date(app.paidAt).toLocaleString() : '—'} />
        <Metric label="Current down payment" value={money(app.approvedDownPaymentAmount)} />
        <Metric label="Current monthly" value={money(app.approvedMonthlyInstallment)} />
        <Metric label="Current financed" value={money(app.approvedFinancedAmount)} />
        <Metric label="Current total" value={money(app.approvedTotalPayable)} />
      </div>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="fin-metric"><span>{label}</span><strong>{value}</strong></div>;
}

function Fact({ label, value, tone }: { label: string; value: string; tone?: 'ok' | 'wait' | 'bad' }) {
  return (
    <div className={`desk-fact ${tone || ''}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusPill({ value, paymentStatus }: { value?: string; paymentStatus?: string }) {
  const shown = paymentStatus === 'PAID' || value === 'PAID' ? 'PAID' : value;
  const tone = shown === 'PAID' || shown === 'APPROVED' || shown === 'DOWN_PAYMENT_SELECTED'
    ? 'ok'
    : shown === 'REJECTED' || shown === 'ELIGIBILITY_FAILED'
      ? 'bad'
      : 'wait';
  return <span className={`fin-pill ${tone}`}>{labelize(shown)}</span>;
}

function PayPill({ value }: { value?: string }) {
  return <span className={`fin-pill ${value === 'PAID' ? 'ok' : 'wait'}`}>{labelize(value || 'UNPAID')}</span>;
}

function Decision({ remarks, setRemarks, onPrimary, primary, onSecond, second, onThird, third }: {
  remarks: string;
  setRemarks: (value: string) => void;
  onPrimary: () => void;
  primary: string;
  onSecond?: () => void;
  second?: string;
  onThird: () => void;
  third: string;
}) {
  return (
    <section className="desk-decision">
      <h4>Decision</h4>
      <label htmlFor="desk-remarks">Remarks</label>
      <textarea id="desk-remarks" value={remarks} onChange={(e) => setRemarks(e.target.value)} placeholder="Required when you send a case back or reject it" />
      <div className="actions">
        <button className="btn btn-primary" type="button" onClick={onPrimary}>{primary}</button>
        {onSecond && second && <button className="btn btn-ghost" type="button" onClick={onSecond}>{second}</button>}
        <button className="btn btn-danger" type="button" onClick={onThird}>{third}</button>
      </div>
    </section>
  );
}

function prettyActor(actor?: string) {
  if (!actor) return '';
  const masked = actor.match(/^customer\s+\*+(\d+)$/i);
  if (masked) return `Customer · ••••${masked[1]}`;
  return actor;
}

function prettyRemark(remark: string) {
  return remark.replace(/[A-Z][A-Z0-9]*_[A-Z0-9_]+/g, (token) => labelize(token));
}

function localPhone(value?: string) {
  if (!value) return '—';
  if (/^92\d{10}$/.test(value)) return `0${value.slice(2)}`;
  return value;
}

function labelize(value?: string) {
  if (!value) return '—';
  return value.split('_').filter(Boolean).map((word) => {
    if (/\d/.test(word) || ['KFS', 'EV', 'DBR', 'OTP', 'CNIC', 'EMI', 'KIBOR'].includes(word)) return word;
    const lower = word.toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  }).join(' ');
}
