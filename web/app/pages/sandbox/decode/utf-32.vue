<script setup lang="ts">
definePageMeta({ auth: false, layout: 'sandbox' })

const { t, te } = useI18n()
const { $api } = useNuxtApp()

useHead({
  title: () => t('sandbox.decode_utf32.title')
})

type Endian = 'big' | 'little'

type SandboxStep
  = | { type: 'binary', value: string, length: number }
    | { type: 'code-point', value: number }
    | { type: 'endianness', value: Endian }

type Utf32DecodeSandboxResponse = {
  bytes: number[]
  codepoint: number
  codepointLabel: string
  glyph: string | null
  label: string | null
  endian: Endian
  steps: SandboxStep[]
}

type DecodeApiError = {
  errorType: 'sandbox.bytes-invalid' | 'sandbox.endian-invalid' | 'encoding.not-decodable'
  params?: {
    reason?: string
  }
}

const route = useRoute()
const router = useRouter()
const initialBytes = (() => {
  const q = route.query.bytes
  // Default sample is the little-endian encoding of "tada" (U+1F389)
  // so the page lands on a supplementary-plane example on first load.
  return typeof q === 'string' && q.length > 0 ? q : '89 F3 01 00'
})()
const initialEndian: Endian = (() => {
  const q = route.query.endian
  // Default to little-endian: it matches what users encounter most in
  // practice (Windows internals, modern x86/ARM in-memory).
  return q === 'big' ? 'big' : 'little'
})()

const rawBytes = ref(initialBytes)
const endian = ref<Endian>(initialEndian)

const debouncedBytes = ref(rawBytes.value)
let debounceTimer: ReturnType<typeof setTimeout> | null = null
watch(rawBytes, (v) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debouncedBytes.value = v
  }, 250)
})
onBeforeUnmount(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})

watch([debouncedBytes, endian], ([v, e]) => {
  router.replace({ query: { ...route.query, bytes: v, endian: e } })
})

// Wrap response + error in a discriminated union so the error state is
// serialized by Nuxt in the SSR payload. A separate `apiError` ref would
// reset to its default on client hydration (refs aren't restored from
// the payload).
type DecodeOutcome
  = | { ok: true, response: Utf32DecodeSandboxResponse }
    | { ok: false, error: { kind: 'bytes' | 'decoder' | 'endian', reason: string } }

const { data: outcome } = await useAsyncData<DecodeOutcome | null>(
  'sandbox-utf32-decode',
  async (): Promise<DecodeOutcome> => {
    try {
      const result = await $api<Utf32DecodeSandboxResponse>(
        `/sandbox/decode/utf-32?bytes=${encodeURIComponent(debouncedBytes.value)}&endian=${endian.value}`
      )
      return { ok: true, response: result }
    } catch (err) {
      const body = (err as { data?: DecodeApiError } | null | undefined)?.data
      if (body?.errorType === 'sandbox.bytes-invalid') {
        return { ok: false, error: { kind: 'bytes', reason: body.params?.reason ?? 'invalid_hex' } }
      }
      if (body?.errorType === 'sandbox.endian-invalid') {
        return { ok: false, error: { kind: 'endian', reason: 'invalid' } }
      }
      if (body?.errorType === 'encoding.not-decodable') {
        return { ok: false, error: { kind: 'decoder', reason: 'decoder' } }
      }
      return { ok: false, error: { kind: 'decoder', reason: 'decoder' } }
    }
  },
  { watch: [debouncedBytes, endian] }
)

const response = computed(() => (outcome.value?.ok ? outcome.value.response : null))
const apiError = computed(() => (outcome.value && !outcome.value.ok ? outcome.value.error : null))

const errorMessage = computed(() => {
  if (!apiError.value) return null
  if (apiError.value.kind === 'endian') return t('sandbox.endian_error_invalid')
  return t(`sandbox.decode_utf32.errors.${apiError.value.reason}`)
})

const labelDescription = computed(() => {
  const short = response.value?.label
  if (!short) return null
  const key = `sandbox.labels.${short}`
  return te(key) ? t(key) : t('sandbox.non_printable')
})

