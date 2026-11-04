import { type FormEvent, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../lib/api';
import { useAuth } from '../../auth/AuthContext';

type Settings = {
  kiborPercent: number;
  configuredKiborPercent: number;
  kiborFloorPercent: number;
  suggestedBankCommissionPercent: number;
  bankCommissionMaxPercent: number;
  downPaymentMinPercent: number;
  downPaymentMaxPercent: number;
  downPaymentOptions: number[];
  tenureMonths: number[];
  dbrThresholdPercent: number;
  cardLimitBasis: string;
  cardLimitFactorPercent: number;
  latePaymentPercent: number;
  quotationMaxAgeDays: number;
  formulaCode?: string;
  formulaNote?: string;
  productEnabled: boolean;
};
type Category = { id: number; code: string; name: string; active: boolean; sortOrder: number };
type Dealer = { id: number; merchantName: string; categoryName: string; active: boolean };
type Whitelist = { id: number; cnic: string; mobile: string; fullName?: string; segmentCode?: string; active: boolean };
type CatalogueProduct = { id: number; title: string; price: number; merchantName?: string; shownToCustomers?: boolean; active: boolean };

export function FinancingAdminPage() {
  const { session } = useAuth();
  const [settings, setSettings] = useState<Settings | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [dealers, setDealers] = useState<Dealer[]>([]);
  const [whitelist, setWhitelist] = useState<Whitelist[]>([]);
  const [catalogue, setCatalogue] = useState<CatalogueProduct[]>([]);
  const [error, setError] = useState('');
  const [categoryForm, setCategoryForm] = useState({ code: '', name: '', active: 'true', sortOrder: '10' });
  const [whiteForm, setWhiteForm] = useState({ cnic: '', mobile: '', fullName: '', segmentCode: '' });
  const [dealerForm, setDealerForm] = useState({ partyId: '', categoryId: '', active: 'true' });
  const [roleForm, setRoleForm] = useState({ email: '', role: 'FINANCING_OPS' });

  async function load() {
    if (!session?.token) return;
    const [s, c, d, w, products] = await Promise.all([
      api<Settings>('/api/admin/financing/settings', { token: session.token }),
      api<Category[]>('/api/admin/financing/categories', { token: session.token }),
      api<Dealer[]>('/api/admin/financing/dealers', { token: session.token }),
      api<Whitelist[]>('/api/admin/financing/whitelist', { token: session.token }),
      api<CatalogueProduct[]>('/api/admin/financing/products', { token: session.token }),
    ]);
    setSettings(s);
    setCategories(c);
    setDealers(d);
    setWhitelist(w);
    setCatalogue(products);
  }

  useEffect(() => {
    load().catch((err) => setError(err.message));
  }, [session?.token]);

  if (!session) return <Navigate to="/login" replace />;
  if (session.role !== 'PLATFORM_ADMIN') return <Navigate to="/dashboard" replace />;

  async function saveSettings(e: FormEvent) {
    e.preventDefault();
    if (!session?.token || !settings) return;
    setError('');
    try {
      const saved = await api<Settings>('/api/admin/financing/settings', {
        method: 'PUT',
        token: session.token,
        body: JSON.stringify({
          ...settings,
          kiborPercent: settings.configuredKiborPercent,
          downPaymentOptions: settings.downPaymentOptions.join(','),
          tenureMonths: settings.tenureMonths.join(','),
        }),
      });
      setSettings(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    }
  }

  return (
    <div className="page">
      <div className="container dash">
        <div className="panel panel--wide">
          <div className="badge">FINANCING SETUP</div>
          <h2 style={{ marginTop: 0 }}>Categories, rates, whitelist</h2>
          <p className="muted">{settings?.formulaNote}</p>
          {error && <p className="error">{error}</p>}
          {settings && (
            <form onSubmit={saveSettings}>
              <div className="form-row">
                <label>KIBOR %</label>
                <input value={settings.configuredKiborPercent} onChange={(e) => setSettings({ ...settings, configuredKiborPercent: Number(e.target.value) })} />
              </div>
              <div className="form-row">
                <label>KIBOR floor %</label>
                <input value={settings.kiborFloorPercent} onChange={(e) => setSettings({ ...settings, kiborFloorPercent: Number(e.target.value) })} />
              </div>
              <div className="form-row">
                <label>Suggested bank commission %</label>
                <input value={settings.suggestedBankCommissionPercent} onChange={(e) => setSettings({ ...settings, suggestedBankCommissionPercent: Number(e.target.value) })} />
              </div>
              <div className="form-row">
                <label>Maximum bank commission %</label>
                <input value={settings.bankCommissionMaxPercent} onChange={(e) => setSettings({ ...settings, bankCommissionMaxPercent: Number(e.target.value) })} />
              </div>
              <div className="form-row">
                <label>Down payment min / max</label>
                <input value={settings.downPaymentMinPercent} onChange={(e) => setSettings({ ...settings, downPaymentMinPercent: Number(e.target.value) })} />
                <input value={settings.downPaymentMaxPercent} onChange={(e) => setSettings({ ...settings, downPaymentMaxPercent: Number(e.target.value) })} />
              </div>
              <div className="form-row">
                <label>Down payment options (comma separated)</label>
                <input value={settings.downPaymentOptions.join(',')} onChange={(e) => setSettings({ ...settings, downPaymentOptions: e.target.value.split(',').map((n) => Number(n.trim())).filter((n) => !Number.isNaN(n)) })} />
              </div>
              <div className="form-row">
                <label>Tenures in months</label>
                <input value={settings.tenureMonths.join(',')} onChange={(e) => setSettings({ ...settings, tenureMonths: e.target.value.split(',').map((n) => Number(n.trim())).filter((n) => !Number.isNaN(n)) })} />
              </div>
              <div className="form-row">
                <label>DBR threshold %</label>
                <input value={settings.dbrThresholdPercent} onChange={(e) => setSettings({ ...settings, dbrThresholdPercent: Number(e.target.value) })} />
              </div>
              <div className="form-row">
                <label>Card limit basis</label>
                <select value={settings.cardLimitBasis} onChange={(e) => setSettings({ ...settings, cardLimitBasis: e.target.value })}>
                  <option value="UNSET">UNSET</option>
                  <option value="UTILIZED">UTILIZED</option>
                  <option value="AVAILABLE">AVAILABLE</option>
                </select>
              </div>
              <div className="form-row">
                <label>Quotation max age (days)</label>
                <input value={settings.quotationMaxAgeDays} onChange={(e) => setSettings({ ...settings, quotationMaxAgeDays: Number(e.target.value) })} />
              </div>
              <div className="form-row">
                <label>Formula code</label>
                <input value={settings.formulaCode || ''} onChange={(e) => setSettings({ ...settings, formulaCode: e.target.value })} />
              </div>
              <label>
                <input type="checkbox" checked={settings.productEnabled} onChange={(e) => setSettings({ ...settings, productEnabled: e.target.checked })} />
                Product available
              </label>
              <div className="actions">
                <button className="btn btn-primary" type="submit">Save settings</button>
              </div>
            </form>
          )}

          <h3>Category</h3>
          <form onSubmit={async (e) => {
            e.preventDefault();
            if (!session.token) return;
            await api('/api/admin/financing/categories', { method: 'POST', token: session.token, body: JSON.stringify(categoryForm) });
            await load();
          }}
          >
            <div className="form-row">
              <label>Code</label>
              <input value={categoryForm.code} onChange={(e) => setCategoryForm({ ...categoryForm, code: e.target.value })} />
            </div>
            <div className="form-row">
              <label>Name</label>
              <input value={categoryForm.name} onChange={(e) => setCategoryForm({ ...categoryForm, name: e.target.value })} />
            </div>
            <button className="btn btn-primary" type="submit">Add category</button>
          </form>
          <ul>{categories.map((c) => <li key={c.id}>{c.code} · {c.name} · {c.active ? 'active' : 'inactive'}</li>)}</ul>

          <h3>Whitelist</h3>
          <form onSubmit={async (e) => {
            e.preventDefault();
            if (!session.token) return;
            await api('/api/admin/financing/whitelist', { method: 'POST', token: session.token, body: JSON.stringify(whiteForm) });
            setWhiteForm({ cnic: '', mobile: '', fullName: '', segmentCode: '' });
            await load();
          }}
          >
            <div className="form-row"><label>CNIC</label><input value={whiteForm.cnic} onChange={(e) => setWhiteForm({ ...whiteForm, cnic: e.target.value })} /></div>
            <div className="form-row"><label>Mobile</label><input value={whiteForm.mobile} onChange={(e) => setWhiteForm({ ...whiteForm, mobile: e.target.value })} /></div>
            <div className="form-row"><label>Name</label><input value={whiteForm.fullName} onChange={(e) => setWhiteForm({ ...whiteForm, fullName: e.target.value })} /></div>
            <div className="form-row"><label>Segment</label><input value={whiteForm.segmentCode} onChange={(e) => setWhiteForm({ ...whiteForm, segmentCode: e.target.value })} /></div>
            <button className="btn btn-primary" type="submit">Save whitelist</button>
          </form>
          <ul>{whitelist.map((row) => <li key={row.id}>{row.cnic} · {row.mobile} · {row.fullName} · {row.active ? 'active' : 'inactive'}</li>)}</ul>

          <h3>Show to customers</h3>
          <p className="muted">A merchant’s price and description stay hidden until you show the product.</p>
          <ul>
            {catalogue.map((product) => (
              <li key={product.id}>
                {product.merchantName} · {product.title} · {product.price} · {product.shownToCustomers ? 'Shown' : 'Hidden'}
                <button
                  className="btn btn-ghost btn-sm"
                  type="button"
                  onClick={async () => {
                    if (!session.token) return;
                    await api(`/api/admin/financing/products/${product.id}/visibility`, {
                      method: 'POST',
                      token: session.token,
                      body: JSON.stringify({ shown: !product.shownToCustomers }),
                    });
                    await load();
                  }}
                >
                  {product.shownToCustomers ? 'Hide' : 'Show to customers'}
                </button>
              </li>
            ))}
          </ul>

          <h3>Authorize dealer</h3>
          <form onSubmit={async (e) => {
            e.preventDefault();
            if (!session.token) return;
            await api('/api/admin/financing/dealers', { method: 'POST', token: session.token, body: JSON.stringify(dealerForm) });
            await load();
          }}
          >
            <div className="form-row"><label>Merchant party id</label><input value={dealerForm.partyId} onChange={(e) => setDealerForm({ ...dealerForm, partyId: e.target.value })} /></div>
            <div className="form-row"><label>Category id</label><input value={dealerForm.categoryId} onChange={(e) => setDealerForm({ ...dealerForm, categoryId: e.target.value })} /></div>
            <button className="btn btn-primary" type="submit">Authorize</button>
          </form>
          <ul>{dealers.map((d) => <li key={d.id}>{d.merchantName} · {d.categoryName} · {d.active ? 'active' : 'inactive'}</li>)}</ul>

          <h3>Desk roles</h3>
          <p className="muted">Grant FINANCING_OPS and FINANCING_CHECKER to two different users. The person who forwards a case cannot approve it. Sign in again after a grant.</p>
          <form onSubmit={async (e) => {
            e.preventDefault();
            if (!session.token) return;
            await api('/api/admin/financing/roles', { method: 'POST', token: session.token, body: JSON.stringify(roleForm) });
            setRoleForm({ email: '', role: 'FINANCING_OPS' });
          }}
          >
            <div className="form-row"><label>Account email</label><input value={roleForm.email} onChange={(e) => setRoleForm({ ...roleForm, email: e.target.value })} /></div>
            <div className="form-row">
              <label>Role</label>
              <select value={roleForm.role} onChange={(e) => setRoleForm({ ...roleForm, role: e.target.value })}>
                <option value="FINANCING_OPS">FINANCING_OPS</option>
                <option value="FINANCING_CHECKER">FINANCING_CHECKER</option>
              </select>
            </div>
            <button className="btn btn-primary" type="submit">Grant role</button>
          </form>
        </div>
      </div>
    </div>
  );
}
