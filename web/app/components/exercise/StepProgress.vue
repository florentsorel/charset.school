<script setup lang="ts">
export type StepState = 'done' | 'active' | 'error' | 'todo'

defineProps<{
  states: StepState[]
}>()
</script>

<template>
  <ol
    class="step-progress"
    :aria-label="$t('exercise.step_progress_label')"
  >
    <li
      v-for="(state, i) in states"
      :key="i"
      class="step-progress-item"
    >
      <span
        class="step-dot"
        :class="{
          'step-dot-done': state === 'done',
          'step-dot-active': state === 'active',
          'step-dot-error': state === 'error',
          'step-dot-todo': state === 'todo'
        }"
        :aria-label="$t(`exercise.step_state.${state}`, { n: i + 1 })"
      >
        <svg
          v-if="state === 'done'"
          width="12"
          height="12"
          viewBox="0 0 12 12"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M2.5 6.2l2.5 2.3L9.5 3.5" />
        </svg>
        <svg
          v-else-if="state === 'error'"
          width="11"
          height="11"
          viewBox="0 0 11 11"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M3 3l5 5M8 3l-5 5" />
        </svg>
        <template v-else>
          {{ String(i + 1).padStart(2, '0') }}
        </template>
      </span>
      <span
        v-if="i < states.length - 1"
        class="step-connector"
        :class="{ 'step-connector-done': state === 'done' }"
      />
    </li>
  </ol>
</template>

<style scoped>
.step-progress {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}
.step-progress-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
