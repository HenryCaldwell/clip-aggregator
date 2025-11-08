package info.henrycaldwell.aggregator.core;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.download.Downloader;

/**
 * Class for constructing downloaders from HOCON configuration blocks.
 * 
 * This class validates a downloader block using its spec and instantiates
 * a concrete downloader.
 */
public final class DownloaderFactory {

  private DownloaderFactory() {
  }

  /**
   * Builds a downloader from a HOCON configuration block.
   *
   * @param config A {@link Config} representing a single downloader block.
   * @return A {@link Downloader} representing the downloader.
   * @throws IllegalArgumentException if the type is unknown.
   */
  public static Downloader fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new IllegalArgumentException("Missing required key name (UNNAMED_DOWNLOADER)");
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new IllegalArgumentException("Missing required key type (" + name + ")");
    }

    String type = config.getString("type");

    switch (type) {
      case "EXAMPLE 1" -> {
        // FUTURE IMPLEMENTATION VALIDATE EACH
        return null;
      }
      case "EXAMPLE 2" -> {
        // FUTURE IMPLEMENTATION VALIDATE EACH
        return null;
      }
      default -> throw new IllegalArgumentException("Unknown downloader type " + type + " (" + name + ")");
    }
  }
}
