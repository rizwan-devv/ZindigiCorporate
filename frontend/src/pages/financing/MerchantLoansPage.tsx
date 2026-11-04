import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../lib/api';
import { useAuth } from '../../auth/AuthContext';
import { PaymentReceipt } from './receipt';

type Row = {
  referenceNo: string;
  customerName: string;
  cnicMasked: string;
  productTitle: string;
  productPrice: number;
  tenureMonths: number;
  status: string;
  paymentStatus: string;
  paymentReference?: string;
  paymentAmount?: number;
  paymentFromWallet?: string;
  paymentToWallet?: string;
  paidAt?: string;
  createdAt?: string;
};

export function MerchantLoansPage() {
  const { session } = useAuth();
  const [rows, setRows] = useState<Row[]>([]);
  const [error, setError] = useState('');
  const [openRef, setOpenRef] = useState('');

  useEffect(() => {
    if (!session?.token) return;
    api<Row[]>('/api/financing/applications', { token: session.token })
      .then(setRows)
      .catch((err) => setError(err.message));
  }, [session?.token]);

  if (!session) return <Navigate to="/login" replace />;

  const money = (value?: number) => value == null ? '—' : `Rs ${Number(value).toLocaleString('en-PK', { maximumFractionDigits: 2 })}`;
  const labelize = (value?: string) => {
    if (!value) return '—';
    return value.split('_').filter(Boolean).map((word) => {
      if (/\d/.test(word) || ['KFS', 'EV', 'DBR', 'OTP', 'CNIC', 'EMI'].includes(word)) return word;
      const lower = word.toLowerCase();
      return lower.charAt(0).toUpperCase() + lower.slice(1);
    }).join(' ');
  };

  return (
    <div className="page fin-shell">
      <div className="container dash">
        <div className="panel panel--wide desk">
          <header className="desk-top">
            <div>
              <p className="desk-kicker">Merchant</p>
              <h2>Financing requests</h2>
              <p className="desk-caption">CNIC is masked. Documents and the financing spread stay with the bank.</p>
            </div>
          </header>
          {error && <p className="error">{error}</p>}
          <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Customer</th>
                <th>CNIC</th>
                <th>Product</th>
                <th>Price</th>
                <th>Tenure</th>
                <th>Status</th>
                <th>Payment</th>
                <th>Receipt</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && <tr><td colSpan={9}>No requests yet.</td></tr>}
              {rows.map((row) => (
                <tr key={row.referenceNo} className={openRef === row.referenceNo ? 'selected' : ''}>
                  <td className="desk-mono">{row.referenceNo}</td>
                  <td>{row.customerName}</td>
                  <td>{row.cnicMasked}</td>
                  <td>{row.productTitle}</td>
                  <td>{money(row.productPrice)}</td>
                  <td>{row.tenureMonths} mo</td>
                  <td><span className={`fin-pill ${row.status === 'APPROVED' || row.status === 'DOWN_PAYMENT_SELECTED' ? 'ok' : row.status === 'REJECTED' ? 'bad' : 'wait'}`}>{labelize(row.status)}</span></td>
                  <td><span className={`fin-pill ${row.paymentStatus === 'PAID' ? 'ok' : 'wait'}`}>{labelize(row.paymentStatus)}</span></td>
                  <td>
                    {row.paymentStatus === 'PAID' ? (
                      <button className="btn btn-ghost btn-sm" type="button" onClick={() => setOpenRef(openRef === row.referenceNo ? '' : row.referenceNo)}>View receipt</button>
                    ) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
          {rows.filter((row) => row.referenceNo === openRef && row.paymentStatus === 'PAID').map((row) => (
            <PaymentReceipt key={row.referenceNo} data={{
              receiptNo: row.paymentReference || row.referenceNo,
              applicationNo: row.referenceNo,
              customerName: row.customerName,
              productTitle: row.productTitle,
              amount: row.paymentAmount,
              fromWallet: row.paymentFromWallet,
              toWallet: row.paymentToWallet,
              paidAt: row.paidAt,
            }} />
          ))}
        </div>
      </div>
    </div>
  );
}
