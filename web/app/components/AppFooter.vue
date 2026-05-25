<script setup lang="ts">
const { t } = useI18n()
</script>

<template>
  <footer class="border-t border-rule mt-auto">
    <div class="mx-auto max-w-6xl px-4 sm:px-6 py-10 text-sm text-mute">
      <div class="max-w-md">
        <div class="font-mono text-base text-ink mb-2">
          charset.school
        </div>
        <p class="text-xs leading-relaxed">
          {{ t('footer.tagline') }}
        </p>
      </div>
    </div>
  </footer>
</template>
