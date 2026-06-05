defmodule Charset.Schema.StepEndianness do
  @moduledoc "Ecto schema for the `attempt_step_endianness` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_endianness" do
    field :expected, :string
    field :user_answer, :string
  end
end
