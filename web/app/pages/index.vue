<script setup lang="ts">
definePageMeta({ auth: false })

const { t } = useI18n()

useHead({
  title: () => t('common.app_name')
})
</script>

<template>
  <main class="px-6 py-16 md:py-24">
    <div class="w-full max-w-[680px] mx-auto text-center">
      <h1 class="text-[40px] md:text-[48px] font-medium leading-[1.1] tracking-[-0.02em] mb-4">
        {{ t('common.app_name') }}
      </h1>
      <p class="text-[var(--color-mute)] text-[16px] md:text-[18px] leading-[1.55] max-w-[520px] mx-auto">
        Charset Playground — landing à venir.
      </p>
    </div>
  </main>
</template>
