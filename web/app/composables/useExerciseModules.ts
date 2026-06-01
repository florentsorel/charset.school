export interface ExerciseModuleLink {
  id: string
  to: string
}

export function useExerciseModules(): ExerciseModuleLink[] {
  return [
    { id: 'utf8-encode', to: '/exercise/encode/utf-8' },
    { id: 'utf8-decode', to: '/exercise/decode/utf-8' },
    { id: 'utf16-encode', to: '/exercise/encode/utf-16' },
    { id: 'utf16-decode', to: '/exercise/decode/utf-16' },
    { id: 'utf32-encode', to: '/exercise/encode/utf-32' },
    { id: 'utf32-decode', to: '/exercise/decode/utf-32' }
  ]
}
