// Universal auth bootstrap. Runs on BOTH SSR and client so user state is
// populated before any route renders — fixes the "auth flash" on public
// pages (landing) where the auth.global.ts middleware bails early on
// `auth: false` and would otherwise leave user.value null on SSR.
//
// fetchMe is idempotent (via the `auth:fetched` useState marker), so the
// SSR plugin → client hydration → route middleware chain only ever issues
// one HTTP request to /auth/me per page load.
//
// On SSR, useRequestHeaders in fetchMe forwards the SESSION cookie to the
// back so the server-side render knows who the user is. Errors are
// swallowed silently on SSR (back transiently down, etc.) — the worst
// case degrades to a "logged-out" SSR render, which the client will
// correct on hydration since the marker is reset on throw.
export default defineNuxtPlugin(async () => {
  const { fetchMe, user } = useAuth()
  if (user.value) return
  try {
    await fetchMe()
  } catch (err) {
    if (import.meta.client) {
      console.error('Boot fetchMe failed:', err)
    }
  }
})
