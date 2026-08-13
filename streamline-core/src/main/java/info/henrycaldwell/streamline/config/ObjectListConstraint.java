package info.henrycaldwell.streamline.config;

import java.util.List;
import java.util.function.Predicate;

import com.typesafe.config.Config;

/**
 * Interface for enforcing a value constraint on an object list configuration
 * key.
 *
 * This interface defines a contract for testing whether a list of nested
 * configuration blocks satisfies a declared restriction.
 */
public interface ObjectListConstraint {

  /**
   * Tests whether the input value satisfies this constraint.
   *
   * @param value A {@link List} of {@link Config} representing the value to
   *              test.
   * @return {@code true} if the value satisfies the constraint, {@code false} if
   *         the value does not satisfy the constraint.
   */
  boolean test(List<? extends Config> value);

  /**
   * Describes this constraint for use in error messages.
   *
   * @return A string representing the human-readable constraint.
   */
  String describe();

  /**
   * Creates a constraint that requires the list to be non-empty.
   *
   * @return An {@link ObjectListConstraint} representing the constraint.
   */
  static ObjectListConstraint nonEmpty() {
    return of(value -> !value.isEmpty(), "non-empty");
  }

  /**
   * Creates a constraint from a predicate and a description.
   *
   * @param predicate   A {@link Predicate} of {@link List} of {@link Config}
   *                    representing the boolean test to apply.
   * @param description A string representing the human-readable constraint.
   * @return An {@link ObjectListConstraint} representing the composed
   *         constraint.
   */
  private static ObjectListConstraint of(Predicate<List<? extends Config>> predicate, String description) {
    return new ObjectListConstraint() {
      @Override
      public boolean test(List<? extends Config> value) {
        return predicate.test(value);
      }

      @Override
      public String describe() {
        return description;
      }
    };
  }
}
