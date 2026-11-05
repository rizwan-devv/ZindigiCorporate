type Plan = {
  tenureMonths: number
  annualRatePercent: number
  monthlyInstallment: number
  totalPayable: number
  downPaymentAmount: number
  financedAmount: number
}

const money = (value?: number) =>
  value == null ? '—' : `Rs ${Number(value).toLocaleString('en-PK', { maximumFractionDigits: 2 })}`

export function PlanCompare({ plans, title = 'Compare tenures' }: { plans: Plan[]; title?: string }) {
  if (!plans.length) return null
  return (
    <section className="plan-compare compact">
      <h3>{title}</h3>
      <div className="plan-table" role="table" aria-label={title}>
        <div className="plan-table-head" role="row">
          <span role="columnheader">Tenure</span>
          <span role="columnheader">Rate</span>
          <span role="columnheader">Monthly</span>
          <span role="columnheader">Total</span>
        </div>
        {plans.map((plan) => (
          <div className="plan-table-row" role="row" key={plan.tenureMonths}>
            <strong role="cell">{plan.tenureMonths} mo</strong>
            <span role="cell">{plan.annualRatePercent}%</span>
            <span role="cell">{money(plan.monthlyInstallment)}</span>
            <span role="cell">{money(plan.totalPayable)}</span>
          </div>
        ))}
      </div>
      <p className="plan-table-note">
        Down {money(plans[0]?.downPaymentAmount)} · Financed {money(plans[0]?.financedAmount)}
      </p>
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
    <article className="plan-card paid plan-card-single">
      <div className="plan-card-top">
        <div>
          <p className="kicker">Financing terms</p>
          <h3>Down payment received</h3>
        </div>
        <span className="pill-mini good">Paid</span>
      </div>
      <div className="figures tight">
        <div>
          <span>Paid</span>
          <strong>{money(paid.amount)}</strong>
        </div>
        <div>
          <span>Monthly</span>
          <strong>{money(current.monthly ?? paid.monthly)}</strong>
        </div>
        <div>
          <span>Rate</span>
          <strong>{(current.rate ?? paid.rate) != null ? `${current.rate ?? paid.rate}%` : '—'}</strong>
        </div>
        <div>
          <span>Tenure</span>
          <strong>
            {(current.tenure ?? paid.tenure) != null ? `${current.tenure ?? paid.tenure} months` : '—'}
          </strong>
        </div>
        <div>
          <span>Financed</span>
          <strong>{money(current.financed ?? paid.financed)}</strong>
        </div>
        <div>
          <span>Installments total</span>
          <strong>{money(current.total)}</strong>
        </div>
      </div>
      {paid.paidAt && (
        <p className="plan-card-meta">Paid at {new Date(paid.paidAt).toLocaleString()}</p>
      )}
    </article>
  )
}
