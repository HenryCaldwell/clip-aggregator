package info.henrycaldwell.streamline.config;

import java.util.List;
import java.util.function.Predicate;

/**
 * Interface for enforcing a value constraint on a string configuration key.
 *
 * This interface defines a contract for testing whether a string satisfies a
 * declared restriction.
 */
public interface StringConstraint {

  /**
   * Tests whether the input value satisfies this constraint.
   *
   * @param value A string representing the value to test.
   * @return {@code true} if the value satisfies the constraint, {@code false} if
   *         the value does not satisfy the constraint.
   */
  boolean test(String value);

  /**
   * Describes this constraint for use in error messages.
   *
   * @return A string representing the human-readable constraint.
   */
  String describe();

  /**
   * Creates a constraint that requires a value to be non-blank.
   *
   * @return A {@link StringConstraint} representing the constraint.
   */
  static StringConstraint nonBlank() {
    return of(value -> !value.isBlank(), "non-blank");
  }

  /**
   * Creates a constraint that requires a value to match one of the allowed
   * options.
   *
   * @param allowed A {@link List} of strings representing the allowed values.
   * @return A {@link StringConstraint} representing the constraint.
   */
  static StringConstraint oneOf(List<String> allowed) {
    return of(value -> allowed.contains(value), "one of " + String.join(", ", allowed));
  }

  /**
   * Creates a constraint from a predicate and a description.
   *
   * @param predicate   A {@link Predicate} of string representing the boolean
   *                    test to apply.
   * @param description A string representing the human-readable constraint.
   * @return A {@link StringConstraint} representing the composed constraint.
   */
  private static StringConstraint of(Predicate<String> predicate, String description) {
    return new StringConstraint() {
      @Override
      public boolean test(String value) {
        return predicate.test(value);
      }

      @Override
      public String describe() {
        return description;
      }
    };
  }
}
