defmodule Charset.Exercise.ValidationResultTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.ValidationResult

  describe "correct/0" do
    test "produces ok=true, no error_type, no params" do
      result = ValidationResult.correct()
      assert result.ok
      assert result.error_type == nil
      assert result.params == %{}
    end
  end

  describe "incorrect/2" do
    test "with error_type only produces ok=false and empty params" do
      result = ValidationResult.incorrect("some.error")
      refute result.ok
      assert result.error_type == "some.error"
      assert result.params == %{}
    end

    test "with error_type and params" do
      result = ValidationResult.incorrect("some.error", %{"got" => "x"})
      refute result.ok
      assert result.error_type == "some.error"
      assert result.params == %{"got" => "x"}
    end
  end

  # Equality behavior - the validator tests compare whole structs.
  describe "equality" do
    test "two correct() results are equal" do
      assert ValidationResult.correct() == ValidationResult.correct()
    end

    test "two incorrect() with same error_type and params are equal" do
      assert ValidationResult.incorrect("a", %{"k" => "v"}) ==
               ValidationResult.incorrect("a", %{"k" => "v"})
    end

    test "correct() != incorrect()" do
      refute ValidationResult.correct() == ValidationResult.incorrect("a")
    end
  end
end
