package info.henrycaldwell.streamline.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.SpecException;

public class AbstractHistoryTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = test
          """);

      assertDoesNotThrow(() -> new TestHistory(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestHistory(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = history
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestHistory(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = test
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestHistory(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Start {

    @Test
    void doesNothingByDefault() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = test
          """);
      TestHistory history = new TestHistory(config);

      assertDoesNotThrow(history::start);
    }
  }

  @Nested
  class Stop {

    @Test
    void doesNothingByDefault() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = test
          """);
      TestHistory history = new TestHistory(config);

      assertDoesNotThrow(history::stop);
    }
  }

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = test
          """);
      TestHistory history = new TestHistory(config);

      String result = history.getName();

      assertEquals("history", result);
    }
  }

  private static final class TestHistory extends AbstractHistory {

    private TestHistory(Config config) {
      super(config);
    }

    @Override
    public boolean contains(ClipRef clip, String runner) {
      return false;
    }

    @Override
    public void add(ClipRef clip, String runner) {
    }
  }
}
