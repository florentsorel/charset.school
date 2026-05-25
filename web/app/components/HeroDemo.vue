<script setup lang="ts">
const { t } = useI18n()

const bytes = [
  { bits: '11110000', markerLen: 5, isCont: false },
  { bits: '10011111', markerLen: 2, isCont: true },
  { bits: '10001110', markerLen: 2, isCont: true },
  { bits: '10001001', markerLen: 2, isCont: true }
]

function bitClass(byte: typeof bytes[number], index: number): string {
  if (byte.isCont) {
    if (index === 0) return 'bit-cont'
    if (index === 1) return 'bit-boundary'
    return 'bit-payload'
  }
  if (byte.markerLen === 0) return 'bit-payload'
  if (index < byte.markerLen - 1) return 'bit-marker'
  if (index === byte.markerLen - 1) return 'bit-boundary' // terminating 0 of the marke
  return 'bit-payload'
}
</script>

<template>
  <div
    class="hero-demo surface"
    aria-hidden="true"
  >
    <p class="font-mono text-xs uppercase tracking-widest text-faint mb-4">
      {{ t('landing.demo_label') }}
    </p>

    <div class="flex items-baseline gap-3 mb-5">
      <span
        class="codepoint-glyph leading-none"
        style="font-size: 22px;"
      >
        U+1F389
      </span>
      <span
        class="leading-none text-mute"
        style="font-size: 22px;"
      >
        🎉
      </span>
    </div>

    <div class="flex flex-col gap-1.5 mb-5">
      <div
        v-for="(byte, byteIdx) in bytes"
        :key="byteIdx"
        class="bit-row"
        style="gap: 2px;"
      >
        <span
          v-for="(bit, bitIdx) in byte.bits"
          :key="bitIdx"
          class="bit"
          :class="bitClass(byte, bitIdx)"
          style="width: 1.5rem; height: 1.85rem; font-size: 13px;"
        >
          {{ bit }}
        </span>
      </div>
    </div>

    <p class="hex text-base font-medium">
      0xF0 0x9F 0x8E 0x89
    </p>
  </div>
</template>

<style scoped>
.hero-demo {
  padding: 1.5rem;
  width: 100%;
}
</style>
