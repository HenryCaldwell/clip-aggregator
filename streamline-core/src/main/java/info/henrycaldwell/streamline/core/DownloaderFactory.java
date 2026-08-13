package info.henrycaldwell.streamline.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.download.AbstractDownloader;
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

  private static final Map<String, Entry> REGISTRY = Map.of(
      "yt-dlp", new Entry(YtDlpDownloader.SPEC, YtDlpDownloader::new),
      "no_op", new Entry(NoOpDownloader.SPEC, NoOpDownloader::new));

  private DownloaderFactory() {
  }

  /**
   * Validates a downloader configuration block.
   *
   * @param config A {@link Config} representing the downloader configuration.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config) {
    String name = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_DOWNLOADER";
    String type = config.hasPath("type") && !config.getString("type").isBlank() ? config.getString("type") : null;

    Entry entry = type != null ? REGISTRY.get(type) : null;
    Spec composite = entry != null ? Spec.union(AbstractDownloader.BASE_SPEC, entry.spec())
        : AbstractDownloader.BASE_SPEC;

    List<SpecException> exceptions = composite.validate(config, Downloader.TYPE, null, name);

    if (type != null && entry == null) {
      exceptions.add(new SpecException(Downloader.TYPE, null, name, "Unknown downloader type",
          MapUtils.ofNullable("type", type)));
    }

    return exceptions;
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
    List<SpecException> exceptions = validate(config);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    return REGISTRY.get(config.getString("type")).factory().apply(config);
  }

  private record Entry(Spec spec, Function<Config, Downloader> factory) {
  }
}
