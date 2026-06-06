defmodule Charset.Exercise.Attempt do
  @moduledoc """
  A persisted exercise attempt: one exercise instance owned by a visitor
  token, with its step-by-step state. Pure domain struct - the Ecto mapping
  lives in `Charset.ExerciseAttempts`.
  """

  alias Charset.Exercise.AttemptStep

  @enforce_keys [
    :id,
    :token,
    :module,
    :level,
    :code_point,
    :encoding,
    :correct,
    :finalized,
    :steps,
    :created_at
  ]
  defstruct [
    :id,
    :token,
    :module,
    :level,
    :code_point,
    :encoding,
    :correct,
    :finalized,
    :duration_ms,
    :steps,
    :created_at
  ]

  @type t :: %__MODULE__{
          id: integer(),
          token: String.t(),
          module: Charset.Exercise.ExerciseModule.t(),
          level: pos_integer(),
          code_point: 0..0x10FFFF,
          encoding: Charset.Encoding.t(),
          correct: boolean(),
          finalized: boolean(),
          duration_ms: integer() | nil,
          steps: [AttemptStep.t()],
          created_at: DateTime.t()
        }
end
