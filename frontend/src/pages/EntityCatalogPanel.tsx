import { useEffect, useState } from 'react';
import { api } from '../lib/api';

type Policy = {
  entityType: string;
  label: string;
  kycRequired: boolean;
  backOfficeKycOnly: boolean;
};

type CatalogDoc = {
  id: number;
  documentCode: string;
  documentLabel: string;
  mandatory: boolean;
  requirement: string;
  custom: boolean;
};

export function EntityCatalogPanel({ token }: { token: string }) {
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [entityType, setEntityType] = useState('');
  const [label, setLabel] = useState('');
  const [docs, setDocs] = useState<CatalogDoc[]>([]);
  const [newName, setNewName] = useState('');
  const [newRequired, setNewRequired] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api<Policy[]>('/api/admin/entity-kyc-policy', { token })
      .then((rows) => {
        setPolicies(rows);
        setEntityType((current) => current || rows[0]?.entityType || '');
      })
      .catch((e: Error) => setError(e.message));
  }, [token]);

  useEffect(() => {
    if (!entityType) return;
    const row = policies.find((p) => p.entityType === entityType);
    setLabel(row?.label || '');
    api<CatalogDoc[]>(`/api/admin/entity-documents/${entityType}`, { token })
      .then(setDocs)
      .catch((e: Error) => setError(e.message));
  }, [entityType, policies, token]);

  async function saveLabel() {
    setBusy(true);
    setError('');
    try {
      const saved = await api<Policy>(`/api/admin/entity-kyc-policy/${entityType}/label`, {
        method: 'PUT',
        token,
        body: JSON.stringify({ displayLabel: label }),
      });
      setPolicies((rows) => rows.map((r) => (r.entityType === saved.entityType ? { ...r, ...saved } : r)));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not rename entity');
    } finally {
      setBusy(false);
    }
  }

  async function saveKyc(required: boolean) {
    setBusy(true);
    setError('');
    try {
      const saved = await api<Policy>(`/api/admin/entity-kyc-policy/${entityType}`, {
        method: 'PUT',
        token,
        body: JSON.stringify({ kycRequired: required }),
      });
      setPolicies((rows) => rows.map((r) => (r.entityType === saved.entityType ? { ...r, ...saved } : r)));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not update KYC');
    } finally {
      setBusy(false);
    }
  }

  async function saveDoc(row: CatalogDoc, nextLabel: string, mandatory: boolean) {
    setBusy(true);
    setError('');
    try {
      const saved = await api<CatalogDoc>(`/api/admin/entity-documents/${row.id}`, {
        method: 'PUT',
        token,
        body: JSON.stringify({ documentLabel: nextLabel, mandatory }),
      });
      setDocs((list) => list.map((d) => (d.id === saved.id ? saved : d)));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not update document');
    } finally {
      setBusy(false);
    }
  }

  async function addDoc() {
    setBusy(true);
    setError('');
    try {
      const saved = await api<CatalogDoc>('/api/admin/entity-documents', {
        method: 'POST',
        token,
        body: JSON.stringify({ entityType, documentLabel: newName, mandatory: newRequired }),
      });
      setDocs((list) => [...list, saved]);
      setNewName('');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not add document');
    } finally {
      setBusy(false);
    }
  }

  async function removeDoc(id: number) {
    setBusy(true);
    setError('');
    try {
      await api(`/api/admin/entity-documents/${id}`, { method: 'DELETE', token });
      setDocs((list) => list.filter((d) => d.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not remove document');
    } finally {
      setBusy(false);
    }
  }

  const policy = policies.find((p) => p.entityType === entityType);

  return (
    <section className="ops-panel" style={{ marginBottom: '1.25rem' }}>
      <h2 className="ops-title" style={{ fontSize: '1.15rem' }}>Entity documents</h2>
      <p className="muted">Choose the documents each entity type must upload. Existing applications keep working when a type has no list of its own.</p>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="form-row">
        <label>Entity type</label>
        <select value={entityType} onChange={(e) => setEntityType(e.target.value)}>
          {policies.map((p) => (
            <option key={p.entityType} value={p.entityType}>{p.label}</option>
          ))}
        </select>
      </div>
      {policy && (
        <div className="form-grid">
          <div className="form-row">
            <label>Name shown on signup</label>
            <input value={label} onChange={(e) => setLabel(e.target.value)} />
          </div>
          <button className="btn btn-ghost btn-sm" type="button" disabled={busy} onClick={() => void saveLabel()}>Save name</button>
          <label className="form-check">
            <input
              type="checkbox"
              checked={policy.kycRequired}
              disabled={busy || policy.backOfficeKycOnly}
              onChange={(e) => void saveKyc(e.target.checked)}
            />
            Identity KYC required before approval
          </label>
        </div>
      )}
      <table className="table" style={{ marginTop: '1rem' }}>
        <thead>
          <tr><th>Document</th><th>Required</th><th></th></tr>
        </thead>
        <tbody>
          {docs.map((row) => (
            <tr key={row.id}>
              <td>
                <input
                  defaultValue={row.documentLabel}
                  onBlur={(e) => {
                    if (e.target.value.trim() !== row.documentLabel) {
                      void saveDoc(row, e.target.value, row.mandatory);
                    }
                  }}
                />
              </td>
              <td>
                {row.requirement === 'ONE_OF' ? 'One of group' : (
                  <input
                    type="checkbox"
                    checked={row.mandatory}
                    disabled={busy}
                    onChange={(e) => void saveDoc(row, row.documentLabel, e.target.checked)}
                  />
                )}
              </td>
              <td>
                {row.custom && (
                  <button className="btn btn-ghost btn-sm" type="button" disabled={busy} onClick={() => void removeDoc(row.id)}>Remove</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="form-row" style={{ marginTop: '0.75rem' }}>
        <label>Add a document</label>
        <input value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="Document name" />
      </div>
      <label className="form-check">
        <input type="checkbox" checked={newRequired} onChange={(e) => setNewRequired(e.target.checked)} />
        Required
      </label>
      <button className="btn btn-primary btn-sm" type="button" disabled={busy || newName.trim().length < 2} onClick={() => void addDoc()}>
        Add document
      </button>
    </section>
  );
}
