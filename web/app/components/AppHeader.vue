<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const { isAuthenticated, logout } = useAuth()

async function onLogout() {
  try {
    await logout()
  } finally {
    await navigateTo(localePath('/login'))
  }
}
</script>

<template>
  <header class="sticky top-0 z-40 bg-page/85 backdrop-blur border-b border-rule">
    <div class="mx-auto max-w-6xl px-4 sm:px-6 py-4 flex items-center justify-between">
      <NuxtLink
        :to="localePath('/')"
        class="flex items-center gap-2 hover:opacity-80 transition-opacity"
      >
        <img
          src="/logo.svg"
          width="22"
          height="13"
          alt=""
          aria-hidden="true"
        >
        <span class="font-mono text-md leading-none lowercase">charset.school</span>
      </NuxtLink>

      <div class="flex items-center gap-2.5">
        <ThemeToggle />
        <button
          v-if="isAuthenticated"
          type="button"
          class="btn btn-quiet"
          @click="onLogout"
        >
          {{ t('auth.logout') }}
        </button>
      </div>
    </div>
  </header>
</template>
