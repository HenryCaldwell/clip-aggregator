package info.henrycaldwell.streamline.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AbstractExceptionTest {

  @Nested
  class GetMessage {

    @Test
    void formatsCategoryAndComponent() {
      TestException ex = new TestException("CAT", "comp", "msg");

      assertTrue(ex.getMessage().startsWith("[CAT:comp] msg"));
    }

    @Test
    void formatsWithNullCategory() {
      TestException ex = new TestException(null, "comp", "msg");

      assertTrue(ex.getMessage().startsWith("[comp] msg"));
    }

    @Test
    void formatsWithNullComponent() {
      TestException ex = new TestException("CAT", null, "msg");

      assertTrue(ex.getMessage().startsWith("[CAT] msg"));
    }

    @Test
    void omitsBracketsWhenCategoryAndComponentAreNull() {
      TestException ex = new TestException(null, null, "msg");

      assertEquals("msg", ex.getMessage());
    }

    @Test
    void includesDetailsWhenPresent() {
      TestException ex = new TestException("CAT", "comp", "msg", Map.of("key", "value"));

      assertTrue(ex.getMessage().contains("(key=value)"));
    }

    @Test
    void omitsDetailsWhenEmpty() {
      TestException ex = new TestException("CAT", "comp", "msg", Map.of());

      assertFalse(ex.getMessage().contains("("));
    }

    @Test
    void omitsDetailsWhenNull() {
      TestException ex = new TestException("CAT", "comp", "msg");

      assertFalse(ex.getMessage().contains("("));
    }
  }

  @Nested
  class GetCause {

    @Test
    void returnsCauseWhenProvided() {
      Throwable cause = new RuntimeException("cause");
      TestException ex = new TestException("CAT", "comp", "msg", null, cause);

      assertEquals(cause, ex.getCause());
    }

    @Test
    void returnsNullWhenNoCause() {
      TestException ex = new TestException("CAT", "comp", "msg");

      assertNull(ex.getCause());
    }
  }

  private static final class TestException extends AbstractException {

    TestException(String category, String component, String message) {
      super(category, component, message);
    }

    TestException(String category, String component, String message, Map<String, ?> details) {
      super(category, component, message, details);
    }

    TestException(String category, String component, String message, Map<String, ?> details, Throwable cause) {
      super(category, component, message, details, cause);
    }
  }
}
