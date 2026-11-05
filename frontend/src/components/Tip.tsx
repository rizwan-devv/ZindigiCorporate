import type { ReactNode } from 'react';

/** Accessible hover tip. Prefer short verbs: what the control does. */
export function Tip({ label, children }: { label: string; children: ReactNode }) {
  return (
    <span className="ui-tip" title={label} data-tip={label}>
      {children}
    </span>
  );
}
