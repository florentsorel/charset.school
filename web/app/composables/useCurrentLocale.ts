import { isLocale, type Locale } from '~/types/locale'

// Typed wrapper over `useI18n().locale` — vue-i18n exposes it as a plain
// string, but we know our app only allows the locales declared in nuxt.config.
// Falls back to 'en' (default) if i18n is in an unexpected state.
export function useCurrentLocale() {
  const { locale } = useI18n()
  return computed<Locale>(() => (isLocale(locale.value) ? locale.value : 'en'))
}
