<script setup lang="ts">
defineProps<{
  moduleId: string
  level: number
  streak: number
}>()

defineEmits<{
  skip: []
}>()

const { t } = useI18n()
</script>

<template>
  <div class="exercise-sub-header">
    <div class="exercise-sub-header-inner">
      <nav
        class="exercise-breadcrumb"
        :aria-label="t('exercise.breadcrumb_label')"
      >
        <span class="crumb-root">{{ t('exercise.breadcrumb_root') }}</span>
        <span class="separator">/</span>
        <span class="crumb font-mono">{{ moduleId }}</span>
        <span class="separator">/</span>
        <span class="crumb crumb-level font-mono">{{ t('exercise.breadcrumb_level', { n: level }) }}</span>
      </nav>
      <div class="exercise-stats">
        <span
          v-if="streak > 0"
          class="font-mono"
        >{{ t('exercise.streak', { n: streak }) }}</span>
        <span
          v-if="streak > 0"
          class="separator-dot"
        >·</span>
        <button
          type="button"
          class="btn-quiet btn"
          @click="$emit('skip')"
        >
          {{ t('exercise.skip_button') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.exercise-sub-header {
  background: var(--color-page);
}
.exercise-sub-header-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0.85rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.exercise-breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.875rem;
}
.separator,
.separator-dot {
  color: var(--color-faint);
}
.crumb-root {
  font-size: 0.84rem;
  color: var(--color-mute);
}
.crumb {
  font-size: 0.84rem;
}
.crumb-level {
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  background: var(--color-subtle);
}
.exercise-stats {
  display: inline-flex;
  align-items: center;
  gap: 0.85rem;
  font-size: 0.825rem;
  color: var(--color-mute);
}
</style>
