import { type FormEvent, useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../lib/api';
import { useAuth } from '../../auth/AuthContext';

type Category = { id: number; name: string };
type Product = {
  id: number;
  categoryId: number;
  title: string;
  description?: string;
  specifications?: string;
  price: number;
  active: boolean;
  shownToCustomers?: boolean;
  imageIds: number[];
};

export function ProductsPage() {
  const { session } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ categoryId: '', title: '', description: '', specifications: '', price: '' });
  const [imageFile, setImageFile] = useState<File | null>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);

  async function load() {
    if (!session?.token) return;
    const [cats, rows] = await Promise.all([
      api<Category[]>('/api/financing/categories', { token: session.token }),
      api<Product[]>('/api/financing/products', { token: session.token }),
    ]);
    setCategories(cats);
    setProducts(rows);
  }

  useEffect(() => {
    load().catch((err) => setError(err.message));
  }, [session?.token]);

  if (!session) return <Navigate to="/login" replace />;

  async function uploadImage(productId: number, file: File) {
    if (!session?.token) return;
    const body = new FormData();
    body.append('file', file);
    await api(`/api/financing/products/${productId}/images`, { method: 'POST', token: session.token, body });
  }

  async function create(e: FormEvent) {
    e.preventDefault();
    if (!session?.token) return;
    setError('');
    setBusy(true);
    try {
      const saved = await api<Product>('/api/financing/products', {
        method: 'POST',
        token: session.token,
        body: JSON.stringify(form),
      });
      if (imageFile && saved?.id) {
        await uploadImage(saved.id, imageFile);
      }
      setForm({ categoryId: '', title: '', description: '', specifications: '', price: '' });
      setImageFile(null);
      if (imageInputRef.current) imageInputRef.current.value = '';
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="container dash">
        <div className="panel panel--wide">
          <div className="badge" title="Merchant catalogue for EV bike financing">PRODUCTS</div>
          <h2 style={{ marginTop: 0 }} title="Products you sell under your own merchant account">Catalogue</h2>
          <p className="muted">
            Save the title, description, price, and optional photo. Active products appear in the customer app for your shop.
          </p>
          {error && <p className="error">{error}</p>}
          <form onSubmit={create}>
            <div className="form-row">
              <label>Category</label>
              <select required value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
                <option value="">Select</option>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div className="form-row">
              <label>Title</label>
              <input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </div>
            <div className="form-row">
              <label>Description</label>
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>
            <div className="form-row">
              <label>Specifications</label>
              <textarea value={form.specifications} onChange={(e) => setForm({ ...form, specifications: e.target.value })} />
            </div>
            <div className="form-row">
              <label>Price</label>
              <input required value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
            </div>
            <div className="form-row">
              <label>Product photo</label>
              <input
                ref={imageInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                title="Optional photo shown in the financing app and on the receipt"
                onChange={(e) => setImageFile(e.target.files?.[0] || null)}
              />
              <p className="muted" style={{ marginTop: 6 }}>
                Optional. JPEG, PNG, or WebP. You can add more photos on the product below after saving.
              </p>
            </div>
            <button
              className="btn btn-primary"
              type="submit"
              disabled={busy}
              title="Save this bike under your merchant account"
            >
              {busy ? 'Saving…' : 'Save product'}
            </button>
          </form>
          <ul>
            {products.map((product) => (
              <li key={product.id}>
                <strong>{product.title}</strong> · {product.price} · {product.active ? 'Active' : 'Inactive'} · {product.imageIds.length} images
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  title="Add another product photo"
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file || !session?.token) return;
                    try {
                      await uploadImage(product.id, file);
                      await load();
                    } catch (err) {
                      setError(err instanceof Error ? err.message : 'Upload failed');
                    } finally {
                      e.target.value = '';
                    }
                  }}
                />
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
