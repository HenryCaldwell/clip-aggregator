package info.henrycaldwell.streamline.config;

import java.util.List;
import java.util.function.Predicate;

/**
 * Interface for enforcing a value constraint on a boolean list configuration
 * key.
 *
 * This interface defines a contract for testing whether a list of booleans
 * satisfies a declared restriction.
 */
public interface BooleanListConstraint {

  /**
   * Tests whether the input value satisfies this constraint.
   *
   * @param value A {@link List} of {@link Boolean} representing the value to
   *              test.
   * @return {@code true} if the value satisfies the constraint, {@code false} if
   *         the value does not satisfy the constraint.
   */
  boolean test(List<Boolean> value);

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
   * @param element A {@link BooleanConstraint} representing the per-element
   *                constraint.
   * @return A {@link BooleanListConstraint} representing the constraint.
   */
  static BooleanListConstraint each(BooleanConstraint element) {
    return of(value -> value.stream().allMatch(element::test), "each " + element.describe());
  }

  /**
   * Creates a constraint from a predicate and a description.
   *
   * @param predicate   A {@link Predicate} of {@link List} of {@link Boolean}
   *                    representing the boolean test to apply.
   * @param description A string representing the human-readable constraint.
   * @return A {@link BooleanListConstraint} representing the composed constraint.
   */
  private static BooleanListConstraint of(Predicate<List<Boolean>> predicate, String description) {
    return new BooleanListConstraint() {
      @Override
      public boolean test(List<Boolean> value) {
        return predicate.test(value);
      }

      @Override
      public String describe() {
        return description;
      }
    };
  }
}
