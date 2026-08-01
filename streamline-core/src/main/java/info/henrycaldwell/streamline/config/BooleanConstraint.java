package info.henrycaldwell.streamline.config;

/**
 * Interface for enforcing a value constraint on a boolean configuration key.
 *
 * This interface defines a contract for testing whether a boolean satisfies a
 * declared restriction.
 */
public interface BooleanConstraint {

  /**
   * Tests whether the input value satisfies this constraint.
   *
   * @param value A {@link Boolean} representing the value to test.
   * @return {@code true} if the value satisfies the constraint, {@code false} if
   *         the value does not satisfy the constraint.
   */
  boolean test(Boolean value);

  /**
   * Describes this constraint for use in error messages.
   *
   * @return A string representing the human-readable constraint.
   */
  String describe();
}
