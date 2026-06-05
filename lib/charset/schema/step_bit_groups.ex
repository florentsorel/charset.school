defmodule Charset.Schema.StepBitGroups do
  @moduledoc "Ecto schema for the `attempt_step_bit_groups` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_bit_groups" do
    field :expected, {:array, :string}
    field :user_answer, {:array, :string}
  end
end
