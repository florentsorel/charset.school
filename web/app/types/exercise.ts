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

// URL slug ↔ backend encoding id. Mirrors the sandbox URL convention
// (`utf-8`, `utf-16`, `windows-1252`, ...) so the routes feel consistent.
export const EncodingSlugs = ['utf-8', 'utf-16', 'utf-32', 'latin1', 'windows-1252'] as const
export type EncodingSlug = typeof EncodingSlugs[number]

export const Directions = ['encode', 'decode'] as const

// (direction, encoding-slug) → backend ModuleId. Used by the
// `/exercise/[direction]/[encoding]` route to look up the module the page
// should drive.
export const ModuleIdByRoute: Record<Direction, Record<EncodingSlug, ModuleId>> = {
  encode: {
    'utf-8': 'utf8-encode',
    'utf-16': 'utf16-encode',
    'utf-32': 'utf32-encode',
    'latin1': 'latin1-encode',
    'windows-1252': 'windows1252-encode'
  },
  decode: {
    'utf-8': 'utf8-decode',
    'utf-16': 'utf16-decode',
    'utf-32': 'utf32-decode',
    'latin1': 'latin1-decode',
    'windows-1252': 'windows1252-decode'
  }
}

// Threshold to advance one level (mirrors ModuleProgress.STREAK_FOR_LEVEL_UP).
// The back owns the actual advancement; the front only uses this constant
// to display "Niveau X · Y/N avant niveau X+1" in the progression indicator.
export const STREAK_FOR_LEVEL_UP = 5

// Per-encoding max level. Mirrors the back's per-encoding Level enums
// (Utf8Level / Utf16Level / etc. via ExerciseModule.maxLevel). Structural
// constant - safe to keep static on the front.
export const MaxLevelByModule: Record<ModuleId, number> = {
  'utf8-encode': 4,
  'utf8-decode': 4,
  'utf16-encode': 2,
  'utf16-decode': 2,
  'utf32-encode': 2,
  'utf32-decode': 2,
  'latin1-encode': 2,
  'latin1-decode': 2,
  'windows1252-encode': 2,
  'windows1252-decode': 2
}

export type StepType
  = | 'format'
    | 'binary'
    | 'bit-groups'
    | 'hex-bytes'
    | 'code-point'
    | 'useful-bit-count'
    | 'offset'

export type ExerciseStep
  = | { type: 'format', choices: string[] }
    | { type: 'binary', length: number }
    | { type: 'bit-groups', groupLengths: number[] }
    | { type: 'hex-bytes', byteCount: number }
    | { type: 'code-point' }
    | { type: 'useful-bit-count' }
    | { type: 'offset' }

export type GenerateExerciseRequest = {
  moduleId: ModuleId
}

export type GenerateExerciseResponse = {
  attemptId: number
  moduleId: ModuleId
  direction: Direction
  level: number
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
    | { type: 'offset', offset: number }

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
  offset?: number | null
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
  lastPlayedAt: string | null
}

export type ProgressResponse = { progress: ModuleProgress[] }
