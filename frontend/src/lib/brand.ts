export const BRAND = {
  name: 'Zindigi Corporate',
  short: 'ZINDIGI CORPORATE',
  backoffice: 'ZINDIGI BACKOFFICE',
  tagline: 'Banking Simple Karo',
  receiptFooter: 'Zindigi Corporate · Keep this receipt for your records',
  /** Official mark from https://zindigi.pk/ */
  logoSrc: '/zindigi-logo.png',
  faviconSrc: '/favicon.png',
} as const;

/** Brand colors extracted from zindigi.pk CSS (primary teal family). */
export const BRAND_COLORS = {
  teal: '#7ACBC7',
  tealStrong: '#5FB3AF',
  tealBright: '#40CECE',
  ink: '#1A1A1A',
  softBg: '#F3FAF9',
} as const;
