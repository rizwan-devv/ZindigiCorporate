import { BRAND } from '../lib/brand';

type Props = {
  compact?: boolean;
  className?: string;
};

/** Official Zindigi wordmark (from zindigi.pk). */
export function BrandLogo({ compact = false, className = '' }: Props) {
  return (
    <img
      src={BRAND.logoSrc}
      alt={BRAND.name}
      className={`brand-logo ${compact ? 'brand-logo--compact' : ''} ${className}`.trim()}
      width={compact ? 120 : 148}
      height={compact ? 34 : 42}
      decoding="async"
    />
  );
}
