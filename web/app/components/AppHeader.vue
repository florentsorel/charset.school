<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const { isAuthenticated, user, logout } = useAuth()

async function onLogout() {
  try {
    await logout()
  } finally {
    await navigateTo(localePath('/login'))
  }
}

// First letters of `user.name`, falling back to the email prefix. Code-point
// aware (Array.from) so emojis / multi-byte glyphs don't break.
const initials = computed(() => {
  if (!user.value) return ''
  const source = user.value.name?.trim() || user.value.email
  const words = source.split(/\s+/)
  return words
    .map(w => Array.from(w)[0] ?? '')
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase()
})

// Dropdown items for the authenticated user avatar (desktop) and the mobile
// hamburger. The locale switcher used to live here too, but it overrode the
// per-user locale stored in the profile — language is now managed solely
// from the profile settings.
const authedItems = computed(() => [
  { label: t('header.my_profile'), icon: 'i-lucide-user', to: localePath('/profile') },
  { label: t('auth.logout'), icon: 'i-lucide-log-out', onSelect: onLogout }
])

const guestItems = computed(() => [
  { label: t('auth.login'), icon: 'i-lucide-log-in', to: localePath('/login') },
  { label: t('auth.register'), icon: 'i-lucide-user-plus', to: localePath('/register') }
])

const sandboxItem = computed(() => ({
  label: t('header.sandbox'),
  icon: 'i-lucide-flask-conical',
  to: localePath('/sandbox')
}))

const mobileMenuItems = computed(() => [
  sandboxItem.value,
  ...(isAuthenticated.value ? authedItems.value : guestItems.value)
])

// Custom styling that overrides Nuxt UI's default dropdown look so it
// matches our design tokens (surface/rule/subtle colors, sans font,
// understated shadow). Same shape used by the desktop user menu.
const dropdownUi = {
  content: 'app-dropdown',
  item: 'app-dropdown-item',
  itemLeadingIcon: 'app-dropdown-item-icon',
  separator: 'app-dropdown-separator'
}

// Mobile variant: full viewport width, no rounded corners, only top/bottom
// borders so the panel touches the screen edges.
const mobileDropdownUi = {
  ...dropdownUi,
  content: 'app-dropdown app-dropdown--full'
}
</script>

<template>
  <header class="sticky top-0 z-40 bg-page/85 backdrop-blur border-b border-rule">
    <div class="mx-auto max-w-6xl px-4 sm:px-6 py-4 flex items-center justify-between gap-4">
      <NuxtLink
        :to="localePath('/')"
        class="flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <img
          src="/logo.svg"
          width="22"
          height="13"
          alt="charset.school"
          aria-hidden="true"
        >
        <span class="font-mono text-md leading-none lowercase">charset.school</span>
      </NuxtLink>

      <!-- Desktop nav (≥ md). Wrapped in a div because UDropdownMenu's
           `class` prop lands on its inner trigger, not on a wrapper —
           making conditional visibility unreliable when applied directly. -->
      <div class="hidden md:flex items-center gap-2.5">
        <NuxtLink
          :to="localePath('/sandbox')"
          class="btn btn-soft text-sm"
        >
          {{ t('header.sandbox') }}
        </NuxtLink>

        <template v-if="!isAuthenticated">
          <NuxtLink
            :to="localePath('/login')"
            class="btn btn-ghost text-sm"
          >
            {{ t('auth.login') }}
          </NuxtLink>
          <NuxtLink
            :to="localePath('/register')"
            class="btn btn-primary text-sm"
          >
            {{ t('auth.register') }}
          </NuxtLink>
        </template>

        <UDropdownMenu
          v-else
          :items="authedItems"
          :ui="dropdownUi"
        >
          <button
            type="button"
            class="user-avatar"
            :aria-label="t('header.user_menu')"
            :title="t('header.user_menu')"
          >
            {{ initials }}
          </button>
        </UDropdownMenu>
      </div>

      <!-- Mobile hamburger (< md). Same wrapper-div trick. The dropdown
           panel is forced to full viewport width via the .app-dropdown--full
           modifier, anchored to the right edge of the trigger. -->
      <div class="md:hidden">
        <UDropdownMenu
          :items="mobileMenuItems"
          :content="{ side: 'bottom', align: 'end', sideOffset: 12, collisionPadding: 0 }"
          :ui="mobileDropdownUi"
        >
          <button
            type="button"
            class="hamburger-btn"
            :aria-label="t('header.menu')"
          >
            <UIcon
              name="i-lucide-menu"
              class="w-5 h-5"
            />
          </button>
        </UDropdownMenu>
      </div>
    </div>
  </header>
</template>
