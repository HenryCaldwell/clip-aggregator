package info.henrycaldwell.streamline.stage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.SpecException;

public class NoOpStagerTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = no_op
          """);

      assertDoesNotThrow(() -> new NoOpStager(config));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = no_op
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new NoOpStager(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Stage {

    @Test
    void returnsMediaUnchanged() {
      MediaRef media = new MediaRef(null, null, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = no_op
          """);
      NoOpStager stager = new NoOpStager(config);

      MediaRef result = stager.stage(media, new CancellationToken());

      assertEquals(media, result);
    }
  }

  @Nested
  class Apply {

    @Test
    void returnsMediaUnchanged() {
      MediaRef media = new MediaRef(null, null, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = no_op
          """);
      NoOpStager stager = new NoOpStager(config);

      MediaRef result = stager.apply(media, new CancellationToken());

      assertEquals(media, result);
    }
  }
}
