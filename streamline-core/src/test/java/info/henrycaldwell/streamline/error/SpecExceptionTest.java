package info.henrycaldwell.streamline.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SpecExceptionTest {

  @Nested
  class GetMessage {

    @Test
    void formatsWithSpecCategory() {
      SpecException ex = new SpecException("comp", "msg");

      assertTrue(ex.getMessage().startsWith("[SPEC:comp] msg"));
    }

    @Test
    void includesDetailsWhenPresent() {
      SpecException ex = new SpecException("comp", "msg", Map.of("key", "value"));

      assertTrue(ex.getMessage().contains("(key=value)"));
    }
  }

  @Nested
  class GetCause {

    @Test
    void returnsCauseWhenProvided() {
      Throwable cause = new RuntimeException("cause");
      SpecException ex = new SpecException("comp", "msg", null, cause);

      assertEquals(cause, ex.getCause());
    }

    @Test
    void returnsNullWhenNoCause() {
      SpecException ex = new SpecException("comp", "msg");

      assertNull(ex.getCause());
    }
  }
}
