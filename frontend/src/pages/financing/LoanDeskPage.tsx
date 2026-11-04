import { useEffect, useState } from 'react';
import { Navigate, useParams } from 'react-router-dom';
import { api, apiUrl } from '../../lib/api';
import { useAuth } from '../../auth/AuthContext';

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
  paymentReference?: string;
  paymentAmount?: number;
  paidAt?: string;
  documents?: Doc[];
  events?: EventRow[];
  createdAt?: string;
};

const money = (value?: number) => value == null ? '—' : Number(value).toLocaleString('en-PK', { maximumFractionDigits: 2 });

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

  return (
    <div className="page fin-shell">
      <div className="container dash">
        <div className="panel panel--wide">
          <div className="badge">LOAN DESK</div>
          <h2 style={{ marginTop: 0 }}>Financing applications</h2>
          <p className="muted">
            Operations reviews the case and forwards it. A different checker approves or rejects it.
            The company commission is {suggested || 'the setup rate'}% and applies to every case.
          </p>
          {error && <p className="error">{error}</p>}
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Customer</th>
                  <th>Product</th>
                  <th>Price</th>
                  <th>Tenure</th>
                  <th>Status</th>
                  <th>Payment</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.referenceNo} className={selected?.referenceNo === row.referenceNo ? 'selected' : ''} onClick={() => load(row.referenceNo)} style={{ cursor: 'pointer' }}>
                    <td>{row.referenceNo}</td>
                    <td>{row.fullName}</td>
                    <td>{row.productTitle}</td>
                    <td>{money(row.productPrice)}</td>
                    <td>{row.tenureMonths ? `${row.tenureMonths} months` : '—'}</td>
                    <td><StatusPill value={row.status} /></td>
                    <td><PayPill value={row.paymentStatus} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected && (
            <section className="fin-case">
              <h3>{selected.referenceNo}</h3>
              <p className="muted">{selected.fullName} · {selected.cnic} · {selected.mobile}</p>
              <div className="fin-metrics">
                <Metric label="Bike" value={selected.productTitle || '—'} />
                <Metric label="Dealer" value={selected.merchantName || '—'} />
                <Metric label="Price" value={money(selected.productPrice)} />
                <Metric label="Tenure" value={selected.tenureMonths ? `${selected.tenureMonths} months` : '—'} />
                <Metric label="Down payment" value={selected.requestedDownPaymentPercent != null ? `${selected.requestedDownPaymentPercent}%` : '—'} />
                <Metric label="KIBOR installment" value={money(selected.indicativeMonthlyInstallment)} />
                <Metric label="Approved rate" value={selected.approvedAnnualRate != null ? `${selected.approvedAnnualRate}%` : '—'} />
                <Metric label="Monthly" value={money(selected.approvedMonthlyInstallment)} />
                <Metric label="Amount due now" value={money(selected.approvedDownPaymentAmount)} />
                <Metric label="Financed" value={money(selected.approvedFinancedAmount)} />
                <Metric label="Installments total" value={money(selected.approvedTotalPayable)} />
                <Metric label="DBR" value={selected.dbrPercent != null ? `${selected.dbrPercent}%` : '—'} />
              </div>
              <p>
                <StatusPill value={selected.status} />{' '}
                <PayPill value={selected.paymentStatus} />{' '}
                <span className="muted">OTP {selected.otpVerifiedAt ? 'verified' : 'missing'} · verification {selected.biometricSource || 'missing'}</span>
              </p>
              <p className="muted">{selected.dbrNote}</p>
              <p className="muted">eCIB {labelize(selected.ecibStatus)} · risk rating {labelize(selected.riskRatingStatus)}</p>
              {selected.paymentReference && <p>Receipt {selected.paymentReference} · {money(selected.paymentAmount)} · {selected.paidAt ? new Date(selected.paidAt).toLocaleString() : ''}</p>}
              {selected.opsRemarks && <p>Operations: {selected.opsRemarks}</p>}
              {selected.checkerRemarks && <p>Checker: {selected.checkerRemarks}</p>}
              <ul className="fin-docs">
                {(selected.documents || []).map((doc) => (
                  <li key={doc.id}>
                    <button className="btn btn-ghost btn-sm" type="button" onClick={() => openDoc(doc.id)}>
                      {labelize(doc.documentCode)} · {doc.originalName}
                    </button>
                  </li>
                ))}
              </ul>
              <ul className="fin-events">
                {(selected.events || []).map((event) => (
                  <li key={`${event.eventType}-${event.createdAt}`}>
                    {event.createdAt ? new Date(event.createdAt).toLocaleString() : ''} · {labelize(event.eventType)} · {event.actorLabel} {event.remark || ''}
                  </li>
                ))}
              </ul>
              {ops && selected.status === 'SUBMITTED' && (
                <Decision remarks={remarks} setRemarks={setRemarks} onPrimary={() => act(`/api/financing/desk/applications/${selected.referenceNo}/forward`, { remarks })} primary="Forward to checker" onSecond={() => act(`/api/financing/desk/applications/${selected.referenceNo}/ops`, { decision: 'DISCREPANT', remarks })} second="Send back" onThird={() => act(`/api/financing/desk/applications/${selected.referenceNo}/ops`, { decision: 'REJECT', remarks })} third="Reject" />
              )}
              {checker && selected.status === 'WITH_CHECKER' && (
                <Decision remarks={remarks} setRemarks={setRemarks} onPrimary={() => act(`/api/financing/desk/applications/${selected.referenceNo}/checker`, { decision: 'APPROVE', remarks })} primary="Approve" onThird={() => act(`/api/financing/desk/applications/${selected.referenceNo}/checker`, { decision: 'REJECT', remarks })} third="Reject" />
              )}
            </section>
          )}
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="fin-metric"><span>{label}</span><strong>{value}</strong></div>;
}

function StatusPill({ value }: { value?: string }) {
  const tone = value === 'APPROVED' || value === 'DOWN_PAYMENT_SELECTED' ? 'ok' : value === 'REJECTED' || value === 'ELIGIBILITY_FAILED' ? 'bad' : 'wait';
  return <span className={`fin-pill ${tone}`}>{labelize(value)}</span>;
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
    <div>
      <div className="form-row">
        <label>Remarks</label>
        <textarea value={remarks} onChange={(e) => setRemarks(e.target.value)} />
      </div>
      <div className="actions">
        <button className="btn btn-primary" type="button" onClick={onPrimary}>{primary}</button>
        {onSecond && second && <button className="btn btn-ghost" type="button" onClick={onSecond}>{second}</button>}
        <button className="btn btn-ghost" type="button" onClick={onThird}>{third}</button>
      </div>
    </div>
  );
}

function labelize(value?: string) {
  if (!value) return '—';
  return value.replaceAll('_', ' ');
}
