<script setup lang="ts">
const props = defineProps<{
  text: string
}>()

const parts = computed(() =>
  props.text.split('`').map((text, i) => ({ text, code: i % 2 === 1 }))
)
</script>

<template>
  <span>
    <template
      v-for="(p, i) in parts"
      :key="i"
    >
      <code
        v-if="p.code"
        class="inline-code"
      >{{ p.text }}</code>
      <template v-else>{{ p.text }}</template>
    </template>
  </span>
</template>
