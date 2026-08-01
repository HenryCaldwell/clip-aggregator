package info.henrycaldwell.streamline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StringConstraintTest {

  @Nested
  class NonBlank {

    @Test
    void acceptsNonBlankValue() {
      StringConstraint constraint = StringConstraint.nonBlank();

      assertTrue(constraint.test("hello"));
    }

    @Test
    void rejectsEmptyValue() {
      StringConstraint constraint = StringConstraint.nonBlank();

      assertFalse(constraint.test(""));
    }

    @Test
    void rejectsWhitespaceValue() {
      StringConstraint constraint = StringConstraint.nonBlank();

      assertFalse(constraint.test("   "));
    }

    @Test
    void describesNonBlank() {
      StringConstraint constraint = StringConstraint.nonBlank();

      assertEquals("non-blank", constraint.describe());
    }
  }

  @Nested
  class OneOf {

    @Test
    void acceptsAllowedValue() {
      StringConstraint constraint = StringConstraint.oneOf(List.of("loop", "stop", "once"));

      assertTrue(constraint.test("loop"));
    }

    @Test
    void rejectsDisallowedValue() {
      StringConstraint constraint = StringConstraint.oneOf(List.of("loop", "stop", "once"));

      assertFalse(constraint.test("other"));
    }

    @Test
    void describesAllowedValues() {
      StringConstraint constraint = StringConstraint.oneOf(List.of("loop", "stop", "once"));

      assertEquals("one of loop, stop, once", constraint.describe());
    }
  }
}