function stepOfType<T extends SandboxStep['type']>(type: T) {
  return (s: SandboxStep): s is Extract<SandboxStep, { type: T }> => s.type === type
}

const endianStep = computed(() => response.value?.steps.find(stepOfType('endianness')))
const binaryStep = computed(() => response.value?.steps.find(stepOfType('binary')))
const codePointStep = computed(() => response.value?.steps.find(stepOfType('code-point')))

function byteLabel(n: number): string {
  return n <= 1 ? t('sandbox.byte_singular', { n }) : t('sandbox.byte_plural', { n })
}

const bytesBinary = computed(() => {
  if (!response.value) return []
  return response.value.bytes.map(b => b.toString(2).padStart(8, '0'))
})

function hexLabel(byte: number): string {
  return `0x${byte.toString(16).toUpperCase().padStart(2, '0')}`
}
</script>

<template>
  <main class="min-w-0">
    <header class="mb-10">
      <p class="font-mono text-xs uppercase tracking-widest text-faint mb-3">
        sandbox
      </p>
      <h1 class="text-3xl font-medium leading-tight tracking-tight mb-2">
        {{ t('sandbox.decode_utf32.title') }}
      </h1>
      <p class="text-sm text-mute leading-relaxed max-w-xl">
        {{ t('sandbox.decode_utf32.subtitle') }}
      </p>
    </header>

    <section class="surface-subtle p-5 sm:p-6 mb-8">
      <div class="field">
        <label
          class="field-label"
          for="sb-bytes"
        >
          {{ t('sandbox.decode_utf32.input_label') }}
        </label>
        <input
          id="sb-bytes"
          v-model="rawBytes"
          class="field-input field-input-mono"
          :class="{ 'is-error': !!apiError }"
          autocomplete="off"
          spellcheck="false"
          :placeholder="t('sandbox.decode_utf32.input_placeholder')"
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
          <InlineDesc :text="t('sandbox.decode_utf32.input_help')" />
        </p>
      </div>

      <div class="field mt-4">
        <span class="field-label">{{ t('sandbox.endian_label') }}</span>
        <div class="flex gap-2 flex-wrap">
          <label class="endian-radio">
            <input
              v-model="endian"
              type="radio"
              value="big"
            >
            <span>{{ t('sandbox.endian_big') }}</span>
          </label>
          <label class="endian-radio">
            <input
              v-model="endian"
              type="radio"
              value="little"
            >
            <span>{{ t('sandbox.endian_little') }}</span>
          </label>
        </div>
      </div>
    </section>

    <template v-if="response">
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
            {{ byteLabel(response.bytes.length) }}
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
              {{ t('sandbox.decode_utf32.bytes_label') }}
            </p>
            <p class="hex text-xl font-medium">
              {{ response.bytes.map(hexLabel).join(' ') }}
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
        </div>
      </section>

      <section>
        <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
          {{ t('sandbox.steps_title') }}
        </h2>

        <ol class="flex flex-col gap-0">
          <li
            v-if="endianStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">01</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.decode_utf32.step.endianness') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t(`sandbox.decode_utf32.step.endianness_desc.${endianStep.value}`)" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {{ endianStep.value === 'big' ? t('sandbox.endian_big') : t('sandbox.endian_little') }}
                </span>
              </div>
            </div>
          </li>

          <li
            v-if="binaryStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">02</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.decode_utf32.step.binary') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t('sandbox.decode_utf32.step.binary_desc')" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{{ binaryStep.value }}</span>
              </div>
            </div>
          </li>

          <li
            v-if="codePointStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">03</span>
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.decode_utf32.step.code_point') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc
                  :text="t('sandbox.decode_utf32.step.code_point_desc', {
                    decimal: codePointStep.value,
                    cp: response.codepointLabel
                  })"
                />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base font-medium">{{ response.codepointLabel }}</span>
              </div>
            </div>
          </li>
        </ol>
      </section>
    </template>
  </main>
</template>
