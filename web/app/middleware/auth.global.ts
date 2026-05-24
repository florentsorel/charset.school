// Auth modes via `definePageMeta({ auth: ... })`:
//   `false`   — public, no check
//   `'guest'` — guest-only, authenticated users redirected to /me
//   omitted   — protected, unauthenticated users redirected to /login
//
// Runs on SSR too so guest/protected redirects come back as 302 before the
// browser ever sees the wrong page (fetchMe forwards the cookie in SSR).
export default defineNuxtRouteMiddleware(async (to) => {
  const authMode = to.meta.auth as 'guest' | false | undefined
  if (authMode === false) return

  const { user, fetchMe } = useAuth()
  if (!user.value) await fetchMe()
  const localePath = useLocalePath()

  if (authMode === 'guest') {
    if (user.value) return navigateTo(localePath('/profile'))
    return
  }

  if (!user.value) return navigateTo(localePath('/login'))
})
