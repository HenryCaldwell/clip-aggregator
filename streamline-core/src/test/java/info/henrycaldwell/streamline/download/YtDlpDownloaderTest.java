package info.henrycaldwell.streamline.download;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import info.henrycaldwell.streamline.core.CancellationReason;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;

public class YtDlpDownloaderTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);

      assertDoesNotThrow(() -> new YtDlpDownloader(config));
    }

    @Test
    void acceptsConfiguredTimeout() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          timeout = 30
          """);

      assertDoesNotThrow(() -> new YtDlpDownloader(config));
    }

    @Test
    void throwsOnMissingYtDlpPath() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new YtDlpDownloader(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=ytDlpPath"));
    }

    @Test
    void throwsOnWrongTypeForYtDlpPath() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = [yt-dlp]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new YtDlpDownloader(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=ytDlpPath"));
    }

    @Test
    void throwsOnWrongTypeForTimeout() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          timeout = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new YtDlpDownloader(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=timeout"));
    }

    @Test
    void throwsOnInvalidTimeout() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          timeout = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new YtDlpDownloader(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=timeout"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new YtDlpDownloader(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Download {

    @Test
    void returnsMediaRefOnSuccess() {
      Path target = tempDir.resolve("output.mp4");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);
      ProcessFactory factory = (c, t) -> javaCommand("-cp", System.getProperty("java.class.path"),
          WriteFileProcess.class.getName(), t.toString());
      YtDlpDownloader downloader = new YtDlpDownloader(config, factory);

      MediaRef result = assertDoesNotThrow(() -> downloader.download(clip, target, new CancellationToken()));

      assertEquals(clip, result.clip());
      assertEquals(target, result.file());
    }

    @Test
    void throwsWhenTargetAlreadyExists() throws IOException {
      Path target = tempDir.resolve("output.mp4");
      Files.writeString(target, "existing");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);
      YtDlpDownloader downloader = new YtDlpDownloader(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> downloader.download(clip, target, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Target file already exists"));
      assertTrue(exception.getMessage().contains("targetPath=" + target));
    }

    @Test
    void throwsWhenProcessFailsToStart() {
      Path target = tempDir.resolve("output.mp4");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = /this/does/not/exist
          """);
      YtDlpDownloader downloader = new YtDlpDownloader(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> downloader.download(clip, target, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to start yt-dlp process"));
    }

    @Test
    void throwsWhenProcessTimesOut() {
      Path target = tempDir.resolve("output.mp4");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          timeout = 1
          """);
      ProcessFactory factory = (c, t) -> javaCommand("-cp", System.getProperty("java.class.path"),
          SleepProcess.class.getName());
      YtDlpDownloader downloader = new YtDlpDownloader(config, factory);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> downloader.download(clip, target, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Timed out while waiting for yt-dlp process"));
      assertTrue(exception.getMessage().contains("timeout=1"));
    }

    @Test
    void throwsWhenProcessExitsWithNonZeroCode() {
      Path target = tempDir.resolve("output.mp4");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);
      ProcessFactory factory = (c, t) -> javaCommand("--definitely-not-a-real-java-option");
      YtDlpDownloader downloader = new YtDlpDownloader(config, factory);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> downloader.download(clip, target, new CancellationToken()));

      assertTrue(exception.getMessage().contains("yt-dlp process exited with non-zero code"));
      assertTrue(exception.getMessage().contains("exitCode="));
    }

    @Test
    void throwsWhenCanceled() {
      Path target = tempDir.resolve("output.mp4");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);
      ProcessFactory factory = (c, t) -> javaCommand("-cp", System.getProperty("java.class.path"),
          SleepProcess.class.getName());
      YtDlpDownloader downloader = new YtDlpDownloader(config, factory);
      CancellationToken token = new CancellationToken();
      Thread canceler = new Thread(() -> {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        token.cancel(CancellationReason.USER_CANCELED);
      });
      canceler.start();

      ComponentException exception = assertThrows(ComponentException.class,
          () -> downloader.download(clip, target, token));

      assertTrue(exception.getMessage().contains("yt-dlp process exited with non-zero code"));
    }

    @Test
    void throwsWhenOutputIsMissing() {
      Path target = tempDir.resolve("output.mp4");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);
      ProcessFactory factory = (c, t) -> javaCommand("-version");
      YtDlpDownloader downloader = new YtDlpDownloader(config, factory);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> downloader.download(clip, target, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Output file missing after download"));
      assertTrue(exception.getMessage().contains("targetPath=" + target));
    }

    @Test
    void throwsWhenOutputIsEmpty() {
      Path target = tempDir.resolve("output.mp4");

      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip", "Title", "Broadcaster", "en", 100, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);
      ProcessFactory factory = (c, t) -> javaCommand("-cp", System.getProperty("java.class.path"),
          WriteEmptyFileProcess.class.getName(), t.toString());
      YtDlpDownloader downloader = new YtDlpDownloader(config, factory);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> downloader.download(clip, target, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Output file empty after download"));
      assertTrue(exception.getMessage().contains("sizeBytes=0"));
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
}
