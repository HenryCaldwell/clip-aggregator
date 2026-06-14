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

public class FpsTransformerTest {

  @TempDir
  Path tempDir;

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
          type = fps
          ffmpegPath = ffmpeg
          """);
      ProcessFactory factory = pb -> {
        Files.writeString(target, "output");
        return new ProcessBuilder(javaBinary(), "-version")
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start();
      };
      FpsTransformer transformer = new FpsTransformer(config, factory);

      MediaRef result = assertDoesNotThrow(() -> transformer.apply(media, new CancellationToken()));

      assertEquals(target, result.file());
    }
  }

  private static String javaBinary() {
    String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
    return Path.of(System.getProperty("java.home"), "bin", exe).toString();
  }
}
