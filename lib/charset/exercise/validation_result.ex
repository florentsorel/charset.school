defmodule Charset.Exercise.ValidationResult do
  @moduledoc """
  Outcome of validating one `(step, answer)` couple.

  Anti-cheat invariant: `params` may carry structural hints (expected length,
  count, position, public bounds) and the user's own input (`got`), but NEVER
  the canonical expected value - it would be readable in the websocket frames.

  Build through `correct/0` and `incorrect/2` only, which enforce the shape
  invariants (a correct result carries no error, an incorrect one always has
  an error type).
  """

  @enforce_keys [:ok]
  defstruct [:ok, :error_type, params: %{}]

  @type t :: %__MODULE__{
          ok: boolean(),
          error_type: String.t() | nil,
          params: %{optional(String.t()) => String.t()}
        }

  @spec correct() :: t()
  def correct, do: %__MODULE__{ok: true}

  @spec incorrect(String.t(), %{optional(String.t()) => String.t()}) :: t()
  def incorrect(error_type, params \\ %{}) when is_binary(error_type) and is_map(params) do
    %__MODULE__{ok: false, error_type: error_type, params: params}
  end
end
