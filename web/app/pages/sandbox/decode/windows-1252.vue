<script setup lang="ts">
definePageMeta({ auth: false, layout: 'sandbox' })

const { t, te } = useI18n()
const { $api } = useNuxtApp()

useHead({
  title: () => t('sandbox.decode_windows1252.title')
})

type SandboxStep
  = | { type: 'binary', value: string, length: number }
    | { type: 'code-point', value: number }

type Windows1252DecodeSandboxResponse = {
  bytes: number[]
  codepoint: number
  codepointLabel: string
  glyph: string | null
  label: string | null
  steps: SandboxStep[]
}

type DecodeApiError = {
  errorType: 'sandbox.bytes-invalid' | 'encoding.not-decodable'
  params?: {
    reason?: string
  }
}

const route = useRoute()
const router = useRouter()
const initialBytes = (() => {
  const q = route.query.bytes
  // Default to 0x80 (Euro) - the Microsoft extension that nobody
  // expects when used to plain Latin-1.
  return typeof q === 'string' && q.length > 0 ? q : '80'
})()
const rawBytes = ref(initialBytes)

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

watch(debouncedBytes, (v) => {
  router.replace({ query: { ...route.query, bytes: v } })
})

type DecodeOutcome
  = | { ok: true, response: Windows1252DecodeSandboxResponse }
    | { ok: false, error: { kind: 'bytes' | 'decoder', reason: string } }

const { data: outcome } = await useAsyncData<DecodeOutcome | null>(
  'sandbox-windows1252-decode',
  async (): Promise<DecodeOutcome> => {
    try {
      const result = await $api<Windows1252DecodeSandboxResponse>(
        `/sandbox/decode/windows-1252?bytes=${encodeURIComponent(debouncedBytes.value)}`
      )
      return { ok: true, response: result }
    } catch (err) {
      const body = (err as { data?: DecodeApiError } | null | undefined)?.data
      if (body?.errorType === 'sandbox.bytes-invalid') {
        return { ok: false, error: { kind: 'bytes', reason: body.params?.reason ?? 'invalid_hex' } }
      }
      if (body?.errorType === 'encoding.not-decodable') {
        return { ok: false, error: { kind: 'decoder', reason: 'not-decodable' } }
      }
      return { ok: false, error: { kind: 'decoder', reason: 'not-decodable' } }
    }
  },
  { watch: [debouncedBytes] }
)

const response = computed(() => (outcome.value?.ok ? outcome.value.response : null))
const apiError = computed(() => (outcome.value && !outcome.value.ok ? outcome.value.error : null))

const errorMessage = computed(() => {
  if (!apiError.value) return null
  return t(`sandbox.decode_windows1252.errors.${apiError.value.reason}`)
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

// Highlight which CP1252 range the byte belongs to, mirroring the encode
// page. The "special" branch is the one users come here to understand.
const byteRange = computed<'ascii' | 'special' | 'latin1' | null>(() => {
  if (!response.value || response.value.bytes.length === 0) return null
  const b = response.value.bytes[0]!
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
        {{ t('sandbox.decode_windows1252.title') }}
      </h1>
      <p class="text-sm text-mute leading-relaxed max-w-xl">
        {{ t('sandbox.decode_windows1252.subtitle') }}
      </p>
    </header>

    <section class="surface-subtle p-5 sm:p-6 mb-8">
      <div class="field">
        <label
          class="field-label"
          for="sb-bytes"
        >
          {{ t('sandbox.decode_windows1252.input_label') }}
        </label>
        <input
          id="sb-bytes"
          v-model="rawBytes"
          class="field-input field-input-mono"
          :class="{ 'is-error': !!apiError }"
          autocomplete="off"
          spellcheck="false"
          :placeholder="t('sandbox.decode_windows1252.input_placeholder')"
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
          <InlineDesc :text="t('sandbox.decode_windows1252.input_help')" />
        </p>
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
              {{ t('sandbox.decode_windows1252.bytes_label') }}
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
          <div v-if="byteRange">
            <p class="font-mono text-xs uppercase tracking-widest text-faint mb-1.5">
              <InlineDesc :text="t('sandbox.decode_windows1252.range_label')" />
            </p>
            <p class="text-sm text-mute">
              <InlineDesc :text="t(`sandbox.decode_windows1252.range_desc.${byteRange}`)" />
            </p>
          </div>
        </div>
      </section>

      <!-- Step-by-step timeline: read byte as binary, then map back to
           the code point via either identity (ASCII / Latin-1 ranges) or
           the CP1252 table (0x80-0x9F). -->
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
                {{ t('sandbox.decode_windows1252.step.binary') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t('sandbox.decode_windows1252.step.binary_desc')" />
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
              <span class="step-dot">02</span>
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.decode_windows1252.step.code_point') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc
                  :text="t(`sandbox.decode_windows1252.step.code_point_desc.${byteRange ?? 'ascii'}`, {
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
