defmodule Charset.Exercise.GenerationError do
  @moduledoc """
  Raised when an exercise cannot be generated (unknown level for the
  encoding's tier set). Always a caller bug - the progress layer drives the
  level and is capped by `ExerciseModule.max_level/1`.
  """

  alias Charset.Encoding

  @type t :: %__MODULE__{
          encoding: Encoding.t(),
          level: integer(),
          reason: String.t()
        }

  defexception [:encoding, :level, :reason]

  @impl Exception
  def message(%__MODULE__{encoding: encoding, level: level, reason: reason}) do
    "Cannot generate exercise for #{Encoding.id(encoding)} level #{level}: #{reason}"
  end
end
