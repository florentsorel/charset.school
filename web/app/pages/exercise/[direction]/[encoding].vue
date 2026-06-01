<script setup lang="ts">
import type { Direction, EncodingSlug, ModuleId, ResumeExerciseResponse } from '~/types/exercise'
import { Directions, EncodingSlugs, MaxLevelByModule, ModuleIdByRoute, STREAK_FOR_LEVEL_UP } from '~/types/exercise'

const SUPPORTED_IN_SLICE: ModuleId[] = ['utf8-encode', 'utf8-decode', 'utf16-encode', 'utf16-decode', 'utf32-encode', 'utf32-decode']

definePageMeta({
  validate(route) {
    const d = route.params.direction
    const e = route.params.encoding
    if (typeof d !== 'string' || typeof e !== 'string') return false
    if (!(Directions as readonly string[]).includes(d)) return false
    if (!(EncodingSlugs as readonly string[]).includes(e)) return false
    const id = ModuleIdByRoute[d as Direction][e as EncodingSlug]
    return SUPPORTED_IN_SLICE.includes(id)
  }
})

const route = useRoute()
const { t } = useI18n()

const direction = route.params.direction as Direction
const encodingSlug = route.params.encoding as EncodingSlug
const moduleId = ModuleIdByRoute[direction][encodingSlug]

const exerciseApi = useExerciseApi()

// SSR-safe read-only bootstrap: progress + any resumable attempt, resolved
// DURING server render (both are GETs). This puts the correct progression
// pill and resume banner into the initial HTML - no client-side flash.
// generate() (a POST that creates an attempt) is deliberately NOT run here:
// it stays client-only so an SSR render / crawler never mutates the DB.
const { data: bootstrap, error: bootstrapError } = await useAsyncData(`exercise-bootstrap-${moduleId}`, async () => {
  const [progressRes, currentRes] = await Promise.all([
    exerciseApi.progress(),
    exerciseApi.current(moduleId)
  ])
  return {
    progress: progressRes.progress.find(p => p.moduleId === moduleId) ?? null,
    resumable: currentRes.attempt
  }
})

// Progression state - the back drives advancement, the front only displays.
// `level` is the user's current tier (1..maxLevel), `streak` the in-row
// correct count at the current tier; both seeded from the SSR bootstrap.
// `maxLevel` is structural per module, read from a static map.
const level = ref(bootstrap.value?.progress?.level ?? 1)
const streak = ref(bootstrap.value?.progress?.streak ?? 0)
const maxLevel = MaxLevelByModule[moduleId]
const atMaxLevel = computed(() => level.value >= maxLevel)
const progressionThreshold = STREAK_FOR_LEVEL_UP

// Number of steps already resolved (answered or revealed) in a resumable
// attempt. A freshly-generated attempt sits at 0.
function resolvedCount(r: ResumeExerciseResponse): number {
  return r.stepStates.filter(s => s.correct || s.revealed).length
}

const resumable = bootstrap.value?.resumable ?? null

// Only offer a resume banner when the attempt has real progress. A 0/N attempt
// (just generated, never touched) is adopted silently as the current exercise
// further down - resuming it would be identical, and the banner reads as a
// misleading "exercise in progress" right after finishing one.
const pendingResume = ref(resumable && resolvedCount(resumable) > 0 ? resumable : null)

const {
  attempt,
  currentStepIndex,
  currentInput,
  currentStatus,
  stepStates,
  inputs,
  statuses,
  loading,
  finalized,
  finalizedCorrect,
  REVEAL_THRESHOLD,
  generate,
  hydrate,
  setInput,
  submitCurrent,
  revealCurrent,
  binaryLockedPrefix
} = useExercise(moduleId)

// UTF-16/UTF-32 carry a byte order. The exercise gives it (a random BE/LE)
// rather than asking the user to pick, so we surface it in the prompt header.
// Derived from the attempt's encoding id; null for single-order encodings.
const endianness = computed<'big' | 'little' | null>(() => {
  const enc = attempt.value?.encoding ?? ''
  if (enc.endsWith('be')) return 'big'
  if (enc.endsWith('le')) return 'little'
  return null
})

