package info.henrycaldwell.streamline.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.SpecException;

public class AbstractPublisherTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = test
          """);

      assertDoesNotThrow(() -> new TestPublisher(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestPublisher(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestPublisher(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = test
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestPublisher(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = test
          """);
      TestPublisher publisher = new TestPublisher(config);

      String result = publisher.getName();

      assertEquals("publisher", result);
    }
  }

  private static final class TestPublisher extends AbstractPublisher {

    private TestPublisher(Config config) {
      super(config, Spec.builder().build());
    }

    @Override
    public PublishRef publish(MediaRef media, CancellationToken token) {
      return null;
    }
  }
}
