package info.henrycaldwell.streamline.config;

import java.util.List;
import java.util.function.Predicate;

/**
 * Interface for enforcing a value constraint on a numeric list configuration
 * key.
 *
 * This interface defines a contract for testing whether a list of numbers
 * satisfies a declared restriction.
 */
public interface NumberListConstraint {

  /**
   * Tests whether the input value satisfies this constraint.
   *
   * @param value A {@link List} of {@link Number} representing the value to
   *              test.
   * @return {@code true} if the value satisfies the constraint, {@code false} if
   *         the value does not satisfy the constraint.
   */
  boolean test(List<Number> value);

  /**
   * Describes this constraint for use in error messages.
   *
   * @return A string representing the human-readable constraint.
   */
  String describe();

  /**
   * Creates a constraint that requires every element to satisfy the given
   * constraint.
   *
   * @param element A {@link NumberConstraint} representing the per-element
   *                constraint.
   * @return A {@link NumberListConstraint} representing the constraint.
   */
  static NumberListConstraint each(NumberConstraint element) {
    return of(value -> value.stream().allMatch(element::test), "each " + element.describe());
  }

  /**
   * Creates a constraint from a predicate and a description.
   *
   * @param predicate   A {@link Predicate} of {@link List} of {@link Number}
   *                    representing the boolean test to apply.
   * @param description A string representing the human-readable constraint.
   * @return A {@link NumberListConstraint} representing the composed constraint.
   */
  private static NumberListConstraint of(Predicate<List<Number>> predicate, String description) {
    return new NumberListConstraint() {
      @Override
      public boolean test(List<Number> value) {
        return predicate.test(value);
      }

      @Override
      public String describe() {
        return description;
      }
    };
  }
}
