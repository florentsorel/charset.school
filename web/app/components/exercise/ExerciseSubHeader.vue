<script setup lang="ts">
defineProps<{
  moduleId: string
  level: number
  maxLevel: number
  streak: number
  threshold: number
  atMax: boolean
  loaded: boolean
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
        <!-- Render only after progress is loaded to avoid flashing the
             default "Niveau 1 · 0/5" before the real values land. While
             loading we reserve the pill space with an unbreakable dash
             so the header height doesn't shift. -->
        <span
          v-if="!loaded"
          class="crumb crumb-level crumb-level-loading font-mono"
          aria-hidden="true"
        >&nbsp;</span>
        <span
          v-else-if="atMax"
          class="crumb crumb-level font-mono"
        >
          {{ t('exercise.progression.max', { n: level }) }}
        </span>
        <span
          v-else
          class="crumb crumb-level font-mono"
        >
          {{ t('exercise.progression.next', { level, done: streak, threshold, next: level + 1 }) }}
        </span>
      </nav>
      <div class="exercise-stats">
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
.separator {
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
.crumb-level-loading {
  /* inline-block so min-width actually reserves space (min-width is a no-op
     on inline elements). Width sized to the longer FR string
     "Niveau 1 · 0/5 avant niveau 2" so the header doesn't reflow when the
     real pill replaces it. */
  display: inline-block;
  min-width: 18ch;
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
