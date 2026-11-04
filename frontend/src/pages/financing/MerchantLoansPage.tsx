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

  return (
    <div className="page">
      <div className="container dash">
        <div className="panel panel--wide">
          <div className="badge">FINANCING</div>
          <h2 style={{ marginTop: 0 }}>Requests for your products</h2>
          <p className="muted">CNIC is masked. Documents and financing spreads stay with the bank desk.</p>
          {error && <p className="error">{error}</p>}
          <table>
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
              {rows.map((row) => (
                <tr key={row.referenceNo}>
                  <td>{row.referenceNo}</td>
                  <td>{row.customerName}</td>
                  <td>{row.cnicMasked}</td>
                  <td>{row.productTitle}</td>
                  <td>{row.productPrice}</td>
                  <td>{row.tenureMonths}</td>
                  <td>{row.status}</td>
                  <td>{row.paymentStatus}</td>
                  <td>
                    {row.paymentStatus === 'PAID' ? (
                      <button className="btn btn-ghost btn-sm" type="button" onClick={() => setOpenRef(row.referenceNo)}>View receipt</button>
                    ) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