// Zero-progress resumable: adopt it as the current exercise instead of
// prompting OR generating a new one (which would orphan it). Runs in setup so
// it's already rendered server-side.
if (resumable && resolvedCount(resumable) === 0) {
  hydrate(resumable)
}

const pendingResumeProgress = computed(() => {
  if (!pendingResume.value) return null
  const total = pendingResume.value.stepStates.length
  const done = pendingResume.value.stepStates.filter(s => s.correct || s.revealed).length
  // At the encoding's max level there's no further progression to chase, so
  // the banner drops the step count and just states the level.
  const atMax = pendingResume.value.level >= maxLevel
  return { done, total, level: pendingResume.value.level, atMax }
})

onMounted(async () => {
  if (pendingResume.value || attempt.value) return

  // If the SSR bootstrap failed (transient back error etc.), `pendingResume`
  // is null only because we never got a successful /current response — NOT
  // because there's no resumable attempt. Re-check on the client before
  // creating a new one, otherwise a failed bootstrap would orphan the
  // user's in-progress attempt. Also refresh the progression the bootstrap
  // couldn't provide.
  if (bootstrapError.value) {
    const { attempt: recovered } = await exerciseApi.current(moduleId)
    if (recovered) {
      // Same rule as setup: prompt only if there's real progress, else adopt.
      if (resolvedCount(recovered) > 0) pendingResume.value = recovered
      else hydrate(recovered)
      return
    }
    await refreshProgress()
  }

  // No resumable attempt → create a fresh one. The POST stays client-side
  // (never during SSR) so renders / crawlers don't spawn attempts.
  await generate()
})

async function resumePending() {
  if (!pendingResume.value) return
  level.value = pendingResume.value.level
  hydrate(pendingResume.value)
  pendingResume.value = null
}

async function discardPendingAndGenerate() {
  pendingResume.value = null
  await generate()
}

function resolvedInput(index: number) {
  return statuses.value[index]?.userInput ?? inputs.value[index] ?? null
}

function formatResolvedValue(index: number): string {
  const revealed = statuses.value[index]?.revealedAnswer
  if (revealed?.type === 'format' && revealed.value) return revealed.value
  const input = resolvedInput(index)
  return input?.type === 'format' && input.value ? input.value : ''
}

function binaryResolvedValue(index: number): string {
  const revealed = statuses.value[index]?.revealedAnswer
  if (revealed?.type === 'binary' && revealed.value) return revealed.value
  const input = resolvedInput(index)
  return input?.type === 'binary' ? input.bits : ''
}

function bitGroupsResolvedValue(index: number): string[] {
  const revealed = statuses.value[index]?.revealedAnswer
  if (revealed?.type === 'bit-groups' && revealed.groups) return revealed.groups
  const input = resolvedInput(index)
  return input?.type === 'bit-groups' ? input.groups : []
}

function hexBytesResolvedValue(index: number): number[] {
  const revealed = statuses.value[index]?.revealedAnswer
  if (revealed?.type === 'hex-bytes' && revealed.bytes) return revealed.bytes
  const input = resolvedInput(index)
  return input?.type === 'hex-bytes' ? input.bytes : []
}

function codePointResolvedValue(index: number): number {
  const revealed = statuses.value[index]?.revealedAnswer
  if (revealed?.type === 'code-point' && revealed.codePoint != null) return revealed.codePoint
  const input = resolvedInput(index)
  return input?.type === 'code-point' && input.codePoint != null ? input.codePoint : 0
}

function offsetResolvedValue(index: number): number {
  const revealed = statuses.value[index]?.revealedAnswer
  if (revealed?.type === 'offset' && revealed.offset != null) return revealed.offset
  const input = resolvedInput(index)
  return input?.type === 'offset' && input.value != null ? input.value : 0
}

