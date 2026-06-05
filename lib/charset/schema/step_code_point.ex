defmodule Charset.Schema.StepCodePoint do
  @moduledoc "Ecto schema for the `attempt_step_code_point` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_code_point" do
    field :expected, :integer
    field :user_answer, :integer
  end
end
