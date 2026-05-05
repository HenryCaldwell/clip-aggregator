package info.henrycaldwell.aggregator.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;

public class FpsTransformerTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = fps
          ffmpegPath = ffmpeg
          """);

      assertDoesNotThrow(() -> new FpsTransformer(config));
    }

    @Test
    void acceptsConfiguredTargetFps() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = fps
          ffmpegPath = ffmpeg
          targetFps = 60
          """);

      assertDoesNotThrow(() -> new FpsTransformer(config));
    }

    @Test
    void throwsOnWrongTypeForTargetFps() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = fps
          ffmpegPath = ffmpeg
          targetFps = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new FpsTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=targetFps"));
    }

    @Test
    void throwsOnInvalidTargetFps() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = fps
          ffmpegPath = ffmpeg
          targetFps = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new FpsTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=targetFps"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = fps
          ffmpegPath = ffmpeg
          targetFps = 60
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new FpsTransformer(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }
}
