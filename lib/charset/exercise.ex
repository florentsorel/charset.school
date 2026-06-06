defmodule Charset.Exercise do
  @moduledoc """
  A single exercise instance, broken down into a sequence of pedagogical steps.

  Two directions:

    * `:encode` - the user receives a code point and must produce its bytes
      in the target encoding (`bytes` is nil)
    * `:decode` - the user receives `bytes` and must identify which code
      point they decode to

  Both directions reuse the same step structs for their composition - only
  the input shown to the user differs.

  Produced by `Charset.Exercise.Generator.ExerciseGenerator`. Consumed by the
  UI (which renders the input + the step widgets) and by `AnswerValidator`
  (which checks each step answer against the step's `expected`).
  """

  alias Charset.Exercise.Step

  @enforce_keys [:direction, :code_point, :encoding, :level, :steps]
  defstruct [:direction, :bytes, :code_point, :encoding, :level, :steps]

  @type t :: %__MODULE__{
          direction: :encode | :decode,
          bytes: binary() | nil,
          code_point: 0..0x10FFFF,
          encoding: Charset.Encoding.t(),
          level: pos_integer(),
          steps: [Step.t()]
        }

  @spec encode(integer(), Charset.Encoding.t(), pos_integer(), [Step.t()]) :: t()
  def encode(code_point, encoding, level, steps) do
    %__MODULE__{
      direction: :encode,
      code_point: code_point,
      encoding: encoding,
      level: level,
      steps: steps
    }
  end

  @spec decode(binary(), integer(), Charset.Encoding.t(), pos_integer(), [Step.t()]) :: t()
  def decode(bytes, code_point, encoding, level, steps) do
    %__MODULE__{
      direction: :decode,
      bytes: bytes,
      code_point: code_point,
      encoding: encoding,
      level: level,
      steps: steps
    }
  end
end
