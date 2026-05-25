<script setup lang="ts">
definePageMeta({ auth: false })

const { t } = useI18n()

useHead({
  titleTemplate: '%siteName'
})
</script>

<template>
  <main class="px-4 sm:px-6 py-16 md:py-24">
    <div class="w-full max-w-2xl mx-auto text-center">
      <h1 class="text-4xl md:text-5xl font-medium leading-tight tracking-tight mb-4">
        {{ t('common.app_name') }}
      </h1>
      <p class="text-mute text-base md:text-lg leading-relaxed max-w-lg mx-auto">
        {{ t('landing.coming_soon') }}
      </p>
    </div>
  </main>
</template>
