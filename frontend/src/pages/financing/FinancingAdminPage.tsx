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
  collectionWallet?: string;
};
type Category = { id: number; code: string; name: string; active: boolean; sortOrder: number };
type Dealer = { id: number; merchantName: string; categoryName: string; active: boolean };
type CatalogueProduct = { id: number; title: string; price: number; merchantName?: string; shownToCustomers?: boolean; active: boolean };

const DOWN_CHOICES = [10, 20, 30, 40, 50, 60];
const TENURE_CHOICES = [6, 12, 18, 24, 36, 48, 60];

export function FinancingAdminPage() {
  const { session } = useAuth();
  const [settings, setSettings] = useState<Settings | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [dealers, setDealers] = useState<Dealer[]>([]);
  const [catalogue, setCatalogue] = useState<CatalogueProduct[]>([]);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState('');
  const [categoryForm, setCategoryForm] = useState({ code: '', name: '', active: 'true', sortOrder: '10' });
  const [dealerForm, setDealerForm] = useState({ partyId: '', categoryId: '', active: 'true' });
  const [roleForm, setRoleForm] = useState({ email: '', role: 'FINANCING_OPS' });

  async function load() {
    if (!session?.token) return;
    const [s, c, d, products] = await Promise.all([
      api<Settings>('/api/admin/financing/settings', { token: session.token }),
      api<Category[]>('/api/admin/financing/categories', { token: session.token }),
      api<Dealer[]>('/api/admin/financing/dealers', { token: session.token }),
      api<CatalogueProduct[]>('/api/admin/financing/products', { token: session.token }),
    ]);
    setSettings(s);
    setCategories(c);
    setDealers(d);
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
    setSaved('');
    try {
      const savedSettings = await api<Settings>('/api/admin/financing/settings', {
        method: 'PUT',
        token: session.token,
        body: JSON.stringify({
          ...settings,
          kiborPercent: settings.configuredKiborPercent,
          downPaymentOptions: settings.downPaymentOptions.join(','),
          tenureMonths: settings.tenureMonths.join(','),
        }),
      });
      setSettings(savedSettings);
      setSaved('Settings saved.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    }
  }

  return (
    <div className="page fin-shell">
      <div className="container dash">
        <div className="panel panel--wide">
          <div className="badge" title="Platform admin only">FINANCING SETUP</div>
          <h2 style={{ marginTop: 0 }} title="Set bank rates and what dealers can sell">Rates and catalogue</h2>
          <p className="muted">Customers come from Zindigi. A case is not held back by a whitelist. The collection wallet receives every down payment.</p>
          {error && <p className="error">{error}</p>}
          {saved && <p className="muted">{saved}</p>}
          {settings && (
            <form onSubmit={saveSettings}>
              <div className="fin-grid">
                <Field label="KIBOR %" value={settings.configuredKiborPercent} onChange={(value) => setSettings({ ...settings, configuredKiborPercent: value })} />
                <Field label="KIBOR floor %" value={settings.kiborFloorPercent} onChange={(value) => setSettings({ ...settings, kiborFloorPercent: value })} />
                <Field label="Bank commission %" value={settings.suggestedBankCommissionPercent} onChange={(value) => setSettings({ ...settings, suggestedBankCommissionPercent: value })} />
                <Field label="Maximum commission %" value={settings.bankCommissionMaxPercent} onChange={(value) => setSettings({ ...settings, bankCommissionMaxPercent: value })} />
                <Field label="Minimum down payment %" value={settings.downPaymentMinPercent} onChange={(value) => setSettings({ ...settings, downPaymentMinPercent: value })} />
                <Field label="Maximum down payment %" value={settings.downPaymentMaxPercent} onChange={(value) => setSettings({ ...settings, downPaymentMaxPercent: value })} />
                <div className="span-2">
                  <Choices
                    label="Down payment options"
                    suffix="%"
                    choices={DOWN_CHOICES}
                    selected={settings.downPaymentOptions}
                    onChange={(downPaymentOptions) => setSettings({ ...settings, downPaymentOptions })}
                  />
                </div>
                <div className="span-2">
                  <Choices
                    label="Tenures"
                    suffix=" months"
                    choices={TENURE_CHOICES}
                    selected={settings.tenureMonths}
                    onChange={(tenureMonths) => setSettings({ ...settings, tenureMonths })}
                  />
                </div>
                <Field label="DBR threshold %" value={settings.dbrThresholdPercent} onChange={(value) => setSettings({ ...settings, dbrThresholdPercent: value })} />
                <div className="form-row">
                  <label>Card limit basis</label>
                  <select value={settings.cardLimitBasis} onChange={(e) => setSettings({ ...settings, cardLimitBasis: e.target.value })}>
                    <option value="UNSET">Not applied</option>
                    <option value="UTILIZED">Utilized limit</option>
                    <option value="AVAILABLE">Available limit</option>
                  </select>
                </div>
                <Field label="Quotation max age (days)" value={settings.quotationMaxAgeDays} onChange={(value) => setSettings({ ...settings, quotationMaxAgeDays: value })} />
                <div className="form-row">
                  <label>Bank collection wallet</label>
                  <input value={settings.collectionWallet || ''} placeholder="03XXXXXXXXX" onChange={(e) => setSettings({ ...settings, collectionWallet: e.target.value })} />
                </div>
                <div className="form-row">
                  <label>Formula</label>
                  <select value={settings.formulaCode || ''} onChange={(e) => setSettings({ ...settings, formulaCode: e.target.value })}>
                    <option value="REDUCING_BALANCE_EMI">Reducing balance EMI</option>
                    <option value="">Quotes off</option>
                  </select>
                </div>
                <label className="span-2">
                  <input type="checkbox" checked={settings.productEnabled} onChange={(e) => setSettings({ ...settings, productEnabled: e.target.checked })} />
                  {' '}Financing available to customers
                </label>
              </div>
              <div className="actions">
                <button className="btn btn-primary" type="submit" title="Save rates, tenures, and the bank collection wallet">Save settings</button>
              </div>
            </form>
          )}

          <section className="fin-section">
            <h3>Products customers can see</h3>
            <p className="muted">A merchant’s price and description stay hidden until you show the product.</p>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr><th>Dealer</th><th>Product</th><th>Price</th><th>Visibility</th><th></th></tr>
                </thead>
                <tbody>
                  {catalogue.map((product) => (
                    <tr key={product.id}>
                      <td>{product.merchantName}</td>
                      <td>{product.title}</td>
                      <td>{Number(product.price).toLocaleString('en-PK')}</td>
                      <td><span className={product.shownToCustomers ? 'fin-pill ok' : 'fin-pill'}>{product.shownToCustomers ? 'Shown' : 'Hidden'}</span></td>
                      <td>
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
                          {product.shownToCustomers ? 'Hide' : 'Show'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="fin-section">
            <h3>Category</h3>
            <form onSubmit={async (e) => {
              e.preventDefault();
              if (!session.token) return;
              await api('/api/admin/financing/categories', { method: 'POST', token: session.token, body: JSON.stringify(categoryForm) });
              setCategoryForm({ code: '', name: '', active: 'true', sortOrder: '10' });
              await load();
            }}
            >
              <div className="fin-grid">
                <div className="form-row"><label>Code</label><input value={categoryForm.code} onChange={(e) => setCategoryForm({ ...categoryForm, code: e.target.value })} /></div>
                <div className="form-row"><label>Name</label><input value={categoryForm.name} onChange={(e) => setCategoryForm({ ...categoryForm, name: e.target.value })} /></div>
              </div>
              <div className="actions"><button className="btn btn-primary" type="submit" title="Create a product category dealers can sell in">Add category</button></div>
            </form>
            <ul>{categories.map((c) => <li key={c.id}>{c.name} · {c.active ? 'Active' : 'Inactive'}</li>)}</ul>
          </section>

          <section className="fin-section">
            <h3>Authorize a dealer</h3>
            <form onSubmit={async (e) => {
              e.preventDefault();
              if (!session.token) return;
              await api('/api/admin/financing/dealers', { method: 'POST', token: session.token, body: JSON.stringify(dealerForm) });
              await load();
            }}
            >
              <div className="fin-grid">
                <div className="form-row"><label>Merchant party id</label><input value={dealerForm.partyId} onChange={(e) => setDealerForm({ ...dealerForm, partyId: e.target.value })} /></div>
                <div className="form-row">
                  <label>Category</label>
                  <select required value={dealerForm.categoryId} onChange={(e) => setDealerForm({ ...dealerForm, categoryId: e.target.value })}>
                    <option value="">Select</option>
                    {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>
              </div>
              <div className="actions"><button className="btn btn-primary" type="submit" title="Allow this merchant to sell in the selected category">Authorize</button></div>
            </form>
            <ul>{dealers.map((d) => <li key={d.id}>{d.merchantName} · {d.categoryName} · {d.active ? 'Active' : 'Inactive'}</li>)}</ul>
          </section>

          <section className="fin-section">
            <h3>Desk roles</h3>
            <p className="muted">Give operations and checker to two different people. The person who forwards a case cannot approve it. Sign in again after a grant.</p>
            <form onSubmit={async (e) => {
              e.preventDefault();
              if (!session.token) return;
              await api('/api/admin/financing/roles', { method: 'POST', token: session.token, body: JSON.stringify(roleForm) });
              setRoleForm({ email: '', role: 'FINANCING_OPS' });
              setSaved('Role granted. That user should sign in again.');
            }}
            >
              <div className="fin-grid">
                <div className="form-row"><label>Account email</label><input value={roleForm.email} onChange={(e) => setRoleForm({ ...roleForm, email: e.target.value })} /></div>
                <div className="form-row">
                  <label>Role</label>
                  <select value={roleForm.role} onChange={(e) => setRoleForm({ ...roleForm, role: e.target.value })}>
                    <option value="FINANCING_OPS">Operations</option>
                    <option value="FINANCING_CHECKER">Checker</option>
                  </select>
                </div>
              </div>
              <div className="actions"><button className="btn btn-primary" type="submit" title="Give financing ops or checker access to that email">Grant role</button></div>
            </form>
          </section>
        </div>
      </div>
    </div>
  );
}

function Field({ label, value, onChange }: { label: string; value: number; onChange: (value: number) => void }) {
  return (
    <div className="form-row">
      <label>{label}</label>
      <input value={value} onChange={(e) => onChange(Number(e.target.value))} />
    </div>
  );
}

function Choices({ label, suffix, choices, selected, onChange }: {
  label: string;
  suffix: string;
  choices: number[];
  selected: number[];
  onChange: (values: number[]) => void;
}) {
  const available = choices.filter((choice) => !selected.includes(choice));
  return (
    <div className="form-row">
      <label>{label}</label>
      <div className="fin-picks">
        {selected.map((value) => (
          <button key={value} className="fin-chip" type="button" onClick={() => onChange(selected.filter((item) => item !== value))}>
            {value}{suffix} · remove
          </button>
        ))}
      </div>
      <select
        value=""
        onChange={(e) => {
          const value = Number(e.target.value);
          if (!value) return;
          onChange([...selected, value].sort((a, b) => a - b));
        }}
      >
        <option value="">Add from the list</option>
        {available.map((choice) => <option key={choice} value={choice}>{choice}{suffix}</option>)}
      </select>
    </div>
  );
}
