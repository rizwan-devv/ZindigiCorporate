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
  documents?: Doc[];
  events?: EventRow[];
  createdAt?: string;
};

export function LoanDeskPage() {
  const { session } = useAuth();
  const params = useParams();
  const [rows, setRows] = useState<Application[]>([]);
  const [selected, setSelected] = useState<Application | null>(null);
  const [suggested, setSuggested] = useState('');
  const [commission, setCommission] = useState('');
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
    <div className="page">
      <div className="container dash">
        <div className="panel panel--wide">
          <div className="badge">LOAN DESK</div>
          <h2 style={{ marginTop: 0 }}>Financing applications</h2>
          <p className="muted">
            Operations sets the bank commission and forwards. The checker can approve or reject, and cannot change the rate.
            eCIB and the internal risk rating are not connected.
          </p>
          {error && <p className="error">{error}</p>}
          <table>
            <thead>
              <tr>
                <th>Reference</th>
                <th>Customer</th>
                <th>CNIC</th>
                <th>Mobile</th>
                <th>Merchant</th>
                <th>Product</th>
                <th>Price</th>
                <th>Tenure</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.referenceNo} onClick={() => load(row.referenceNo)} style={{ cursor: 'pointer' }}>
                  <td>{row.referenceNo}</td>
                  <td>{row.fullName}</td>
                  <td>{row.cnic}</td>
                  <td>{row.mobile}</td>
                  <td>{row.merchantName}</td>
                  <td>{row.productTitle}</td>
                  <td>{row.productPrice}</td>
                  <td>{row.tenureMonths}</td>
                  <td>{row.status}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {selected && (
            <div>
              <h3>{selected.referenceNo} · {selected.status}</h3>
              <p>{selected.fullName} · {selected.cnic} · {selected.mobile}</p>
              <p>{selected.merchantName} · {selected.productTitle} · {selected.productPrice}</p>
              <p>Requested down payment {selected.requestedDownPaymentPercent}% · tenure {selected.tenureMonths} months</p>
              <p>KIBOR {selected.indicativeKiborPercent}% · indicative installment {selected.indicativeMonthlyInstallment}</p>
              <p>Bank commission {selected.bankCommissionPercent ?? 'not set'} · approved rate {selected.approvedAnnualRate ?? '—'}</p>
              <p>OTP {selected.otpVerifiedAt ? 'verified' : 'missing'} · biometric {selected.biometricSource || 'missing'} {selected.biometricRef || ''}</p>
              <p>eCIB {selected.ecibStatus} · risk rating {selected.riskRatingStatus}</p>
              <p>DBR {selected.dbrPercent}% · {selected.dbrPassed ? 'within threshold' : 'above threshold or not assessed'}</p>
              <p className="muted">{selected.dbrNote}</p>
              <p>Payment {selected.paymentStatus}. Approved installment {selected.approvedMonthlyInstallment ?? '—'} · total {selected.approvedTotalPayable ?? '—'}</p>
              {selected.opsRemarks && <p>Operations: {selected.opsRemarks}</p>}
              {selected.checkerRemarks && <p>Checker: {selected.checkerRemarks}</p>}
              <ul>
                {(selected.documents || []).map((doc) => (
                  <li key={doc.id}>
                    <button className="btn btn-ghost btn-sm" type="button" onClick={() => openDoc(doc.id)}>
                      {doc.documentCode} · {doc.originalName}
                    </button>
                  </li>
                ))}
              </ul>
              <ul>
                {(selected.events || []).map((event) => (
                  <li key={`${event.eventType}-${event.createdAt}`}>{event.createdAt} · {event.eventType} · {event.actorLabel} {event.remark || ''}</li>
                ))}
              </ul>
              {ops && selected.status === 'SUBMITTED' && (
                <div>
                  <div className="form-row">
                    <label>Bank commission % (suggested {suggested}). Stored separately from KIBOR.</label>
                    <input value={commission} onChange={(e) => setCommission(e.target.value)} placeholder={suggested} />
                  </div>
                  <div className="form-row">
                    <label>Remarks</label>
                    <textarea value={remarks} onChange={(e) => setRemarks(e.target.value)} />
                  </div>
                  <div className="actions">
                    <button className="btn btn-primary" type="button" onClick={() => act(
                      `/api/financing/desk/applications/${selected.referenceNo}/commission`,
                      { percent: commission || suggested, remarks },
                    )}
                    >
                      Save commission
                    </button>
                    <button className="btn btn-primary" type="button" onClick={() => act(
                      `/api/financing/desk/applications/${selected.referenceNo}/forward`,
                      { remarks },
                    )}
                    >
                      Forward to checker
                    </button>
                    <button className="btn btn-ghost" type="button" onClick={() => act(
                      `/api/financing/desk/applications/${selected.referenceNo}/ops`,
                      { decision: 'DISCREPANT', remarks },
                    )}
                    >
                      Discrepant
                    </button>
                    <button className="btn btn-ghost" type="button" onClick={() => act(
                      `/api/financing/desk/applications/${selected.referenceNo}/ops`,
                      { decision: 'REJECT', remarks },
                    )}
                    >
                      Reject
                    </button>
                  </div>
                </div>
              )}
              {checker && selected.status === 'WITH_CHECKER' && (
                <div>
                  <div className="form-row">
                    <label>Checker remarks</label>
                    <textarea value={remarks} onChange={(e) => setRemarks(e.target.value)} />
                  </div>
                  <div className="actions">
                    <button className="btn btn-primary" type="button" onClick={() => act(
                      `/api/financing/desk/applications/${selected.referenceNo}/checker`,
                      { decision: 'APPROVE', remarks },
                    )}
                    >
                      Approve
                    </button>
                    <button className="btn btn-ghost" type="button" onClick={() => act(
                      `/api/financing/desk/applications/${selected.referenceNo}/checker`,
                      { decision: 'REJECT', remarks },
                    )}
                    >
                      Reject
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
