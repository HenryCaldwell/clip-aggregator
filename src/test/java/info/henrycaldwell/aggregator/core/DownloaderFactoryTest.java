package info.henrycaldwell.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;

public class DownloaderFactoryTest {

  @Nested
  class FromConfig {

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = yt-dlp
          ytDlpPath = yt-dlp
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = yt_dlp_downloader
          ytDlpPath = yt-dlp
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = yt_dlp_downloader
          type = ""
          ytDlpPath = yt-dlp
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_downloader
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Unknown downloader type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

  }
}
