defmodule Charset.Schema.StepFormat do
  @moduledoc "Ecto schema for the `attempt_step_format` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_format" do
    field :choices, {:array, :string}
    field :expected, :string
    field :user_answer, :string
  end
end
