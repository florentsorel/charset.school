<script setup lang="ts">
definePageMeta({ layout: 'sandbox' })

const { t, te } = useI18n()
const { $api } = useNuxtApp()

useHead({
  title: () => t('sandbox.encode_windows1252.title')
})

type SandboxStep
  = | { type: 'binary', value: string, length: number }
    | { type: 'hex-bytes', bytes: number[] }

type Windows1252EncodeSandboxResponse = {
  codepoint: number
  codepointLabel: string
  glyph: string | null
  label: string | null
  steps: SandboxStep[]
}

type SandboxApiError = {
  errorType: 'sandbox.input-invalid' | 'encoding.not-encodable'
  params?: {
    reason?: string
  }
}

const route = useRoute()
const router = useRouter()
const initialRawInput = (() => {
  const q = route.query.input
  // Default to U+20AC (Euro) - the canonical Windows-1252 Microsoft
  // extension at byte 0x80, perfect to demonstrate what makes CP1252
  // different from plain Latin-1.
  if (typeof q !== 'string' || q.length === 0) return 'U+20AC'
  // `+` in query strings is decoded as a literal space per RFC, so a URL
  // like `?input=U+20AC` (without proper %2B encoding) lands here as
  // "U 20AC". Repair only the start-of-string `U ` / `u ` pattern.
  return q.replace(/^([Uu]) /, '$1+')
})()
const rawInput = ref(initialRawInput)

const debouncedInput = ref(rawInput.value)
let debounceTimer: ReturnType<typeof setTimeout> | null = null
watch(rawInput, (v) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debouncedInput.value = v
  }, 250)
})
onBeforeUnmount(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})

watch(debouncedInput, (v) => {
  router.replace({ query: { ...route.query, input: v } })
})

// Discriminated union mirrored on the UTF-8/16 sandbox pages - keeps
// the error state in the SSR payload (a separate ref would reset on
// client hydration).
type EncodeOutcome
  = | { ok: true, response: Windows1252EncodeSandboxResponse }
    | { ok: false, error: { kind: 'parse' | 'encoder', reason: string } }

const { data: outcome } = await useAsyncData<EncodeOutcome | null>(
  'sandbox-windows1252-encode',
  async (): Promise<EncodeOutcome> => {
    try {
      const result = await $api<Windows1252EncodeSandboxResponse>(
        `/sandbox/encode/windows-1252?input=${encodeURIComponent(debouncedInput.value)}`
      )
      return { ok: true, response: result }
    } catch (err) {
      const body = (err as { data?: SandboxApiError } | null | undefined)?.data
      if (body?.errorType === 'sandbox.input-invalid') {
        return { ok: false, error: { kind: 'parse', reason: body.params?.reason ?? 'unparseable' } }
      }
      if (body?.errorType === 'encoding.not-encodable') {
        return { ok: false, error: { kind: 'encoder', reason: 'not-encodable' } }
      }
      return { ok: false, error: { kind: 'parse', reason: 'unparseable' } }
    }
  },
  { watch: [debouncedInput] }
)

const response = computed(() => (outcome.value?.ok ? outcome.value.response : null))
const apiError = computed(() => (outcome.value && !outcome.value.ok ? outcome.value.error : null))

const errorMessage = computed(() => {
  if (!apiError.value) return null
  if (apiError.value.kind === 'encoder') return t('sandbox.encode_windows1252.errors.not-encodable')
  return t(`sandbox.errors.${apiError.value.reason}`)
})

// Long-form expansion of the short mnemonic label (e.g. `PUA` -> `Private
// Use Area`). Falls back to the generic "non imprimable" suffix when no
// expansion is registered for the label.
const labelDescription = computed(() => {
  const short = response.value?.label
  if (!short) return null
  const key = `sandbox.labels.${short}`
  return te(key) ? t(key) : t('sandbox.non_printable')
})

function stepOfType<T extends SandboxStep['type']>(type: T) {
  return (s: SandboxStep): s is Extract<SandboxStep, { type: T }> => s.type === type
}

const binaryStep = computed(() => response.value?.steps.find(stepOfType('binary')))
const hexBytesStep = computed(() => response.value?.steps.find(stepOfType('hex-bytes')))

function byteLabel(n: number): string {
  return n <= 1 ? t('sandbox.byte_singular', { n }) : t('sandbox.byte_plural', { n })
}

const bytesBinary = computed(() => {
  if (!hexBytesStep.value) return []
  return hexBytesStep.value.bytes.map(b => b.toString(2).padStart(8, '0'))
})

function hexLabel(byte: number): string {
  return `0x${byte.toString(16).toUpperCase().padStart(2, '0')}`
}

// Highlight which CP1252 range the byte belongs to, so the pedagogical
// goal (showing the Microsoft extension at 0x80-0x9F) is visible at a
// glance. ASCII / Latin-1 identity ranges get a neutral tag; the special
// block gets a distinctive one.
const byteRange = computed<'ascii' | 'special' | 'latin1' | null>(() => {
  if (!hexBytesStep.value || hexBytesStep.value.bytes.length === 0) return null
  const b = hexBytesStep.value.bytes[0]!
  if (b <= 0x7F) return 'ascii'
  if (b <= 0x9F) return 'special'
  return 'latin1'
})
</script>

