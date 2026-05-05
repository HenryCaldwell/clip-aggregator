package info.henrycaldwell.aggregator.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;

public class VerticalBlurTransformerTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          """);

      assertDoesNotThrow(() -> new VerticalBlurTransformer(config));
    }

    @Test
    void acceptsConfiguredTargetWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          targetWidth = 720
          """);

      assertDoesNotThrow(() -> new VerticalBlurTransformer(config));
    }

    @Test
    void acceptsConfiguredTargetHeight() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          targetHeight = 1280
          """);

      assertDoesNotThrow(() -> new VerticalBlurTransformer(config));
    }

    @Test
    void acceptsConfiguredBlurSigma() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          blurSigma = 24.5
          """);

      assertDoesNotThrow(() -> new VerticalBlurTransformer(config));
    }

    @Test
    void acceptsConfiguredBlurSteps() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          blurSteps = 3
          """);

      assertDoesNotThrow(() -> new VerticalBlurTransformer(config));
    }

    @Test
    void throwsOnWrongTypeForTargetWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          targetWidth = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=targetWidth"));
    }

    @Test
    void throwsOnInvalidTargetWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          targetWidth = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=targetWidth"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForTargetHeight() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          targetHeight = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=targetHeight"));
    }

    @Test
    void throwsOnInvalidTargetHeight() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          targetHeight = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=targetHeight"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForBlurSigma() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          blurSigma = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=blurSigma"));
    }

    @Test
    void throwsOnInvalidBlurSigma() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          blurSigma = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=blurSigma"));
      assertTrue(exception.getMessage().contains("value=0.0"));
    }

    @Test
    void throwsOnWrongTypeForBlurSteps() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          blurSteps = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=blurSteps"));
    }

    @Test
    void throwsOnInvalidBlurSteps() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          blurSteps = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=blurSteps"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new VerticalBlurTransformer(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }
}
