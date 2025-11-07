package info.henrycaldwell.aggregator.core;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.download.Downloader;

/**
 * Class for constructing downloaders from HOCON configuration blocks.
 * 
 * This class validates a downloader block using its spec and instantiates
 * a concrete downloader.
 */
public final class DownloaderFactory {

  private static final Spec DOWNLOADER_BLOCK_SPEC = Spec.builder()
      .requiredString("type", "name")
      .build();

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
    DOWNLOADER_BLOCK_SPEC.validate(config, "BASE_DOWNLOADER");

    String type = config.getString("type");
    String name = config.getString("name");

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
