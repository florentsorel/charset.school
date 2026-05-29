export const ModuleIds = [
  'utf8-encode',
  'utf8-decode',
  'utf16-encode',
  'utf16-decode',
  'utf32-encode',
  'utf32-decode',
  'latin1-encode',
  'latin1-decode',
  'windows1252-encode',
  'windows1252-decode'
] as const

export type ModuleId = typeof ModuleIds[number]

export type Direction = 'encode' | 'decode'

export type Granularity = 'verbose' | 'standard' | 'compact'

export type StepType
  = | 'format'
    | 'binary'
    | 'bit-groups'
    | 'hex-bytes'
    | 'code-point'
    | 'useful-bit-count'
    | 'endianness'

export type ExerciseStep
  = | { type: 'format', choices: string[] }
    | { type: 'binary', length: number }
    | { type: 'bit-groups', groupLengths: number[] }
    | { type: 'hex-bytes', byteCount: number }
    | { type: 'code-point' }
    | { type: 'useful-bit-count' }
    | { type: 'endianness' }

export type GenerateExerciseRequest = {
  moduleId: ModuleId
  level: number
  granularity: Granularity
}

export type GenerateExerciseResponse = {
  attemptId: number
  moduleId: ModuleId
  direction: Direction
  level: number
  granularity: Granularity
  encoding: string
  codePoint: number | null
  codePointLabel: string | null
  bytes: number[] | null
  steps: ExerciseStep[]
}

export type StepStateDto = {
  correct: boolean
  revealed: boolean
  attempts: number
  errorType: string | null
  canReveal: boolean
  userAnswer: RevealedAnswer | null
  revealedAnswer: RevealedAnswer | null
}

export type ResumeExerciseResponse = GenerateExerciseResponse & {
  stepStates: StepStateDto[]
}

export type CurrentExerciseResponse = {
  attempt: ResumeExerciseResponse | null
}

export type AnswerPayload
  = | { type: 'format', value: string }
    | { type: 'binary', bits: string }
    | { type: 'bit-groups', groups: string[] }
    | { type: 'hex-bytes', bytes: number[] }
    | { type: 'code-point', codePoint: number }
    | { type: 'useful-bit-count', count: number }
    | { type: 'endianness', value: 'big' | 'little' }

export type ValidateStepRequest = {
  attemptId: number
  stepIndex: number
  answer: AnswerPayload
}

export type ValidateStepResponse = {
  ok: boolean
  errorType: string | null
  params: Record<string, string>
  attempts: number
  canReveal: boolean
  attemptFinalized: boolean
  attemptCorrect: boolean
}

export type RevealStepRequest = {
  attemptId: number
  stepIndex: number
}

export type RevealedAnswer = {
  type: StepType
  value?: string | null
  groups?: string[] | null
  bytes?: number[] | null
  codePoint?: number | null
  count?: number | null
}

export type RevealStepResponse = {
  stepIndex: number
  attempts: number
  answer: RevealedAnswer
  attemptFinalized: boolean
  attemptCorrect: boolean
}

export type ModuleProgress = {
  moduleId: ModuleId
  level: number
  streak: number
  attempts: number
  errors: number
  suggestedLevel: number
  lastPlayedAt: string | null
}

export type ProgressResponse = { progress: ModuleProgress[] }
