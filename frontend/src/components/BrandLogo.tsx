import { BRAND } from '../lib/brand';

type Variant = 'lockup' | 'mark' | 'sidebar';

type Props = {
  variant?: Variant;
  className?: string;
  /** Override product line (e.g. Corporate / Backoffice). */
  title?: string;
  /** Pass empty string to hide subtitle. */
  subtitle?: string;
};

/**
 * Theme-safe Zindigi identity: SVG mark (no marketing plate) + text lockup.
 */
export function BrandLogo({
  variant = 'lockup',
  className = '',
  title,
  subtitle,
}: Props) {
  const heading = title ?? BRAND.name;
  const sub = subtitle === undefined ? BRAND.tagline : subtitle;

  if (variant === 'mark') {
    return (
      <span className={`brand-identity brand-identity--mark ${className}`.trim()} aria-hidden>
        <BrandMarkIcon />
      </span>
    );
  }

  return (
    <span className={`brand-identity brand-identity--${variant} ${className}`.trim()}>
      <BrandMarkIcon />
      <span className="brand-identity-text">
        <span className="brand-identity-title">{heading}</span>
        {sub ? <span className="brand-identity-sub">{sub}</span> : null}
      </span>
    </span>
  );
}

/** Geometric Z mark — teal plate, dark Z (readable on light & dark UIs). */
function BrandMarkIcon() {
  return (
    <svg
      className="brand-identity-mark"
      viewBox="0 0 40 40"
      width="40"
      height="40"
      aria-hidden
      focusable="false"
    >
      <rect width="40" height="40" rx="10" className="brand-identity-mark-plate" />
      <path
        className="brand-identity-mark-z"
        d="M11 11h18v4.2L18.2 26H29v3H11v-4.2L22.8 14H11V11z"
      />
    </svg>
  );
}
