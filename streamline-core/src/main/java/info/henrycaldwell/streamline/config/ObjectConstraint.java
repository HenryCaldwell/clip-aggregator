package info.henrycaldwell.streamline.config;

import com.typesafe.config.Config;

/**
 * Interface for enforcing a value constraint on an object configuration key.
 *
 * This interface defines a contract for testing whether a nested configuration
 * block satisfies a declared restriction.
 */
public interface ObjectConstraint {

  /**
   * Tests whether the input value satisfies this constraint.
   *
   * @param value A {@link Config} representing the value to test.
   * @return {@code true} if the value satisfies the constraint, {@code false} if
   *         the value does not satisfy the constraint.
   */
  boolean test(Config value);

  /**
   * Describes this constraint for use in error messages.
   *
   * @return A string representing the human-readable constraint.
   */
  String describe();
}
