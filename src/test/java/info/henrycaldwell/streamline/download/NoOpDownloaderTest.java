package info.henrycaldwell.streamline.download;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.SpecException;

public class NoOpDownloaderTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = no_op
          """);

      assertDoesNotThrow(() -> new NoOpDownloader(config));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = no_op
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new NoOpDownloader(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Download {

    @Test
    void returnsMediaRefOnSuccess() {
      Path target = tempDir.resolve("clip.mp4");
      ClipRef clip = new ClipRef("clip-1", "https://example.com/clip-1", "Title", "Broadcaster", "en", 0, null);
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = no_op
          """);
      NoOpDownloader downloader = new NoOpDownloader(config);

      MediaRef result = downloader.download(clip, target, new CancellationToken());

      assertEquals(clip, result.clip());
      assertEquals(target, result.file());
    }
  }
}
