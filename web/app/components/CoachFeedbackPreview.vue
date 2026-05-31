<script setup lang="ts">
// Static "coach correcting a mistake" preview for the landing aperçu section.
// Mirrors the real BitGroups step ("split the useful bits") for U+00E9 (é,
// 2 bytes): the UTF-8 markers (110 / 10) are shown as FIXED context - the
// user only fills the useful payload bits - and the coach catches a wrong
// payload bit, not a marker. Illustrates "a coach that corrects you bit by
// bit" without re-doing the hero's encoding demo.

const { t } = useI18n()

type Kind = 'marker' | 'cont' | 'boundary' | 'payload' | 'wrong'

// Byte 0 (target 0xC3 = 11000011): marker `110` then payload `00011`. The
// user got the last payload bit wrong (typed 0, should be 1).
const byte0: Array<{ bit: string, kind: Kind }> = [
  { bit: '1', kind: 'marker' },
  { bit: '1', kind: 'marker' },
  { bit: '0', kind: 'boundary' },
  { bit: '0', kind: 'payload' },
  { bit: '0', kind: 'payload' },
  { bit: '0', kind: 'payload' },
  { bit: '1', kind: 'payload' },
  { bit: '0', kind: 'wrong' }
]

// Byte 1 (0xA9 = 10101001): cont marker `10` then payload `101001` - correct.
const byte1: Array<{ bit: string, kind: Kind }> = [
  { bit: '1', kind: 'cont' },
  { bit: '0', kind: 'boundary' },
  { bit: '1', kind: 'payload' },
  { bit: '0', kind: 'payload' },
  { bit: '1', kind: 'payload' },
  { bit: '0', kind: 'payload' },
  { bit: '0', kind: 'payload' },
  { bit: '1', kind: 'payload' }
]
</script>

<template>
  <div
    class="coach-card surface"
    aria-hidden="true"
  >
    <p class="font-mono text-xs uppercase tracking-widest text-faint mb-4">
      {{ t('landing.coach.step_label') }}
    </p>

    <div class="flex items-baseline gap-3 mb-4">
      <span
        class="codepoint-glyph leading-none"
        style="font-size: 20px;"
      >U+00E9</span>
      <span
        class="leading-none text-mute"
        style="font-size: 20px;"
      >é</span>
    </div>

    <div class="flex flex-col gap-1.5 mb-5">
      <div
        class="bit-row"
        style="gap: 2px;"
      >
        <span
          v-for="(b, i) in byte0"
          :key="`b0-${i}`"
          class="bit"
          :class="`bit-${b.kind}`"
          style="width: 1.5rem; height: 1.85rem; font-size: 13px;"
        >{{ b.bit }}</span>
      </div>
      <div
        class="bit-row"
        style="gap: 2px;"
      >
        <span
          v-for="(b, i) in byte1"
          :key="`b1-${i}`"
          class="bit"
          :class="`bit-${b.kind}`"
          style="width: 1.5rem; height: 1.85rem; font-size: 13px;"
        >{{ b.bit }}</span>
      </div>
    </div>

    <div class="coach-tip">
      <UIcon
        name="i-lucide-info"
        class="coach-tip-icon"
        aria-hidden="true"
      />
      <p class="coach-tip-text">
        {{ t('landing.coach.feedback') }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.coach-card {
  padding: 1.5rem;
  width: 100%;
}
.coach-tip {
  display: flex;
  gap: 0.6rem;
  align-items: flex-start;
  background: var(--color-accent-soft);
  border-left: 3px solid var(--color-accent);
  border-radius: 4px;
  padding: 0.7rem 0.9rem;
}
.coach-tip-icon {
  flex-shrink: 0;
  width: 1rem;
  height: 1rem;
  margin-top: 2px;
  color: var(--color-accent);
}
.coach-tip-text {
  font-size: 0.825rem;
  line-height: 1.55;
  color: var(--color-ink);
}
</style>