// UTF-8 marker pattern per byte: first byte has `1`+ ... `0` matching the
// byte count, continuation bytes always start with `10`.
function utf8MarkersForGroups(groupLengths: number[]): string[] {
  const byteCount = groupLengths.length
  if (byteCount <= 1) return []
  const firstMarker = '1'.repeat(byteCount) + '0'
  return [firstMarker, ...Array(byteCount - 1).fill('10')]
}

// UTF-16 supplementary: each 10-bit half is prefixed by a 6-bit surrogate
// marker - 110110 for the high surrogate (base 0xD800), 110111 for the low
// (base 0xDC00) - so marker + payload forms each 16-bit code unit. Without
// these the hex bytes can't be derived from the visible bits.
function utf16SurrogateMarkers(groupLengths: number[]): string[] {
  if (groupLengths.length !== 2) return []
  return ['110110', '110111']
}

// Marker bits shown as a fixed prefix on each bit group. UTF-8 and UTF-16
// supplementary both carry markers; other encodings render bare groups.
const isUtf8 = moduleId.startsWith('utf8')
const isUtf16 = moduleId.startsWith('utf16')
function bitGroupMarkers(groupLengths: number[]): string[] {
  if (isUtf8) return utf8MarkersForGroups(groupLengths)
  if (isUtf16) return utf16SurrogateMarkers(groupLengths)
  return []
}

// The two UTF-16 surrogate markers ARE the bases 0xD800 (high) and 0xDC00 (low)
// - i.e. marker bits + an all-zero payload. Labelling them under each group makes
// it explicit that the payload is added onto these bases.
function bitGroupLabels(groupLengths: number[]): string[] {
  if (isUtf16 && groupLengths.length === 2) return ['0xD800', '0xDC00']
  return []
}

// Some steps read differently per direction. The offset step subtracts (encode)
// vs reads the binary as hex (decode); the UTF-16 decode binary step assembles
// the two halves; and the decode code-point step is where 0x10000 is added back
// (the offset step's presence marks that supplementary case - BMP / UTF-8 have none).
function stepTitleKey(stepType: string): string {
  if (stepType === 'offset') return direction === 'decode' ? 'exercise.step_title.offset_decode' : 'exercise.step_title.offset'
  if (stepType === 'binary' && direction === 'decode' && isUtf16) return 'exercise.step_title.binary_decode'
  if (stepType === 'code-point' && (attempt.value?.steps.some(s => s.type === 'offset') ?? false)) {
    return 'exercise.step_title.code_point_decode'
  }
  return `exercise.step_title.${stepType}`
}

// Binary grouping separator. UTF-8 is byte-aligned (every 8). For UTF-16 we
// group by nibble (every 4) at the binary step: the value being converted is a
// hex number (code point - 0x10000), so 4-bit groups line up 1:1 with the hex
// digits and are easier to transcribe. The 10|10 surrogate split is shown at
// the bit-groups step instead.
const binaryBoundary = isUtf16 ? 4 : 8

type BitSegment = { length: number, role: 'marker' | 'payload' | 'cont' | 'boundary' | 'plain' }

// Colour the resolved binary by group once the user has reached the
// bit-groups step, so they can visually see where each group starts/ends
// without re-counting bits. Padding bits stay neutral.
function binarySegmentsFor(index: number): BitSegment[] | undefined {
  if (!attempt.value) return undefined
  // UTF-16 binary is grouped by nibble (hex digits), not by the 10|10 surrogate
  // split - so we keep it neutral here; the split is coloured at the bit-groups
  // step instead.
  if (isUtf16) return undefined
  const step = attempt.value.steps[index]
  if (!step || step.type !== 'binary') return undefined

  const bgIdx = attempt.value.steps.findIndex(s => s.type === 'bit-groups')
  if (bgIdx === -1) return undefined
  if (currentStepIndex.value < bgIdx) return undefined

  const bg = attempt.value.steps[bgIdx]
  if (!bg || bg.type !== 'bit-groups') return undefined

  const groupLengths = bg.groupLengths
  const useful = groupLengths.reduce((s, n) => s + n, 0)
  const padding = step.length - useful

  const palette: BitSegment['role'][] = ['payload', 'marker', 'cont', 'boundary']
  const segments: BitSegment[] = []
  if (padding > 0) segments.push({ length: padding, role: 'plain' })
  groupLengths.forEach((g, i) => {
    segments.push({ length: g, role: palette[i % palette.length]! })
  })
  return segments
}

