package info.henrycaldwell.streamline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class NumberListConstraintTest {

  @Nested
  class Each {

    @Test
    void acceptsAllElementsPass() {
      NumberConstraint element = new TestNumberConstraint(value -> true, "test");
      NumberListConstraint constraint = NumberListConstraint.each(element);
      List<Number> values = List.of(1, 2, 3);

      assertTrue(constraint.test(values));
    }

    @Test
    void rejectsAnyElementFails() {
      NumberConstraint element = new TestNumberConstraint(value -> value.intValue() != 0, "test");
      NumberListConstraint constraint = NumberListConstraint.each(element);
      List<Number> values = List.of(1, 0, 3);

      assertFalse(constraint.test(values));
    }

    @Test
    void acceptsEmptyList() {
      NumberConstraint element = new TestNumberConstraint(value -> false, "test");
      NumberListConstraint constraint = NumberListConstraint.each(element);

      assertTrue(constraint.test(List.of()));
    }

    @Test
    void describesElementConstraint() {
      NumberConstraint element = new TestNumberConstraint(value -> true, "my description");
      NumberListConstraint constraint = NumberListConstraint.each(element);

      assertEquals("each my description", constraint.describe());
    }
  }

  private static final class TestNumberConstraint implements NumberConstraint {

    private final Predicate<Number> predicate;
    private final String description;

    private TestNumberConstraint(Predicate<Number> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(Number value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }
}
