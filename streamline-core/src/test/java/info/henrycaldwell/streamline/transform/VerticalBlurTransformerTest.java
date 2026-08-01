package info.henrycaldwell.streamline.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.PathUtils;

public class VerticalBlurTransformerTest {

  @TempDir
  Path tempDir;

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
      assertTrue(exception.getMessage().contains("value=0"));
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

  @Nested
  class Apply {

    @Test
    void returnsMediaRefOnSuccess() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "data");
      Path target = PathUtils.deriveOut(source, "-temp.mp4");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = vertical_blur
          ffmpegPath = ffmpeg
          """);
      ProcessFactory factory = pb -> {
        Files.writeString(target, "output");
        return new ProcessBuilder(javaBinary(), "-version")
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start();
      };
      VerticalBlurTransformer transformer = new VerticalBlurTransformer(config, factory);

      MediaRef result = assertDoesNotThrow(() -> transformer.apply(media, new CancellationToken()));

      assertEquals(target, result.file());
    }
  }

  private static String javaBinary() {
    String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
    return Path.of(System.getProperty("java.home"), "bin", exe).toString();
  }
}
