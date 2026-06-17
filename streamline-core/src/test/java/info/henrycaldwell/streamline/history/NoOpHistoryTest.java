package info.henrycaldwell.streamline.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.SpecException;

public class NoOpHistoryTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, null, null, null, 0, null);

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = no_op
          """);

      assertDoesNotThrow(() -> new NoOpHistory(config));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = no_op
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new NoOpHistory(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Claim {

    @Test
    void returnsTrue() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = no_op
          """);
      NoOpHistory history = new NoOpHistory(config);

      assertTrue(history.claim(CLIP, "runner"));
    }
  }

  @Nested
  class Prepare {

    @Test
    void doesNothing() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = no_op
          """);
      NoOpHistory history = new NoOpHistory(config);

      assertDoesNotThrow(() -> history.prepare(new MediaRef(CLIP, null, null), "runner"));
    }
  }

  @Nested
  class Publish {

    @Test
    void doesNothing() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = no_op
          """);
      NoOpHistory history = new NoOpHistory(config);

      assertDoesNotThrow(() -> history.publish(new PublishRef(CLIP, null), "runner", "publisher"));
    }
  }

  @Nested
  class Fail {

    @Test
    void doesNothing() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = no_op
          """);
      NoOpHistory history = new NoOpHistory(config);

      assertDoesNotThrow(() -> history.fail(CLIP, "runner", "error message"));
    }
  }
}
