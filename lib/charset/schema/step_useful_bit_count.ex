defmodule Charset.Schema.StepUsefulBitCount do
  @moduledoc "Ecto schema for the `attempt_step_useful_bit_count` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_useful_bit_count" do
    field :expected, :integer
    field :user_answer, :integer
  end
end
