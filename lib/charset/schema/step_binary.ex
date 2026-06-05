defmodule Charset.Schema.StepBinary do
  @moduledoc "Ecto schema for the `attempt_step_binary` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_binary" do
    field :expected, :string
    field :bit_length, :integer
    field :user_answer, :string
  end
end
