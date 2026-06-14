package info.henrycaldwell.streamline.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;

public class FFmpegTransformerTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, null, null, null, 0, null);

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);

      assertDoesNotThrow(() -> new TestFFmpegTransformer(config));
    }

    @Test
    void acceptsConfiguredTimeout() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          timeout = 30
          """);

      assertDoesNotThrow(() -> new TestFFmpegTransformer(config));
    }

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
    void throwsOnWrongTypeForFFmpegPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = [ffmpeg]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestFFmpegTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=ffmpegPath"));
    }

    @Test
    void throwsOnWrongTypeForTimeout() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          timeout = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestFFmpegTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=timeout"));
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

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestFFmpegTransformer(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Preflight {

    @Test
    void throwsWhenSourceIsNull() {
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef(CLIP, null, null);
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

      MediaRef media = new MediaRef(CLIP, source, null);
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
    void throwsWhenSourceIsNotARegularFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");
      Files.createDirectory(source);

      MediaRef media = new MediaRef(CLIP, source, null);
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

      MediaRef media = new MediaRef(CLIP, source, null);
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
      MediaRef media = new MediaRef(CLIP, source, null);
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

      MediaRef media = new MediaRef(CLIP, source, null);
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

      MediaRef media = new MediaRef(CLIP, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      assertDoesNotThrow(
          () -> transformer.callRunProcess(javaCommand("-version"), media, source, target, new CancellationToken()));
    }

    @Test
    void throwsWhenProcessFailsToStart() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef(CLIP, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callRunProcess(new ProcessBuilder("/this/does/not/exist"), media, source, target,
              new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to start ffmpeg process"));
      assertTrue(exception.getMessage().contains("clipId=clip-1"));
    }

    @Test
    void throwsWhenProcessTimesOut() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef(CLIP, source, null);
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
          () -> transformer.callRunProcess(processBuilder, media, source, target, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Timed out while waiting for ffmpeg process"));
      assertTrue(exception.getMessage().contains("timeout=1"));
    }

    @Test
    void throwsWhenProcessExitsWithNonZeroCode() {
      Path source = tempDir.resolve("source.mp4");
      Path target = tempDir.resolve("target.mp4");

      MediaRef media = new MediaRef(CLIP, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          ffmpegPath = ffmpeg
          """);
      TestFFmpegTransformer transformer = new TestFFmpegTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.callRunProcess(javaCommand("--definitely-not-a-real-java-option"), media, source, target,
              new CancellationToken()));

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

      MediaRef media = new MediaRef(CLIP, source, null);
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

      MediaRef media = new MediaRef(CLIP, source, null);
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

      MediaRef media = new MediaRef(CLIP, source, null);
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

    private void callPreflight(MediaRef media, Path source, Path target) {
      super.preflight(media, source, target);
    }

    private void callRunProcess(ProcessBuilder processBuilder, MediaRef media, Path source, Path target,
        CancellationToken token) {
      super.runProcess(processBuilder, media, source, target, token);
    }

    private void callPostflight(MediaRef media, Path source, Path target) {
      super.postflight(media, source, target);
    }

    @Override
    protected MediaRef apply(MediaRef media, CancellationToken token) {
      return media;
    }
  }
}
