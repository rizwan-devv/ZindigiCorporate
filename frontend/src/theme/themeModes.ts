export type ThemeMode = 'dark' | 'zindigi';

/** Cycle order used by the theme switcher. */
export const THEME_ORDER: ThemeMode[] = ['zindigi', 'dark'];

export const THEME_LABELS: Record<ThemeMode, string> = {
  dark: 'Dark',
  zindigi: 'Zindigi',
};

/** Map legacy stored themes (light / d3) to the two remaining modes. */
export function migrateTheme(raw: string | null): ThemeMode | null {
  if (raw === 'zindigi' || raw === 'dark') return raw;
  if (raw === 'light') return 'zindigi';
  if (raw === 'd3') return 'dark';
  return null;
}

export function isThemeMode(raw: string | null): raw is ThemeMode {
  return raw === 'dark' || raw === 'zindigi';
}

export function isLightScheme(theme: ThemeMode): boolean {
  return theme === 'zindigi';
}
