defmodule Charset.Schema.StepOffset do
  @moduledoc "Ecto schema for the `attempt_step_offset` child table."

  use Ecto.Schema

  @primary_key {:step_id, :id, autogenerate: false}
  schema "attempt_step_offset" do
    field :expected, :integer
    field :user_answer, :integer
  end
end
