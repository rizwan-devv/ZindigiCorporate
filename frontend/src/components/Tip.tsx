import type { ReactNode } from 'react';

/** Hover tip that explains what a control does. */
export function Tip({ label, children, as = 'span' }: { label: string; children: ReactNode; as?: 'span' | 'div' }) {
  const Tag = as;
  return (
    <Tag className="ui-tip" data-tip={label} title={label}>
      {children}
    </Tag>
  );
}

/** Page-level tip chip for the current portal screen. */
export function PageTip({ text }: { text: string }) {
  if (!text) return null;
  return (
    <p className="page-tip" title={text} data-tip={text}>
      <span aria-hidden>i</span>
      {text}
    </p>
  );
}
