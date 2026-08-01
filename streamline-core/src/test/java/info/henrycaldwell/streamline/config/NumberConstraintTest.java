package info.henrycaldwell.streamline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class NumberConstraintTest {

  @Nested
  class GreaterThan {

    @Test
    void acceptsAboveMinimum() {
      NumberConstraint constraint = NumberConstraint.greaterThan(0);

      assertTrue(constraint.test(1));
    }

    @Test
    void rejectsAtMinimum() {
      NumberConstraint constraint = NumberConstraint.greaterThan(0);

      assertFalse(constraint.test(0));
    }

    @Test
    void rejectsBelowMinimum() {
      NumberConstraint constraint = NumberConstraint.greaterThan(0);

      assertFalse(constraint.test(-1));
    }

    @Test
    void describesMinimum() {
      NumberConstraint constraint = NumberConstraint.greaterThan(0);

      assertEquals("greater than 0.0", constraint.describe());
    }
  }

  @Nested
  class AtLeast {

    @Test
    void acceptsAboveMinimum() {
      NumberConstraint constraint = NumberConstraint.atLeast(0);

      assertTrue(constraint.test(1));
    }

    @Test
    void acceptsAtMinimum() {
      NumberConstraint constraint = NumberConstraint.atLeast(0);

      assertTrue(constraint.test(0));
    }

    @Test
    void rejectsBelowMinimum() {
      NumberConstraint constraint = NumberConstraint.atLeast(0);

      assertFalse(constraint.test(-1));
    }

    @Test
    void describesMinimum() {
      NumberConstraint constraint = NumberConstraint.atLeast(0);

      assertEquals("at least 0.0", constraint.describe());
    }
  }

  @Nested
  class Between {

    @Test
    void acceptsInRange() {
      NumberConstraint constraint = NumberConstraint.between(0, 1);

      assertTrue(constraint.test(0.5));
    }

    @Test
    void acceptsAtMinimum() {
      NumberConstraint constraint = NumberConstraint.between(0, 1);

      assertTrue(constraint.test(0));
    }

    @Test
    void acceptsAtMaximum() {
      NumberConstraint constraint = NumberConstraint.between(0, 1);

      assertTrue(constraint.test(1));
    }

    @Test
    void rejectsBelowRange() {
      NumberConstraint constraint = NumberConstraint.between(0, 1);

      assertFalse(constraint.test(-0.5));
    }

    @Test
    void rejectsAboveRange() {
      NumberConstraint constraint = NumberConstraint.between(0, 1);

      assertFalse(constraint.test(1.5));
    }

    @Test
    void describesRange() {
      NumberConstraint constraint = NumberConstraint.between(0, 1);

      assertEquals("between 0.0 and 1.0", constraint.describe());
    }
  }
}
