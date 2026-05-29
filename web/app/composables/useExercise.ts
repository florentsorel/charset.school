import type {
  AnswerPayload,
  ExerciseStep,
  GenerateExerciseResponse,
  Granularity,
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
    | { type: 'endianness', value: 'big' | 'little' | null }

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

  async function generate(level: number, granularity: Granularity) {
    loading.value = true
    try {
      const fresh = await api.generate({ moduleId, level, granularity })
      const seededInputs: Record<number, StepInput> = {}
      fresh.steps.forEach((step, i) => {
        seededInputs[i] = initialInput(step)
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
      seededInputs[i] = restoredInput ?? initialInput(step)
      if (state) {
        seededStatuses[i] = {
          correct: state.correct,
          attempts: state.attempts,
          errorType: state.errorType,
          params: {},
          canReveal: state.canReveal,
          revealedAnswer: null,
          userInput: restoredInput
        }
      }
    })
    const firstUnresolved = resume.steps.findIndex((_, i) => {
      const s = resume.stepStates[i]
      return !s || (!s.correct && !s.revealed)
    })

    const generateLike: GenerateExerciseResponse = {
      attemptId: resume.attemptId,
      moduleId: resume.moduleId,
      direction: resume.direction,
      level: resume.level,
      granularity: resume.granularity,
      encoding: resume.encoding,
      codePoint: resume.codePoint,
      codePointLabel: resume.codePointLabel,
      bytes: resume.bytes,
      steps: resume.steps
    }
    attempt.value = generateLike
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
    revealCurrent
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
    case 'endianness':
      if (answer.type !== 'endianness' || !answer.value) return null
      if (answer.value !== 'big' && answer.value !== 'little') return null
      return { type: 'endianness', value: answer.value }
  }
}

function initialInput(step: ExerciseStep): StepInput {
  switch (step.type) {
    case 'format': return { type: 'format', value: null }
    case 'binary': return { type: 'binary', bits: '' }
    case 'bit-groups': return { type: 'bit-groups', groups: step.groupLengths.map(() => '') }
    case 'hex-bytes': return { type: 'hex-bytes', bytes: [] }
    case 'code-point': return { type: 'code-point', codePoint: null }
    case 'useful-bit-count': return { type: 'useful-bit-count', value: null }
    case 'endianness': return { type: 'endianness', value: null }
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
    case 'endianness':
      return input.value ? { type: 'endianness', value: input.value } : null
  }
}
