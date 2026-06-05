defmodule Charset.Schema.StepHexBytes do
  @moduledoc "Ecto schema for the `attempt_step_hex_bytes` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_hex_bytes" do
    field :expected, {:array, :integer}
    field :user_answer, {:array, :integer}
  end
end
