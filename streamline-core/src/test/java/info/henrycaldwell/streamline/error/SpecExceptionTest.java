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
      SpecException ex = new SpecException(null, null, "name", "msg");

      assertTrue(ex.getMessage().startsWith("[SPEC:name] msg"));
    }

    @Test
    void includesDetailsWhenPresent() {
      SpecException ex = new SpecException(null, null, "name", "msg", Map.of("key", "value"));

      assertTrue(ex.getMessage().contains("(key=value)"));
    }
  }

  @Nested
  class GetCause {

    @Test
    void returnsConfiguredCause() {
      Throwable cause = new RuntimeException("cause");
      SpecException ex = new SpecException(null, null, "name", "msg", null, cause);

      assertEquals(cause, ex.getCause());
    }

    @Test
    void returnsNullWhenCauseIsMissing() {
      SpecException ex = new SpecException(null, null, "name", "msg");

      assertNull(ex.getCause());
    }
  }
}
