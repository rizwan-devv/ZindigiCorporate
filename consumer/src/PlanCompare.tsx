type Plan = {
  tenureMonths: number
  annualRatePercent: number
  monthlyInstallment: number
  totalPayable: number
  downPaymentAmount: number
  financedAmount: number
}

const money = (value?: number) => value == null ? '—' : `Rs ${Number(value).toLocaleString('en-PK', { maximumFractionDigits: 2 })}`

export function PlanCompare({ plans, title = 'Compare tenures' }: { plans: Plan[]; title?: string }) {
  if (!plans.length) return null
  return (
    <section className="plan-compare">
      <h3>{title}</h3>
      <div className="plan-compare-grid">
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
      </div>
    </section>
  )
}

export function PaidVsCurrent({
  paid,
  current,
}: {
  paid: { amount?: number; rate?: number; monthly?: number; financed?: number; tenure?: number; paidAt?: string }
  current: { downPayment?: number; rate?: number; monthly?: number; financed?: number; total?: number; tenure?: number }
}) {
  return (
    <div className="plan-split">
      <article className="plan-card paid">
        <p className="kicker">Paid plan</p>
        <h3>Down payment received</h3>
        <dl>
          <div><dt>Paid</dt><dd>{money(paid.amount)}</dd></div>
          <div><dt>Rate</dt><dd>{paid.rate != null ? `${paid.rate}%` : '—'}</dd></div>
          <div><dt>Monthly</dt><dd>{money(paid.monthly)}</dd></div>
          <div><dt>Financed</dt><dd>{money(paid.financed)}</dd></div>
          <div><dt>Tenure</dt><dd>{paid.tenure != null ? `${paid.tenure} months` : '—'}</dd></div>
          <div><dt>Paid at</dt><dd>{paid.paidAt ? new Date(paid.paidAt).toLocaleString() : '—'}</dd></div>
        </dl>
      </article>
      <article className="plan-card current">
        <p className="kicker">Current plan</p>
        <h3>Updated financing terms</h3>
        <dl>
          <div><dt>Down payment</dt><dd>{money(current.downPayment)}</dd></div>
          <div><dt>Rate</dt><dd>{current.rate != null ? `${current.rate}%` : '—'}</dd></div>
          <div><dt>Monthly</dt><dd>{money(current.monthly)}</dd></div>
          <div><dt>Financed</dt><dd>{money(current.financed)}</dd></div>
          <div><dt>Installments total</dt><dd>{money(current.total)}</dd></div>
          <div><dt>Tenure</dt><dd>{current.tenure != null ? `${current.tenure} months` : '—'}</dd></div>
        </dl>
      </article>
    </div>
  )
}
