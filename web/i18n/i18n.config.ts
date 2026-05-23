export default defineI18nConfig(() => ({
  legacy: false,
  datetimeFormats: {
    fr: {
      long: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      },
      short: { year: 'numeric', month: 'numeric', day: 'numeric' }
    },
    en: {
      long: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      },
      short: { year: 'numeric', month: 'numeric', day: 'numeric' }
    }
  }
}))