<template>
  <main class="min-w-0">
    <header class="mb-10">
      <p class="font-mono text-xs uppercase tracking-widest text-faint mb-3">
        sandbox
      </p>
      <h1 class="text-3xl font-medium leading-tight tracking-tight mb-2">
        {{ t('sandbox.encode_windows1252.title') }}
      </h1>
      <p class="text-sm text-mute leading-relaxed max-w-xl">
        {{ t('sandbox.encode_windows1252.subtitle') }}
      </p>
    </header>

    <!-- Input -->
    <section class="surface-subtle p-5 sm:p-6 mb-8">
      <div class="field">
        <label
          class="field-label"
          for="sb-cp"
        >
          {{ t('sandbox.input_label') }}
        </label>
        <input
          id="sb-cp"
          v-model="rawInput"
          class="field-input field-input-mono"
          :class="{ 'is-error': !!apiError }"
          autocomplete="off"
          spellcheck="false"
          :placeholder="t('sandbox.input_placeholder')"
        >
        <p
          v-if="errorMessage"
          class="field-error"
        >
          {{ errorMessage }}
        </p>
        <p
          v-else
          class="field-help"
        >
          {{ t('sandbox.input_help') }}
        </p>
      </div>
    </section>

    <template v-if="response && hexBytesStep">
      <section class="section-card mb-6">
        <div class="flex items-baseline justify-between mb-4 gap-3 flex-wrap">
          <div class="flex items-baseline gap-3 flex-wrap">
            <span class="codepoint-glyph codepoint-label-lg">
              {{ response.codepointLabel }}
            </span>
            <span
              v-if="response.glyph"
              class="codepoint-label-lg text-mute"
            >
              {{ response.glyph }}
            </span>
            <span
              v-else-if="response.label"
              class="text-mute flex items-baseline gap-2 flex-wrap"
            >
              <span class="font-mono font-medium text-xl">{{ response.label }}</span>
              <span class="text-xs text-faint">({{ labelDescription }})</span>
            </span>
          </div>
          <span class="font-mono text-xs uppercase tracking-widest text-faint">
            {{ byteLabel(hexBytesStep.bytes.length) }}
          </span>
        </div>

        <div class="flex flex-col gap-3">
          <div>
            <p class="font-mono text-xs uppercase tracking-widest text-faint mb-1.5">
              {{ t('sandbox.decimal_label') }}
            </p>
            <p class="hex text-xl font-medium">
              {{ response.codepoint }}
            </p>
          </div>
          <div>
            <p class="font-mono text-xs uppercase tracking-widest text-faint mb-1.5">
              {{ t('sandbox.hex_label') }}
            </p>
            <p class="hex text-xl font-medium">
              {{ hexBytesStep.bytes.map(hexLabel).join(' ') }}
            </p>
          </div>
          <div>
            <p class="font-mono text-xs uppercase tracking-widest text-faint mb-1.5">
              {{ t('sandbox.binary_label') }}
            </p>
            <div class="flex flex-col gap-1.5">
              <div
                v-for="(byte, byteIdx) in bytesBinary"
                :key="byteIdx"
                class="bit-row bit-row-tight"
              >
                <span
                  v-for="(bit, bitIdx) in byte"
                  :key="bitIdx"
                  class="bit bit-sm bit-payload"
                >
                  {{ bit }}
                </span>
              </div>
            </div>
          </div>
          <div v-if="byteRange">
            <p class="font-mono text-xs uppercase tracking-widest text-faint mb-1.5">
              <InlineDesc :text="t('sandbox.encode_windows1252.range_label')" />
            </p>
            <p class="text-sm text-mute">
              <InlineDesc :text="t(`sandbox.encode_windows1252.range_desc.${byteRange}`)" />
            </p>
          </div>
        </div>
      </section>

      <!-- Step-by-step timeline: simpler than UTF-8/16 (no markers, no
           surrogate split) - just binary and hex. -->
      <section>
        <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
          {{ t('sandbox.steps_title') }}
        </h2>

        <ol class="flex flex-col gap-0">
          <li
            v-if="binaryStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">01</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.encode_windows1252.step.binary') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t(`sandbox.encode_windows1252.step.binary_desc.${byteRange ?? 'ascii'}`, { cp: response.codepointLabel })" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{{ binaryStep.value }}</span>
              </div>
            </div>
          </li>

          <li class="flex gap-5">
            <div class="flex flex-col items-center">
              <span class="step-dot">02</span>
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.encode_windows1252.step.hex') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t('sandbox.encode_windows1252.step.hex_desc')" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base font-medium">{{ hexBytesStep.bytes.map(hexLabel).join(' ') }}</span>
              </div>
            </div>
          </li>
        </ol>
      </section>
    </template>
  </main>
</template>
