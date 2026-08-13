package info.henrycaldwell.streamline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ObjectListConstraintTest {

  @Nested
  class NonEmpty {

    @Test
    void acceptsNonEmptyList() {
      ObjectListConstraint constraint = ObjectListConstraint.nonEmpty();
      List<? extends Config> values = List.of(ConfigFactory.parseString("a = 1"));

      assertTrue(constraint.test(values));
    }

    @Test
    void rejectsEmptyList() {
      ObjectListConstraint constraint = ObjectListConstraint.nonEmpty();

      assertFalse(constraint.test(List.of()));
    }

    @Test
    void describesNonEmpty() {
      ObjectListConstraint constraint = ObjectListConstraint.nonEmpty();

      assertEquals("non-empty", constraint.describe());
    }
  }
}
