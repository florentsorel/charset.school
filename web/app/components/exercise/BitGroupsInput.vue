<script setup lang="ts">
const props = defineProps<{
  modelValue: string[]
  groupLengths: number[]
  markerPatterns?: string[]
  // Per-group caption shown under each group (e.g. the surrogate base 0xD800).
  markerLabels?: string[]
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

type BitInputExposed = { focusFirst: () => void, focusLast: () => void }
// Nullable: Vue invokes the function ref with `null` on unmount/patch, and we
// assign it so stale instances are cleared (e.g. when groupLengths changes).
const groups = ref<(BitInputExposed | null)[]>([])

function setGroup(index: number, value: string) {
  const next = props.modelValue.slice()
  while (next.length < props.groupLengths.length) next.push('')
  next[index] = value
  emit('update:modelValue', next)
}

// Bits flow as one sequence across groups: filling the last bit of a group
// jumps to the first bit of the next, and going back past the first bit
// lands on the last bit of the previous group.
function onOverflow(index: number) {
  groups.value[index + 1]?.focusFirst()
}

function onUnderflow(index: number) {
  groups.value[index - 1]?.focusLast()
}

function markerFor(byteIndex: number): string | null {
  return props.markerPatterns?.[byteIndex] ?? null
}

function markerRole(_char: string, byteIndex: number): 'marker' | 'cont' {
  return byteIndex === 0 ? 'marker' : 'cont'
}

function labelFor(byteIndex: number): string | null {
  return props.markerLabels?.[byteIndex] ?? null
}
</script>

<template>
  <div class="bit-groups-input">
    <div
      v-for="(length, i) in groupLengths"
      :key="i"
      class="bit-groups-byte"
    >
      <span class="bit-groups-byte-cells">
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
          :ref="el => { groups[i] = (el as unknown as BitInputExposed | null) }"
          :model-value="modelValue[i] ?? ''"
          :length="length"
          :disabled="disabled"
          @update:model-value="setGroup(i, $event)"
          @overflow="onOverflow(i)"
          @underflow="onUnderflow(i)"
        />
      </span>
      <span
        v-if="labelFor(i)"
        class="bit-groups-byte-label"
      >{{ labelFor(i) }}</span>
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
  flex-direction: column;
  align-items: flex-start;
  gap: 0.3rem;
}
.bit-groups-byte-cells {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}
.bit-groups-byte-label {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--color-bit-marker);
}
</style>
