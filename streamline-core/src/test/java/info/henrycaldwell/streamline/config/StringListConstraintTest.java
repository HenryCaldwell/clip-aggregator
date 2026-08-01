package info.henrycaldwell.streamline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StringListConstraintTest {

  @Nested
  class Each {

    @Test
    void acceptsAllElementsPass() {
      StringConstraint element = new TestStringConstraint(value -> true, "test");
      StringListConstraint constraint = StringListConstraint.each(element);

      assertTrue(constraint.test(List.of("a", "b", "c")));
    }

    @Test
    void rejectsAnyElementFails() {
      StringConstraint element = new TestStringConstraint(value -> !value.isEmpty(), "test");
      StringListConstraint constraint = StringListConstraint.each(element);

      assertFalse(constraint.test(List.of("a", "", "c")));
    }

    @Test
    void acceptsEmptyList() {
      StringConstraint element = new TestStringConstraint(value -> false, "test");
      StringListConstraint constraint = StringListConstraint.each(element);

      assertTrue(constraint.test(List.of()));
    }

    @Test
    void describesElementConstraint() {
      StringConstraint element = new TestStringConstraint(value -> true, "my description");
      StringListConstraint constraint = StringListConstraint.each(element);

      assertEquals("each my description", constraint.describe());
    }
  }

  private static final class TestStringConstraint implements StringConstraint {

    private final Predicate<String> predicate;
    private final String description;

    private TestStringConstraint(Predicate<String> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(String value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }
}
