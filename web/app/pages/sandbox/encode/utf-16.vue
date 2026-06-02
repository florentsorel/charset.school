<script setup lang="ts">
definePageMeta({ layout: 'sandbox' })

const { t, te } = useI18n()
const { $api } = useNuxtApp()

useHead({
  title: () => t('sandbox.encode_utf16.title')
})

type Endian = 'big' | 'little'

type SandboxStep
  = | { type: 'format', choices: string[], value: string }
    | { type: 'binary', value: string, length: number }
    | { type: 'bit-groups', groups: string[] }
    | { type: 'hex-bytes', bytes: number[] }
    | { type: 'endianness', value: Endian }

type Utf16EncodeSandboxResponse = {
  codepoint: number
  codepointLabel: string
  glyph: string | null
  label: string | null
  endian: Endian
  steps: SandboxStep[]
}

type SandboxApiError = {
  errorType: 'sandbox.input-invalid' | 'sandbox.endian-invalid' | 'encoding.not-encodable'
  params?: {
    reason?: string
  }
}

const route = useRoute()
const router = useRouter()
const initialInput = (() => {
  const q = route.query.input
  if (typeof q !== 'string' || q.length === 0) return 'U+1F389'
  return q.replace(/^([Uu]) /, '$1+')
})()
const initialEndian: Endian = (() => {
  const q = route.query.endian
  return q === 'big' ? 'big' : 'little'
})()

const rawInput = ref(initialInput)
const endian = ref<Endian>(initialEndian)

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

watch([debouncedInput, endian], ([v, e]) => {
  router.replace({ query: { ...route.query, input: v, endian: e } })
})

type EncodeOutcome
  = | { ok: true, response: Utf16EncodeSandboxResponse }
    | { ok: false, error: string }

const { data: outcome } = await useAsyncData<EncodeOutcome | null>(
  'sandbox-utf16-encode',
  async (): Promise<EncodeOutcome> => {
    try {
      const result = await $api<Utf16EncodeSandboxResponse>(
        `/sandbox/encode/utf-16?input=${encodeURIComponent(debouncedInput.value)}&endian=${endian.value}`
      )
      return { ok: true, response: result }
    } catch (err) {
      const body = (err as { data?: SandboxApiError } | null | undefined)?.data
      if (body?.errorType === 'sandbox.input-invalid') {
        return { ok: false, error: body.params?.reason ?? 'unparseable' }
      }
      if (body?.errorType === 'sandbox.endian-invalid') {
        return { ok: false, error: 'endian' }
      }
      return { ok: false, error: 'unparseable' }
    }
  },
  { watch: [debouncedInput, endian] }
)

const response = computed(() => (outcome.value?.ok ? outcome.value.response : null))
const apiError = computed(() => (outcome.value && !outcome.value.ok ? outcome.value.error : null))

const errorMessage = computed(() => {
  if (!apiError.value) return null
  if (apiError.value === 'endian') return t('sandbox.endian_error_invalid')
  return t(`sandbox.errors.${apiError.value}`)
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
const formatStep = computed(() => response.value?.steps.find(stepOfType('format')))
const binaryStep = computed(() => response.value?.steps.find(stepOfType('binary')))
const bitGroupsStep = computed(() => response.value?.steps.find(stepOfType('bit-groups')))
const hexBytesStep = computed(() => response.value?.steps.find(stepOfType('hex-bytes')))

const codeUnitCount = computed(() => {
  if (!hexBytesStep.value) return 0
  return hexBytesStep.value.bytes.length / 2
})

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
</script>

<template>
  <main class="min-w-0">
    <header class="mb-10">
      <p class="font-mono text-xs uppercase tracking-widest text-faint mb-3">
        sandbox
      </p>
      <h1 class="text-3xl font-medium leading-tight tracking-tight mb-2">
        {{ t('sandbox.encode_utf16.title') }}
      </h1>
      <p class="text-sm text-mute leading-relaxed max-w-xl">
        {{ t('sandbox.encode_utf16.subtitle') }}
      </p>
    </header>

    <!-- Input + endianness -->
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
                {{ t('sandbox.encode_utf16.step.endianness') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t(`sandbox.encode_utf16.step.endianness_desc.${endianStep.value}`)" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {{ endianStep.value === 'big' ? t('sandbox.endian_big') : t('sandbox.endian_little') }}
                </span>
              </div>
            </div>
          </li>

          <li
            v-if="formatStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">02</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.encode_utf16.step.format') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t(`sandbox.encode_utf16.step.format_desc.${codeUnitCount}`)" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">{{ t(formatStep.value) }}</span>
              </div>
            </div>
          </li>

          <li
            v-if="binaryStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">03</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.encode_utf16.step.binary') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc
                  :text="codeUnitCount === 1
                    ? t('sandbox.encode_utf16.step.binary_desc_1', { cp: response.codepointLabel })
                    : t('sandbox.encode_utf16.step.binary_desc_2')"
                />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{{ binaryStep.value }}</span>
              </div>
            </div>
          </li>

          <li
            v-if="bitGroupsStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">04</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.encode_utf16.step.bit_groups') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t('sandbox.encode_utf16.step.bit_groups_desc')" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{{ bitGroupsStep.groups.join(' | ') }}</span>
              </div>
            </div>
          </li>

          <li class="flex gap-5">
            <div class="flex flex-col items-center">
              <span class="step-dot">{{ bitGroupsStep ? '05' : '04' }}</span>
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.encode_utf16.step.hex') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc
                  :text="codeUnitCount === 1
                    ? t('sandbox.encode_utf16.step.hex_desc_1')
                    : t('sandbox.encode_utf16.step.hex_desc_2')"
                />
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
