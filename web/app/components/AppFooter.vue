<script setup lang="ts">
const { t } = useI18n()
</script>

<template>
  <footer class="border-t border-rule mt-auto">
    <div class="mx-auto max-w-6xl px-4 sm:px-6 py-10 text-sm text-mute">
      <div class="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
        <div class="max-w-md">
          <div class="font-mono text-base text-ink mb-2">
            charset.school
          </div>
          <p class="text-xs leading-relaxed">
            {{ t('footer.tagline') }}
          </p>
        </div>
        <p class="text-xs text-mute sm:text-right">
          {{ t('footer.developed_by') }}
          <a
            href="https://sorel.dev"
            target="_blank"
            rel="noopener noreferrer"
            class="text-ink hover:text-accent transition-colors"
          >Florent Sorel</a>
        </p>
      </div>
    </div>
  </footer>
</template>
