<script setup lang="ts">
// Static "coach correcting a mistake" preview for the landing aperçu
// section. Shows the user mid-exercise (step 4 of "encode UTF-8" for
// U+00E9) with a wrong marker on byte 1, plus the coach explaining what's
// missing. Illustrates the "Un coach qui te corrige bit à bit" promise
// without re-doing the encoding demo from the hero.

const { t } = useI18n()

// Byte 1 — wrong: the user forgot to insert the `110` 2-byte marker
// and wrote the raw padding zeros + tail bits into the byte. The first
// 3 bits are highlighted as wrong; the trailing 5 happen to match the
// correct payload.
const byte1 = [
  { bit: '0', wrong: true },
  { bit: '0', wrong: true },
  { bit: '0', wrong: true },
  { bit: '0', wrong: false },
  { bit: '0', wrong: false },
  { bit: '0', wrong: false },
  { bit: '1', wrong: false },
  { bit: '1', wrong: false }
]

// Byte 2 — correct: `10` (cont prefix) + `101001` (payload).
type Kind = 'cont' | 'boundary' | 'payload'
const byte2: Array<{ bit: string, kind: Kind }> = [
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

    <p class="font-mono text-xs uppercase tracking-widest text-faint mb-2">
      {{ t('landing.coach.your_answer') }}
    </p>

    <div class="flex flex-col gap-1.5 mb-5">
      <div
        class="bit-row"
        style="gap: 2px;"
      >
        <span
          v-for="(b, i) in byte1"
          :key="`b1-${i}`"
          class="bit"
          :class="b.wrong ? 'bit-wrong' : 'bit-payload'"
          style="width: 1.5rem; height: 1.85rem; font-size: 13px;"
        >{{ b.bit }}</span>
      </div>
      <div
        class="bit-row"
        style="gap: 2px;"
      >
        <span
          v-for="(b, i) in byte2"
          :key="`b2-${i}`"
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
