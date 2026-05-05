package info.henrycaldwell.aggregator.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;
import info.henrycaldwell.aggregator.error.SpecException;

public class FFmpegTransformerTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void throwsOnMissingFFmpegPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestFFmpegTransformer(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=ffmpegPath"));
    }

    @Test
    void usesDefaultTimeout() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);

      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      assertEquals(180L, transformer.timeout());
    }

    @Test
    void usesConfiguredTimeout() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          timeout = 30
          """);

      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      assertEquals(30L, transformer.timeout());
    }

    @Test
    void throwsOnInvalidTimeout() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          timeout = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestFFmpegTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=timeout"));
      assertTrue(exception.getMessage().contains("value=0"));
    }
  }

  @Nested
  class Preflight {

    @Test
    void throwsWhenSourceIsNull() {
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef("clip-1", null, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callPreflight(media, null, target));

      assertTrue(exception.getMessage().contains("Input file missing or not a regular file"));
      assertTrue(exception.getMessage().contains("clipId=clip-1"));
      assertTrue(exception.getMessage().contains("sourcePath=null"));
    }

    @Test
    void throwsWhenSourceIsMissing() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callPreflight(media, source, target));

      assertTrue(exception.getMessage().contains("Input file missing or not a regular file"));
      assertTrue(exception.getMessage().contains("sourcePath=" + source));
    }

    @Test
    void throwsWhenTargetIsNull() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callPreflight(media, source, null));

      assertTrue(exception.getMessage().contains("Target path is null"));
      assertTrue(exception.getMessage().contains("targetPath=null"));
    }

    @Test
    void createsTargetParentDirectories() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("nested").resolve("target.mp4");

      Files.writeString(source, "source");
      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      transformer.callPreflight(media, source, target);

      assertTrue(Files.isDirectory(target.getParent()));
      assertFalse(Files.exists(target));
    }

    @Test
    void throwsWhenTargetAlreadyExists() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");
      Files.writeString(source, "source");
      Files.writeString(target, "target");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callPreflight(media, source, target));

      assertTrue(exception.getMessage().contains("Target file already exists"));
      assertTrue(exception.getMessage().contains("targetPath=" + target));
    }
  }

  @Nested
  class RunProcess {

    @Test
    void succeedsWhenProcessExitsWithZero() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      assertDoesNotThrow(() -> transformer.callRunProcess(javaCommand("-version"), media, source, target));
    }

    @Test
    void throwsWhenProcessTimesOut() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          timeout = 1
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);
      ProcessBuilder processBuilder = javaCommand(
          "-cp",
          System.getProperty("java.class.path"),
          SleepProcess.class.getName());

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callRunProcess(processBuilder, media, source, target));

      assertTrue(exception.getMessage().contains("Timed out while waiting for ffmpeg process"));
      assertTrue(exception.getMessage().contains("timeout=1"));
    }

    @Test
    void throwsWhenProcessExitsWithNonZeroCode() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callRunProcess(javaCommand("--definitely-not-a-real-java-option"), media, source, target));

      assertTrue(exception.getMessage().contains("ffmpeg process exited with non-zero code"));
      assertTrue(exception.getMessage().contains("exitCode="));
    }
  }

  @Nested
  class Postflight {

    @Test
    void throwsWhenOutputIsMissing() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callPostflight(media, source, target));

      assertTrue(exception.getMessage().contains("Output file missing after transform"));
      assertTrue(exception.getMessage().contains("targetPath=" + target));
    }

    @Test
    void throwsWhenOutputIsEmpty() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");
      Files.writeString(target, "");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callPostflight(media, source, target));

      assertTrue(exception.getMessage().contains("Output file empty after transform"));
      assertTrue(exception.getMessage().contains("sizeBytes=0"));
    }

    @Test
    void succeedsWhenOutputIsNonEmpty() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");
      Files.writeString(target, "output");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      assertDoesNotThrow(() -> transformer.callPostflight(media, source, target));
    }
  }

  private static ProcessBuilder javaCommand(String... args) {
    String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
    String javaBinary = Path.of(System.getProperty("java.home"), "bin", executable).toString();

    List<String> command = new ArrayList<>();
    command.add(javaBinary);
    command.addAll(List.of(args));

    return new ProcessBuilder(command);
  }

  private static final class TestFFmpegTransformer extends FFmpegTransformer {

    private TestFFmpegTransformer(Config config) {
      super(config, Spec.builder().build());
    }

    private long timeout() {
      return timeout;
    }

    private void callPreflight(MediaRef media, Path source, Path target) {
      super.preflight(media, source, target);
    }

    private void callRunProcess(ProcessBuilder processBuilder, MediaRef media, Path source, Path target) {
      super.runProcess(processBuilder, media, source, target);
    }

    private void callPostflight(MediaRef media, Path source, Path target) {
      super.postflight(media, source, target);
    }

    @Override
    protected MediaRef apply(MediaRef media) {
      return media;
    }
  }
}
