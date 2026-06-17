package info.henrycaldwell.streamline.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.SpecException;

public class NoOpPublisherTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = no_op
          """);

      assertDoesNotThrow(() -> new NoOpPublisher(config));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = no_op
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new NoOpPublisher(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Publish {

    @Test
    void returnsPublishRefWithNullUri() {
      MediaRef media = new MediaRef(new ClipRef("clip-1", null, null, null, null, 0, null), null, null);
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = no_op
          """);
      NoOpPublisher publisher = new NoOpPublisher(config);

      PublishRef result = publisher.publish(media, new CancellationToken());

      assertNull(result.uri());
    }
  }
}
