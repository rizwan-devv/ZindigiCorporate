import { type FormEvent, useEffect, useState } from 'react';
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
  imageIds: number[];
};

export function ProductsPage() {
  const { session } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ categoryId: '', title: '', description: '', specifications: '', price: '' });

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

  async function create(e: FormEvent) {
    e.preventDefault();
    if (!session?.token) return;
    setError('');
    try {
      await api('/api/financing/products', {
        method: 'POST',
        token: session.token,
        body: JSON.stringify(form),
      });
      setForm({ categoryId: '', title: '', description: '', specifications: '', price: '' });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    }
  }

  return (
    <div className="page">
      <div className="container dash">
        <div className="panel panel--wide">
          <div className="badge">PRODUCTS</div>
          <h2 style={{ marginTop: 0 }}>Catalogue</h2>
          <p className="muted">
            Customers see a product only after a platform admin authorizes this merchant for its category.
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
            <button className="btn btn-primary" type="submit">Save product</button>
          </form>
          <ul>
            {products.map((product) => (
              <li key={product.id}>
                <strong>{product.title}</strong> · {product.price} · {product.active ? 'Active' : 'Hidden'} · {product.imageIds.length} images
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file || !session?.token) return;
                    const body = new FormData();
                    body.append('file', file);
                    try {
                      await api(`/api/financing/products/${product.id}/images`, { method: 'POST', token: session.token, body });
                      await load();
                    } catch (err) {
                      setError(err instanceof Error ? err.message : 'Upload failed');
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
