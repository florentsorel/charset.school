import type {
  CurrentExerciseResponse,
  GenerateExerciseRequest,
  GenerateExerciseResponse,
  ModuleId,
  ProgressResponse,
  RevealStepRequest,
  RevealStepResponse,
  ValidateStepRequest,
  ValidateStepResponse
} from '~/types/exercise'

export function useExerciseApi() {
  const { $api } = useNuxtApp()

  function current(moduleId: ModuleId): Promise<CurrentExerciseResponse> {
    return $api<CurrentExerciseResponse>('/exercise/current', {
      method: 'GET',
      params: { moduleId }
    })
  }

  function generate(request: GenerateExerciseRequest): Promise<GenerateExerciseResponse> {
    return $api<GenerateExerciseResponse>('/exercise/generate', {
      method: 'POST',
      body: request
    })
  }

  function validate(request: ValidateStepRequest): Promise<ValidateStepResponse> {
    return $api<ValidateStepResponse>('/exercise/validate', {
      method: 'POST',
      body: request
    })
  }

  function reveal(request: RevealStepRequest): Promise<RevealStepResponse> {
    return $api<RevealStepResponse>('/exercise/reveal', {
      method: 'POST',
      body: request
    })
  }

  function progress(): Promise<ProgressResponse> {
    return $api<ProgressResponse>('/progress')
  }

  return { current, generate, validate, reveal, progress }
}
