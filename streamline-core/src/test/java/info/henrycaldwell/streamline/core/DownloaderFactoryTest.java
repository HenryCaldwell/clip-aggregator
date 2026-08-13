package info.henrycaldwell.streamline.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.error.SpecException;

public class DownloaderFactoryTest {

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = no_op
          """);

      assertTrue(DownloaderFactory.validate(config).isEmpty());
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = DownloaderFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          """);

      SpecException exception = DownloaderFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = unknown
          """);

      SpecException exception = DownloaderFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Unknown downloader type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

    @Test
    void accumulatesBaseAndConcreteErrors() {
      Config config = ConfigFactory.parseString("""
          type = "yt-dlp"
          """);

      List<SpecException> exceptions = DownloaderFactory.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=name")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=ytDlpPath")));
    }
  }

  @Nested
  class FromConfig {

    @Test
    void returnsDownloader() {
      Config config = ConfigFactory.parseString("""
          name = downloader
          type = no_op
          """);

      assertDoesNotThrow(() -> DownloaderFactory.fromConfig(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_downloader
          """);

      SpecException exception = assertThrows(SpecException.class, () -> DownloaderFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_downloader
          type = ""
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
