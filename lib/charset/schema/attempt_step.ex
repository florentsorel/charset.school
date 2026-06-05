defmodule Charset.Schema.AttemptStep do
  @moduledoc "Ecto schema for the `attempt_steps` parent table (step_type discriminator)."

  use Ecto.Schema

  schema "attempt_steps" do
    field :attempt_id, :id
    field :position, :integer
    field :step_type, :string
    field :correct, :boolean, default: false
    field :error_type, :string
    field :attempts, :integer, default: 0
    field :revealed, :boolean, default: false
  end
end
