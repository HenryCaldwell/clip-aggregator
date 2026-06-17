package info.henrycaldwell.streamline.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ComponentExceptionTest {

  @Nested
  class GetMessage {

    @Test
    void formatsWithComponentCategory() {
      ComponentException ex = new ComponentException("comp", "msg");

      assertTrue(ex.getMessage().startsWith("[COMPONENT:comp] msg"));
    }

    @Test
    void includesDetailsWhenPresent() {
      ComponentException ex = new ComponentException("comp", "msg", Map.of("key", "value"));

      assertTrue(ex.getMessage().contains("(key=value)"));
    }
  }

  @Nested
  class GetCause {

    @Test
    void returnsCauseWhenProvided() {
      Throwable cause = new RuntimeException("cause");
      ComponentException ex = new ComponentException("comp", "msg", null, cause);

      assertEquals(cause, ex.getCause());
    }

    @Test
    void returnsNullWhenNoCause() {
      ComponentException ex = new ComponentException("comp", "msg");

      assertNull(ex.getCause());
    }
  }
}
