package info.henrycaldwell.streamline.config;

import java.util.function.Predicate;

/**
 * Interface for enforcing a value constraint on a numeric configuration key.
 *
 * This interface defines a contract for testing whether a number satisfies a
 * declared restriction.
 */
public interface NumberConstraint {

  /**
   * Tests whether the input value satisfies this constraint.
   *
   * @param value A {@link Number} representing the value to test.
   * @return {@code true} if the value satisfies the constraint, {@code false} if
   *         the value does not satisfy the constraint.
   */
  boolean test(Number value);

  /**
   * Describes this constraint for use in error messages.
   *
   * @return A string representing the human-readable constraint.
   */
  String describe();

  /**
   * Creates a constraint that requires a value to be greater than a minimum.
   *
   * @param min A double representing the exclusive minimum allowed value.
   * @return A {@link NumberConstraint} representing the constraint.
   */
  static NumberConstraint greaterThan(double min) {
    return of(value -> value.doubleValue() > min, "greater than " + min);
  }

  /**
   * Creates a constraint that requires a value to be greater than or equal to a
   * minimum.
   *
   * @param min A double representing the inclusive minimum allowed value.
   * @return A {@link NumberConstraint} representing the constraint.
   */
  static NumberConstraint atLeast(double min) {
    return of(value -> value.doubleValue() >= min, "at least " + min);
  }

  /**
   * Creates a constraint that requires a value to lie within an inclusive range.
   *
   * @param min A double representing the inclusive minimum allowed value.
   * @param max A double representing the inclusive maximum allowed value.
   * @return A {@link NumberConstraint} representing the constraint.
   */
  static NumberConstraint between(double min, double max) {
    return of(value -> value.doubleValue() >= min && value.doubleValue() <= max,
        "between " + min + " and " + max);
  }

  /**
   * Creates a constraint from a predicate and a description.
   *
   * @param predicate   A {@link Predicate} of {@link Number} representing the
   *                    boolean test to apply.
   * @param description A string representing the human-readable constraint.
   * @return A {@link NumberConstraint} representing the composed constraint.
   */
  private static NumberConstraint of(Predicate<Number> predicate, String description) {
    return new NumberConstraint() {
      @Override
      public boolean test(Number value) {
        return predicate.test(value);
      }

      @Override
      public String describe() {
        return description;
      }
    };
  }
}
