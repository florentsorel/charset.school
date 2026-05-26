<script setup lang="ts">
definePageMeta({ auth: false, layout: 'sandbox' })

const { t } = useI18n()
const { $api } = useNuxtApp()

useHead({
  title: () => t('sandbox.title')
})

type SandboxStep
  = | { type: 'format', choices: string[], value: string }
    | { type: 'binary', value: string, length: number }
    | { type: 'bit-groups', groups: string[] }
    | { type: 'hex-bytes', bytes: number[] }

type Utf8SandboxResponse = {
  codepoint: number
  codepointLabel: string
  glyph: string | null
  label: string | null
  steps: SandboxStep[]
}

type SandboxParseError = {
  errorType: 'sandbox.input-invalid'
  params: {
    reason: 'empty' | 'unparseable' | 'out_of_range' | 'surrogate'
  }
}

const rawInput = ref('U+00E9')

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

const apiError = ref<SandboxParseError['params']['reason'] | null>(null)

const { data: response } = await useAsyncData<Utf8SandboxResponse | null>(
  'sandbox-utf8',
  async () => {
    try {
      const result = await $api<Utf8SandboxResponse>(
        `/sandbox/encode/utf-8?input=${encodeURIComponent(debouncedInput.value)}`
      )
      apiError.value = null
      return result
    } catch (err) {
      const body = (err as { data?: SandboxParseError } | null | undefined)?.data
      if (body?.errorType === 'sandbox.input-invalid') {
        apiError.value = body.params.reason
      }
      return null
    }
  },
  { watch: [debouncedInput] }
)

const errorMessage = computed(() => {
  if (!apiError.value) return null
  return t(`sandbox.errors.${apiError.value}`)
})

const formatStep = computed(() => response.value?.steps.find(s => s.type === 'format'))
const binaryStep = computed(() => response.value?.steps.find(s => s.type === 'binary'))
const bitGroupsStep = computed(() => response.value?.steps.find(s => s.type === 'bit-groups'))
const hexBytesStep = computed(() => response.value?.steps.find(s => s.type === 'hex-bytes'))

function byteLabel(n: number): string {
  return n <= 1 ? t('sandbox.byte_singular', { n }) : t('sandbox.byte_plural', { n })
}

function bitClass(byteIndex: number, bitIndex: number, byteCount: number): string {
  // Continuation byte: `10` is the full continuation marker (2 bits).
  if (byteIndex > 0) {
    return bitIndex < 2 ? 'bit-cont' : 'bit-payload'
  }
  // Leader byte marker is the complete pattern, terminator included:
  //   1-byte = `0`     (1 bit)
  //   2-byte = `110`   (3 bits)
  //   3-byte = `1110`  (4 bits)
  //   4-byte = `11110` (5 bits)
  // The trailing `0` is what distinguishes e.g. `110` (2-byte) from `111`
  // (3+ byte) - it belongs to the marker, not to the payload.
  const markerLen = byteCount === 1 ? 1 : byteCount + 1
  return bitIndex < markerLen ? 'bit-marker' : 'bit-payload'
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
        {{ t('sandbox.title') }}
      </h1>
      <p class="text-sm text-mute leading-relaxed max-w-xl">
        {{ t('sandbox.subtitle') }}
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

    <!-- Result + verbose toggle -->
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
              class="text-mute flex items-baseline gap-2"
            >
              <span class="font-mono font-medium text-xl">{{ response.label }}</span>
              <span class="text-xs text-faint">({{ t('sandbox.non_printable') }})</span>
            </span>
          </div>
          <span class="font-mono text-xs uppercase tracking-widest text-faint">
            {{ byteLabel(hexBytesStep.bytes.length) }}
          </span>
        </div>

        <div class="flex flex-col gap-3">
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
                  class="bit bit-sm"
                  :class="bitClass(byteIdx, bitIdx, bytesBinary.length)"
                >
                  {{ bit }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Step-by-step timeline (à la theme/encode.html), no success/error
           icons since there's nothing to validate in the sandbox - just
           numbered neutral dots. -->
      <section>
        <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
          {{ t('sandbox.steps_title') }}
        </h2>

        <ol class="flex flex-col gap-0">
          <li
            v-if="formatStep"
            class="flex gap-5"
          >
            <div class="flex flex-col items-center">
              <span class="step-dot">01</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.step.format') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t(`sandbox.step.format_desc.${hexBytesStep.bytes.length}`)" />
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
              <span class="step-dot">02</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.step.binary') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t('sandbox.step.binary_desc', { cp: response.codepointLabel, bits: binaryStep.length })" />
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
              <span class="step-dot">03</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.step.split') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t('sandbox.step.split_desc', { slots: bitGroupsStep.groups.map(g => g.length).join(' + ') })" />
              </p>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{{ bitGroupsStep.groups.join(' | ') }}</span>
              </div>
            </div>
          </li>

          <li class="flex gap-5">
            <div class="flex flex-col items-center">
              <span class="step-dot">{{ bitGroupsStep ? '04' : '03' }}</span>
              <span class="step-connector" />
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.step.markers') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t(`sandbox.step.markers_desc.${hexBytesStep.bytes.length}`)" />
              </p>
              <div class="surface px-5 py-4">
                <div class="flex flex-col gap-2">
                  <div
                    v-for="(byte, byteIdx) in bytesBinary"
                    :key="byteIdx"
                    class="flex items-center gap-3 flex-wrap"
                  >
                    <span class="font-mono text-xs text-faint min-w-[3.5rem]">
                      byte {{ byteIdx + 1 }}
                    </span>
                    <div class="bit-row bit-row-tight">
                      <span
                        v-for="(bit, bitIdx) in byte"
                        :key="bitIdx"
                        class="bit bit-sm"
                        :class="bitClass(byteIdx, bitIdx, bytesBinary.length)"
                      >
                        {{ bit }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </li>

          <li class="flex gap-5">
            <div class="flex flex-col items-center">
              <span class="step-dot">{{ bitGroupsStep ? '05' : '04' }}</span>
            </div>
            <div class="flex-1 pb-6">
              <h3 class="text-sm font-medium mb-2 mt-1">
                {{ t('sandbox.step.hex') }}
              </h3>
              <p class="text-sm text-mute mb-3 leading-relaxed">
                <InlineDesc :text="t(hexBytesStep.bytes.length === 1 ? 'sandbox.step.hex_desc_singular' : 'sandbox.step.hex_desc_plural', { count: hexBytesStep.bytes.length })" />
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
