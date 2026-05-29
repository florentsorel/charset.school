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

// Width at which 16 bits + boundary + container padding fits on a single
// line; below this we wrap at 8 bits to stay mobile-friendly.
// Computed from CSS: 16 fixed-size bits + 16 gaps + 1 byte-boundary sep =
// 529.6px, plus 138px of container chain padding/border/track = 667.6px.
// Bump to 685 for scrollbar headroom on Windows/Linux.
export function useFitsWideBitRow() {
  return useMediaQuery('(min-width: 685px)')
}
