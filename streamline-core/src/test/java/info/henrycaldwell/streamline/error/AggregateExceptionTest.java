package info.henrycaldwell.streamline.error;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AggregateExceptionTest {

  @Nested
  class Constructor {

    @Test
    void acceptsSingleException() {
      TestException first = new TestException("first");

      assertDoesNotThrow(() -> new AggregateException(List.of(first)));
    }

    @Test
    void acceptsMultipleExceptions() {
      TestException first = new TestException("first");
      TestException second = new TestException("second");

      assertDoesNotThrow(() -> new AggregateException(List.of(first, second)));
    }

    @Test
    void throwsOnNullExceptions() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> new AggregateException(null));

      assertEquals("exceptions must be provided", exception.getMessage());
    }

    @Test
    void throwsOnEmptyExceptions() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> new AggregateException(List.of()));

      assertEquals("exceptions must not be empty", exception.getMessage());
    }
  }

  @Nested
  class GetExceptions {

    @Test
    void returnsConfiguredExceptions() {
      TestException first = new TestException("first");
      TestException second = new TestException("second");
      AggregateException aggregate = new AggregateException(List.of(first, second));

      assertEquals(List.of(first, second), aggregate.getExceptions());
    }
  }

  @Nested
  class GetMessage {

    @Test
    void joinsMessagesWithNewline() {
      TestException first = new TestException("first");
      TestException second = new TestException("second");
      AggregateException aggregate = new AggregateException(List.of(first, second));

      assertEquals("first\nsecond", aggregate.getMessage());
    }
  }

  @Nested
  class PrintStackTrace {

    @Test
    void printsEachExceptionToStream() {
      TestException first = new TestException("first");
      TestException second = new TestException("second");
      AggregateException aggregate = new AggregateException(List.of(first, second));

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      aggregate.printStackTrace(new PrintStream(buffer, true, StandardCharsets.UTF_8));

      String output = buffer.toString(StandardCharsets.UTF_8);

      assertTrue(output.contains("first"));
      assertTrue(output.contains("second"));
      assertTrue(output.indexOf("first") < output.indexOf("second"));
      assertTrue(output.contains(System.lineSeparator() + System.lineSeparator()));
    }

    @Test
    void printsEachExceptionToWriter() {
      TestException first = new TestException("first");
      TestException second = new TestException("second");
      AggregateException aggregate = new AggregateException(List.of(first, second));

      StringWriter buffer = new StringWriter();
      aggregate.printStackTrace(new PrintWriter(buffer));

      String output = buffer.toString();

      assertTrue(output.contains("first"));
      assertTrue(output.contains("second"));
      assertTrue(output.indexOf("first") < output.indexOf("second"));
      assertTrue(output.contains(System.lineSeparator() + System.lineSeparator()));
    }
  }

  private static final class TestException extends AbstractException {

    TestException(String message) {
      super(null, null, null, null, message);
    }
  }
}
