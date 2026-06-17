package info.henrycaldwell.streamline.core;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.download.Downloader;
import info.henrycaldwell.streamline.download.NoOpDownloader;
import info.henrycaldwell.streamline.download.YtDlpDownloader;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing downloaders from configuration.
 * 
 * This class validates a downloader configuration block and instantiates a
 * concrete downloader implementation.
 */
public final class DownloaderFactory {

  private DownloaderFactory() {
  }

  /**
   * Builds a downloader from the given configuration block.
   *
   * @param config A {@link Config} representing the downloader configuration.
   * @return A {@link Downloader} representing the configured downloader.
   * @throws SpecException if the configuration is invalid or the downloader type
   *                       is unknown.
   */
  public static Downloader fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_DOWNLOADER", "Missing required key", MapUtils.ofNullable("key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(name, "Missing required key", MapUtils.ofNullable("key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "yt-dlp" -> {
        return new YtDlpDownloader(config);
      }
      case "no_op" -> {
        return new NoOpDownloader(config);
      }
      default -> throw new SpecException(name, "Unknown downloader type", MapUtils.ofNullable("type", type));
    }
  }
}
