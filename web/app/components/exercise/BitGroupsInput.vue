<script setup lang="ts">
const props = defineProps<{
  modelValue: string[]
  groupLengths: number[]
  markerPatterns?: string[]
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

function setGroup(index: number, value: string) {
  const next = props.modelValue.slice()
  while (next.length < props.groupLengths.length) next.push('')
  next[index] = value
  emit('update:modelValue', next)
}

function markerFor(byteIndex: number): string | null {
  return props.markerPatterns?.[byteIndex] ?? null
}

function markerRole(_char: string, byteIndex: number): 'marker' | 'cont' {
  return byteIndex === 0 ? 'marker' : 'cont'
}
</script>

<template>
  <div class="bit-groups-input">
    <div
      v-for="(length, i) in groupLengths"
      :key="i"
      class="bit-groups-byte"
    >
      <span
        v-if="markerFor(i)"
        class="bit-row"
      >
        <span
          v-for="(char, ci) in markerFor(i)!.split('')"
          :key="ci"
          class="bit"
          :class="{
            'bit-marker': markerRole(char, i) === 'marker',
            'bit-cont': markerRole(char, i) === 'cont'
          }"
        >{{ char }}</span>
      </span>
      <BitInput
        :model-value="modelValue[i] ?? ''"
        :length="length"
        :disabled="disabled"
        @update:model-value="setGroup(i, $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.bit-groups-input {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 18px;
  align-items: center;
}
.bit-groups-byte {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}
</style>
