// Reactive matchMedia. Defaults to `false` during SSR so the mobile-first
// layout is rendered first, then upgraded on client mount when the query
// actually evaluates.
export function useMediaQuery(query: string) {
  const matches = ref(false)
  if (import.meta.client) {
    const mql = window.matchMedia(query)
    matches.value = mql.matches
    const onChange = (e: MediaQueryListEvent) => {
      matches.value = e.matches
    }
    mql.addEventListener('change', onChange)
    onUnmounted(() => mql.removeEventListener('change', onChange))
  }
  return matches
}
