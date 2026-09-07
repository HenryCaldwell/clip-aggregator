package info.henrycaldwell.streamline.download;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.SpecException;

public class AbstractDownloaderTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = test
          """);

      assertDoesNotThrow(() -> new TestDownloader(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestDownloader(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestDownloader(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = test
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestDownloader(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = test
          """);
      TestDownloader downloader = new TestDownloader(config);

      String result = downloader.getName();

      assertEquals("downloader", result);
    }
  }

  private static final class TestDownloader extends AbstractDownloader {

    private TestDownloader(Config config) {
      super(config);
    }

    @Override
    public MediaRef download(ClipRef clip, Path target, CancellationToken token) {
      return null;
    }
  }
}
