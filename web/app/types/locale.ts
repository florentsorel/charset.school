// Supported UI locales. Kept in sync with nuxt.config.ts → i18n.locales.
export const LOCALES = ['fr', 'en'] as const
export type Locale = typeof LOCALES[number]

export function isLocale(value: string): value is Locale {
  return (LOCALES as readonly string[]).includes(value)
}
