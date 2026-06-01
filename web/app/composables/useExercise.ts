import type {
  AnswerPayload,
  Direction,
  ExerciseStep,
  GenerateExerciseResponse,
  ModuleId,
  ResumeExerciseResponse,
  RevealedAnswer,
  ValidateStepResponse
} from '~/types/exercise'
import type { StepState } from '~/components/exercise/StepProgress.vue'

type StepInput
  = | { type: 'format', value: string | null }
    | { type: 'binary', bits: string }
    | { type: 'bit-groups', groups: string[] }
    | { type: 'hex-bytes', bytes: number[] }
    | { type: 'code-point', codePoint: number | null }
    | { type: 'useful-bit-count', value: number | null }
    | { type: 'offset', value: number | null }

type StepStatus = {
  correct: boolean
  attempts: number
  errorType: string | null
  params: Record<string, string>
  canReveal: boolean
  revealedAnswer: RevealedAnswer | null
  userInput: StepInput | null
}

const REVEAL_THRESHOLD = 3

export function useExercise(moduleId: ModuleId) {
  const api = useExerciseApi()

  const attempt = ref<GenerateExerciseResponse | null>(null)
  const currentStepIndex = ref(0)
  const inputs = ref<Record<number, StepInput>>({})
  const statuses = ref<Record<number, StepStatus>>({})
  const loading = ref(false)
  const finalized = ref(false)
  const finalizedCorrect = ref(false)

  const stepStates = computed<StepState[]>(() => {
    if (!attempt.value) return []
    return attempt.value.steps.map((_, i) => {
      const status = statuses.value[i]
      if (status?.correct) return 'done'
      if (i === currentStepIndex.value) {
        return status?.errorType ? 'error' : 'active'
      }
      return 'todo'
    })
  })

  const currentStep = computed<ExerciseStep | null>(() => {
    if (!attempt.value) return null
    return attempt.value.steps[currentStepIndex.value] ?? null
  })

  const currentInput = computed<StepInput | null>(() => inputs.value[currentStepIndex.value] ?? null)

  const currentStatus = computed<StepStatus | null>(() => statuses.value[currentStepIndex.value] ?? null)

  async function generate() {
    loading.value = true
    try {
      const fresh = await api.generate({ moduleId })
      const seededInputs: Record<number, StepInput> = {}
      fresh.steps.forEach((step, i) => {
        seededInputs[i] = initialInput(step, fresh.steps, fresh.direction)
      })
      attempt.value = fresh
      currentStepIndex.value = 0
      inputs.value = seededInputs
      statuses.value = {}
      finalized.value = false
      finalizedCorrect.value = false
    } finally {
      loading.value = false
    }
  }

  function hydrate(resume: ResumeExerciseResponse) {
    const seededInputs: Record<number, StepInput> = {}
    const seededStatuses: Record<number, StepStatus> = {}
    resume.steps.forEach((step, i) => {
      const state = resume.stepStates[i]
      const restoredInput = state?.userAnswer ? revealedAnswerToInput(step, state.userAnswer) : null
      seededInputs[i] = restoredInput ?? initialInput(step, resume.steps, resume.direction)
      if (state) {
        // A revealed step is resolved (read-only) even though the DB stores
        // `correct = false`. Treat it as done for rendering so the user sees
        // the revealed answer instead of a blank input.
        seededStatuses[i] = {
          correct: state.correct || state.revealed,
          attempts: state.attempts,
          errorType: state.errorType,
          params: {},
          canReveal: state.canReveal,
          revealedAnswer: state.revealedAnswer,
          userInput: restoredInput
        }
      }
    })
    const firstUnresolved = resume.steps.findIndex((_, i) => {
      const s = resume.stepStates[i]
      return !s || (!s.correct && !s.revealed)
    })

    attempt.value = {
      attemptId: resume.attemptId,
      moduleId: resume.moduleId,
      direction: resume.direction,
      level: resume.level,
      encoding: resume.encoding,
      codePoint: resume.codePoint,
      codePointLabel: resume.codePointLabel,
      bytes: resume.bytes,
      steps: resume.steps
    }
    inputs.value = seededInputs
    statuses.value = seededStatuses
    currentStepIndex.value = firstUnresolved === -1 ? resume.steps.length - 1 : firstUnresolved
    finalized.value = false
    finalizedCorrect.value = false
  }

  function setInput(input: StepInput) {
    inputs.value[currentStepIndex.value] = input
  }

  async function submitCurrent() {
    if (!attempt.value || !currentInput.value) return
    const stepIndex = currentStepIndex.value
    const submittedInput = currentInput.value
    const payload = inputToPayload(submittedInput)
    if (!payload) return

    loading.value = true
    try {
      const result = await api.validate({
        attemptId: attempt.value.attemptId,
        stepIndex,
        answer: payload
      })
      applyValidateResult(stepIndex, result, submittedInput)
    } finally {
      loading.value = false
    }
  }

  async function revealCurrent() {
    if (!attempt.value) return
    const stepIndex = currentStepIndex.value

    loading.value = true
    try {
      const result = await api.reveal({
        attemptId: attempt.value.attemptId,
        stepIndex
      })
      const existing = statuses.value[stepIndex] ?? defaultStatus()
      statuses.value[stepIndex] = {
        ...existing,
        attempts: result.attempts,
        correct: true,
        errorType: null,
        params: {},
        canReveal: false,
        revealedAnswer: result.answer,
        userInput: existing.userInput
      }
      finalized.value = result.attemptFinalized
      finalizedCorrect.value = result.attemptCorrect
      advanceOrFinalize()
    } finally {
      loading.value = false
    }
  }

  function applyValidateResult(stepIndex: number, result: ValidateStepResponse, submittedInput: StepInput) {
    const existing = statuses.value[stepIndex]
    statuses.value[stepIndex] = {
      attempts: result.attempts,
      correct: result.ok,
      errorType: result.errorType,
      params: result.params,
      canReveal: result.canReveal,
      revealedAnswer: existing?.revealedAnswer ?? null,
      userInput: submittedInput
    }
    finalized.value = result.attemptFinalized
    finalizedCorrect.value = result.attemptCorrect
    if (result.ok) advanceOrFinalize()
  }

  function advanceOrFinalize() {
    if (!attempt.value) return
    const total = attempt.value.steps.length
    if (currentStepIndex.value < total - 1) {
      currentStepIndex.value++
    }
  }

  // Locked (pre-filled) leading cells for a step's BitInput - the decode
  // binary padding zeros. 0 for everything else.
  function binaryLockedPrefix(step: ExerciseStep): number {
    if (!attempt.value) return 0
    return decodeBinaryPadding(step, attempt.value.steps, attempt.value.direction)
  }

  return {
    attempt,
    currentStepIndex,
    currentStep,
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
  }
}

