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
    <div class="mx-auto max-w-6xl px-4 sm:px-6 py-4 flex items-center justify-between gap-4 flex-wrap">
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

      <div class="flex items-center gap-2.5">
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
        <button
          v-else
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
