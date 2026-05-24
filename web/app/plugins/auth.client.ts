// Hydrate auth state on client boot. SSR already populates user.value when
// the route's middleware runs fetchMe - and forwards Set-Cookie headers from
// the back so the browser ends up with XSRF-TOKEN for future mutations.
//
// fetchMe rethrows non-401 errors (transient 5xx, network). We catch them
// here so a flaky /auth/me doesn't break app startup on public routes —
// callers that actually need auth state will retry via the route middleware.
export default defineNuxtPlugin(async () => {
  const { fetchMe, user } = useAuth()
  if (user.value) return
  try {
    await fetchMe()
  } catch (err) {
    console.error('Boot fetchMe failed:', err)
  }
})
