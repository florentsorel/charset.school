defmodule Charset.Exercise.AttemptStep do
  @moduledoc """
  The persisted state of one step inside an attempt: the step definition
  (with its server-side `expected`), the submission counters driving the
  graded hints, and the user's latest answer.
  """

  alias Charset.Exercise.Answer
  alias Charset.Exercise.Step

  @enforce_keys [:id, :position, :step, :correct, :attempts, :revealed]
  defstruct [
    :id,
    :position,
    :step,
    :correct,
    :error_type,
    :attempts,
    :revealed,
    :user_answer
  ]

  @type t :: %__MODULE__{
          id: integer(),
          position: non_neg_integer(),
          step: Step.t(),
          correct: boolean(),
          error_type: String.t() | nil,
          attempts: non_neg_integer(),
          revealed: boolean(),
          user_answer: Answer.t() | nil
        }

  @doc "A step is resolved once it is correct or its answer was revealed."
  @spec resolved?(t()) :: boolean()
  def resolved?(%__MODULE__{} = step), do: step.correct or step.revealed
end