function defaultStatus(): StepStatus {
  return {
    correct: false,
    attempts: 0,
    errorType: null,
    params: {},
    canReveal: false,
    revealedAnswer: null,
    userInput: null
  }
}

function revealedAnswerToInput(step: ExerciseStep, answer: RevealedAnswer): StepInput | null {
  switch (step.type) {
    case 'format':
      return answer.type === 'format' && answer.value ? { type: 'format', value: answer.value } : null
    case 'binary':
      return answer.type === 'binary' && answer.value != null ? { type: 'binary', bits: answer.value } : null
    case 'bit-groups':
      return answer.type === 'bit-groups' && answer.groups ? { type: 'bit-groups', groups: answer.groups } : null
    case 'hex-bytes':
      return answer.type === 'hex-bytes' && answer.bytes ? { type: 'hex-bytes', bytes: answer.bytes } : null
    case 'code-point':
      return answer.type === 'code-point' && answer.codePoint != null ? { type: 'code-point', codePoint: answer.codePoint } : null
    case 'useful-bit-count':
      return answer.type === 'useful-bit-count' && answer.count != null ? { type: 'useful-bit-count', value: answer.count } : null
    case 'offset':
      return answer.type === 'offset' && answer.offset != null ? { type: 'offset', value: answer.offset } : null
  }
}

// In a decode exercise, the binary step's leading bits are pure byte-alignment
// padding (zeros): length minus the useful bits (= sum of the bit-groups step's
// group lengths, which the user already established). We pre-fill that padding
// instead of making the user type it. Encode keeps padding manual (its binary
// step comes before the count, so the padding is part of the work). Returns 0
// when not applicable.
function decodeBinaryPadding(step: ExerciseStep, steps: ExerciseStep[], direction: Direction): number {
  if (direction !== 'decode' || step.type !== 'binary') return 0
  const bitGroups = steps.find(s => s.type === 'bit-groups')
  if (!bitGroups || bitGroups.type !== 'bit-groups') return 0
  const useful = bitGroups.groupLengths.reduce((sum, n) => sum + n, 0)
  return Math.max(0, step.length - useful)
}

function initialInput(step: ExerciseStep, steps: ExerciseStep[], direction: Direction): StepInput {
  switch (step.type) {
    case 'format': return { type: 'format', value: null }
    case 'binary': return { type: 'binary', bits: '0'.repeat(decodeBinaryPadding(step, steps, direction)) }
    case 'bit-groups': return { type: 'bit-groups', groups: step.groupLengths.map(() => '') }
    case 'hex-bytes': return { type: 'hex-bytes', bytes: [] }
    case 'code-point': return { type: 'code-point', codePoint: null }
    case 'useful-bit-count': return { type: 'useful-bit-count', value: null }
    case 'offset': return { type: 'offset', value: null }
  }
}

function inputToPayload(input: StepInput): AnswerPayload | null {
  switch (input.type) {
    case 'format':
      return input.value ? { type: 'format', value: input.value } : null
    case 'binary':
      return { type: 'binary', bits: input.bits }
    case 'bit-groups':
      return { type: 'bit-groups', groups: input.groups }
    case 'hex-bytes':
      if (input.bytes.some(b => b < 0)) return null
      return { type: 'hex-bytes', bytes: input.bytes }
    case 'code-point':
      return input.codePoint != null ? { type: 'code-point', codePoint: input.codePoint } : null
    case 'useful-bit-count':
      return input.value != null ? { type: 'useful-bit-count', count: input.value } : null
    case 'offset':
      return input.value != null ? { type: 'offset', offset: input.value } : null
  }
}