function usefulBitCountResolvedValue(index: number): number {
  const revealed = statuses.value[index]?.revealedAnswer
  if (revealed?.type === 'useful-bit-count' && revealed.count != null) return revealed.count
  const input = resolvedInput(index)
  return input?.type === 'useful-bit-count' && input.value != null ? input.value : 0
}

// Client-side refresh after a finalized attempt: the back may have advanced
// the level / bumped the streak. Defaults are kept on transient failure.
async function refreshProgress() {
  try {
    const { progress } = await exerciseApi.progress()
    const moduleProgress = progress.find(p => p.moduleId === moduleId)
    if (moduleProgress) {
      level.value = moduleProgress.level
      streak.value = moduleProgress.streak
    }
  } catch {
    // keep current values
  }
}

async function regenerateNext() {
  pendingResume.value = null
  await generate()
  await refreshProgress()
}

useHead({
  title: () => t(`exercise.module.${moduleId}.title`)
})
</script>

<template>
  <div class="exercise-page">
    <ExerciseSubHeader
      :direction="direction"
      :encoding="encodingSlug"
      :level="level"
      :max-level="maxLevel"
      :streak="streak"
      :threshold="progressionThreshold"
      :at-max="atMaxLevel"
      @skip="regenerateNext"
    />

    <div class="exercise-container">
      <div
        v-if="pendingResume"
        class="exercise-resume-banner banner-accent"
        role="region"
        :aria-label="t('exercise.resume.banner_label')"
      >
        <p class="exercise-resume-message">
          {{ t(pendingResumeProgress?.atMax ? 'exercise.resume.message_max' : 'exercise.resume.message', {
            done: pendingResumeProgress?.done,
            total: pendingResumeProgress?.total,
            level: pendingResumeProgress?.level
          }) }}
        </p>
        <div class="exercise-resume-actions">
          <button
            type="button"
            class="btn btn-primary"
            @click="resumePending"
          >
            {{ t('exercise.resume.continue_button') }}
          </button>
          <button
            type="button"
            class="btn btn-ghost"
            @click="discardPendingAndGenerate"
          >
            {{ t('exercise.resume.new_button') }}
          </button>
        </div>
      </div>

      <header
        v-if="!pendingResume"
        class="exercise-prompt"
      >
        <p class="exercise-prompt-tag">
          {{ t('exercise.prompt_label') }}
        </p>
        <h1 class="exercise-prompt-title">
          {{ t(`exercise.module.${moduleId}.prompt`) }}
        </h1>

        <div
          v-if="attempt && attempt.direction === 'encode'"
          class="exercise-prompt-card surface-subtle"
        >
          <div>
            <p class="exercise-prompt-card-label">
              {{ t('exercise.code_point_label') }}
            </p>
            <div class="exercise-prompt-card-content">
              <span class="codepoint-glyph">{{ attempt.codePointLabel }}</span>
            </div>
          </div>
          <div v-if="endianness">
            <p class="exercise-prompt-card-label">
              {{ t('exercise.byte_order_label') }}
            </p>
            <div class="exercise-prompt-card-content">
              <span class="endianness-value">{{ t(`exercise.endianness.${endianness}`) }}</span>
            </div>
          </div>
        </div>

        <div
          v-else-if="attempt && attempt.direction === 'decode'"
          class="exercise-prompt-card surface-subtle"
        >
          <div>
            <p class="exercise-prompt-card-label">
              {{ t('exercise.bytes_label') }}
            </p>
            <div class="exercise-prompt-card-content">
              <span
                v-for="(byte, i) in attempt.bytes || []"
                :key="i"
                class="byte-display"
              >
                {{ byte.toString(16).toUpperCase().padStart(2, '0') }}
              </span>
            </div>
          </div>
          <div v-if="endianness">
            <p class="exercise-prompt-card-label">
              {{ t('exercise.byte_order_label') }}
            </p>
            <div class="exercise-prompt-card-content">
              <span class="endianness-value">{{ t(`exercise.endianness.${endianness}`) }}</span>
            </div>
          </div>
        </div>
      </header>

      <ol
        v-if="attempt"
        class="exercise-steps"
      >
        <li
          v-for="(step, index) in attempt.steps"
          :key="index"
          class="exercise-step"
        >
          <div class="exercise-step-track">
            <span
              class="step-dot"
              :class="{
                'step-dot-done': stepStates[index] === 'done',
                'step-dot-active': stepStates[index] === 'active',
                'step-dot-error': stepStates[index] === 'error',
                'step-dot-todo': stepStates[index] === 'todo'
              }"
            >
              <svg
                v-if="stepStates[index] === 'done'"
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
                v-else-if="stepStates[index] === 'error'"
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
                {{ String(index + 1).padStart(2, '0') }}
              </template>
            </span>
            <span
              v-if="index < attempt.steps.length - 1"
              class="step-connector"
              :class="{ 'step-connector-done': stepStates[index] === 'done' }"
            />
          </div>

          <div class="exercise-step-content">
            <div class="exercise-step-header">
              <span class="exercise-step-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <h2 class="exercise-step-title">
                {{ t(stepTitleKey(step.type)) }}
              </h2>
            </div>

            <div
              v-if="index === currentStepIndex && !finalized"
              class="exercise-step-input surface"
            >
              <FormatSelector
                v-if="step.type === 'format' && currentInput?.type === 'format'"
                :model-value="currentInput.value"
                :choices="step.choices"
                @update:model-value="setInput({ type: 'format', value: $event })"
              />
              <BitInput
                v-else-if="step.type === 'binary' && currentInput?.type === 'binary'"
                :model-value="currentInput.bits"
                :length="step.length"
                :boundary-every="binaryBoundary"
                :locked-prefix="binaryLockedPrefix(step)"
                @update:model-value="setInput({ type: 'binary', bits: $event })"
              />
              <BitGroupsInput
                v-else-if="step.type === 'bit-groups' && currentInput?.type === 'bit-groups'"
                :model-value="currentInput.groups"
                :group-lengths="step.groupLengths"
                :marker-patterns="bitGroupMarkers(step.groupLengths)"
                :marker-labels="bitGroupLabels(step.groupLengths)"
                @update:model-value="setInput({ type: 'bit-groups', groups: $event })"
              />
              <HexInput
                v-else-if="step.type === 'hex-bytes' && currentInput?.type === 'hex-bytes'"
                :model-value="currentInput.bytes"
                :byte-count="step.byteCount"
                @update:model-value="setInput({ type: 'hex-bytes', bytes: $event })"
              />
              <CodePointInput
                v-else-if="step.type === 'code-point' && currentInput?.type === 'code-point'"
                :model-value="currentInput.codePoint"
                @update:model-value="setInput({ type: 'code-point', codePoint: $event })"
              />
              <UsefulBitCountInput
                v-else-if="step.type === 'useful-bit-count' && currentInput?.type === 'useful-bit-count'"
                :model-value="currentInput.value"
                @update:model-value="setInput({ type: 'useful-bit-count', value: $event })"
              />
              <OffsetInput
                v-else-if="step.type === 'offset' && currentInput?.type === 'offset'"
                :model-value="currentInput.value"
                @update:model-value="setInput({ type: 'offset', value: $event })"
              />

              <FeedbackPanel
                v-if="currentStatus?.errorType"
                :error-type="currentStatus.errorType"
                :params="currentStatus.params"
                :attempts="currentStatus.attempts"
                :can-reveal="currentStatus.canReveal"
                :threshold="REVEAL_THRESHOLD"
                @reveal="revealCurrent"
              />

              <div class="exercise-step-actions">
                <button
                  type="button"
                  class="btn btn-primary"
                  :disabled="loading"
                  @click="submitCurrent"
                >
                  {{ t('exercise.submit_button') }}
                </button>
              </div>
            </div>

            <div
              v-else-if="statuses[index]?.correct"
              class="exercise-step-resolved"
            >
              <div class="exercise-step-resolved-header">
                <span
                  class="exercise-step-resolved-tag"
                  :class="{ 'exercise-step-resolved-tag-revealed': statuses[index].revealedAnswer }"
                >
                  {{ statuses[index].revealedAnswer ? t('exercise.step.revealed') : t('exercise.step.solved') }}
                </span>
              </div>
              <div class="exercise-step-resolved-answer">
                <template v-if="step.type === 'format'">
                  <span class="font-mono text-sm">{{ t(formatResolvedValue(index)) }}</span>
                </template>
                <template v-else-if="step.type === 'binary'">
                  <BitDisplay
                    :bits="binaryResolvedValue(index)"
                    :boundary-every="binaryBoundary"
                    :segments="binarySegmentsFor(index)"
                  />
                </template>
                <template v-else-if="step.type === 'bit-groups'">
                  <span class="bit-groups-display">
                    <template
                      v-for="(group, gi) in bitGroupsResolvedValue(index)"
                      :key="gi"
                    >
                      <span class="bit-groups-byte-resolved">
                        <span class="bit-groups-byte-cells">
                          <span
                            v-if="bitGroupMarkers(step.groupLengths)[gi]"
                            class="bit-row"
                          >
                            <span
                              v-for="(char, ci) in bitGroupMarkers(step.groupLengths)[gi]!.split('')"
                              :key="ci"
                              class="bit"
                              :class="{
                                'bit-marker': gi === 0,
                                'bit-cont': gi > 0
                              }"
                            >{{ char }}</span>
                          </span>
                          <BitDisplay :bits="group" />
                        </span>
                        <span
                          v-if="bitGroupLabels(step.groupLengths)[gi]"
                          class="bit-groups-byte-label"
                        >{{ bitGroupLabels(step.groupLengths)[gi] }}</span>
                      </span>
                    </template>
                  </span>
                </template>
                <template v-else-if="step.type === 'hex-bytes'">
                  <span class="bytes-display">
                    <span
                      v-for="(byte, bi) in hexBytesResolvedValue(index)"
                      :key="bi"
                      class="byte-display"
                    >
                      {{ byte.toString(16).toUpperCase().padStart(2, '0') }}
                    </span>
                  </span>
                </template>
                <template v-else-if="step.type === 'code-point'">
                  <span class="codepoint-glyph">U+{{ codePointResolvedValue(index).toString(16).toUpperCase().padStart(4, '0') }}</span>
                </template>
                <template v-else-if="step.type === 'useful-bit-count'">
                  <span class="font-mono text-sm">{{ usefulBitCountResolvedValue(index) }} {{ t('exercise.useful_bit_count_suffix') }}</span>
                </template>
                <template v-else-if="step.type === 'offset'">
                  <span class="font-mono text-sm">0x{{ offsetResolvedValue(index).toString(16).toUpperCase() }}</span>
                </template>
              </div>
            </div>
          </div>
        </li>
      </ol>

      <div
        v-if="finalized"
        class="exercise-finalized surface-subtle"
      >
        <p class="exercise-finalized-message">
          {{ finalizedCorrect ? t('exercise.finalized.success') : t('exercise.finalized.failed') }}
        </p>

        <div class="exercise-finalized-actions">
          <button
            type="button"
            class="btn btn-primary"
            :disabled="loading"
            @click="regenerateNext"
          >
            {{ t('exercise.next_button') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.exercise-page {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.exercise-container {
  /* 800px lets the 20-bit UTF-16 binary sit on a single line. Narrower
     viewports (mobile/tablet) keep width:100% and the binary row wraps via
     its autoFit flex layout - no overflow. */
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem 1.5rem 6rem;
}
.exercise-resume-banner {
  /* card shape via .banner-accent */
  padding: 1.25rem 1.4rem;
  margin-bottom: 2rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.exercise-resume-message {
  color: var(--color-accent);
  font-size: 0.95rem;
  line-height: 1.5;
  max-width: 540px;
}
.exercise-resume-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.exercise-prompt {
  margin-bottom: 2.5rem;
}
.exercise-prompt-tag {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--color-faint);
  margin-bottom: 0.6rem;
}
.exercise-prompt-title {
  font-size: 1.35rem;
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: 1.4rem;
}
.exercise-prompt-card {
  padding: 1.4rem 1.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  align-items: start;
}
.exercise-prompt-card-label {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-faint);
  margin-bottom: 0.4rem;
}
.exercise-prompt-card-content {
  display: flex;
  gap: 0.6rem;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 1.4rem;
}
.codepoint-glyph {
  font-family: var(--font-mono);
  font-size: 2.1rem;
  line-height: 1;
  letter-spacing: 0.04em;
}
.byte-display {
  display: inline-flex;
  padding: 0.3rem 0.65rem;
  background: var(--color-surface);
  border: 1px solid var(--color-rule);
  border-radius: 6px;
  font-weight: 600;
}
.endianness-value {
  font-size: 1.05rem;
  line-height: 1;
  font-weight: 600;
}
.exercise-steps {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}
.exercise-step {
  display: flex;
  gap: 1.25rem;
}
.exercise-step-track {
  display: flex;
  flex-direction: column;
  align-items: center;
}
/* Phones: drop the step timeline (dot + connector) so the bit rows get the full
   column width. The step number still shows in the header. Rule placed after
   the base .exercise-step-track so it wins at equal specificity when matched. */
@media (max-width: 455px) {
  .exercise-step-track {
    display: none;
  }
  .exercise-container {
    padding-left: 1rem;
    padding-right: 1rem;
  }
}
.exercise-step-content {
  flex: 1;
  padding-bottom: 1.5rem;
}
.exercise-step-header {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  margin-bottom: 0.65rem;
}
.exercise-step-index {
  font-family: var(--font-mono);
  font-size: 0.78rem;
  color: var(--color-faint);
}
.exercise-step-title {
  font-size: 0.95rem;
  font-weight: 500;
}
.exercise-step-input {
  /* card shape via .surface */
  padding: 1.1rem 1.25rem;
}
.exercise-step-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}
.exercise-step-resolved {
  font-size: 0.9rem;
  color: var(--color-mute);
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}
.exercise-step-resolved-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.exercise-step-resolved-tag {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-ok);
  background: var(--color-ok-soft);
  padding: 0.18rem 0.5rem;
  border-radius: 4px;
}
.exercise-step-resolved-tag-revealed {
  color: var(--color-warn);
  background: var(--color-warn-soft);
}
.exercise-step-resolved-answer {
  padding: 0.6rem 0;
  display: inline-flex;
  align-items: center;
}
.bit-groups-display {
  display: inline-flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}
.bit-groups-byte-resolved {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.3rem;
}
.bit-groups-byte-resolved .bit-groups-byte-cells {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}
.bit-groups-byte-label {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--color-bit-marker);
}
.bytes-display {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
}
.exercise-finalized {
  /* card shape via .surface-subtle */
  margin-top: 2rem;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.exercise-finalized-message {
  font-size: 0.95rem;
}
.exercise-finalized-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
</style>
