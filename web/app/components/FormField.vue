<script setup lang="ts">
defineProps<{
  label: string
  name: string
  type?: string
  autocomplete?: string
  error?: string
  mono?: boolean
}>()

const model = defineModel<string>({ required: true })

const id = useId()
const errorId = computed(() => `${id}-error`)
</script>

<template>
  <div class="field">
    <label
      class="field-label"
      :for="id"
    >{{ label }}</label>
    <input
      :id="id"
      v-model="model"
      :type="type ?? 'text'"
      :name="name"
      :autocomplete="autocomplete"
      class="field-input"
      :class="{ 'field-input-mono': mono, 'is-error': !!error }"
      :aria-invalid="!!error"
      :aria-describedby="error ? errorId : undefined"
    >
    <p
      v-if="error"
      :id="errorId"
      class="field-error"
    >
      {{ error }}
    </p>
  </div>
</template>
