package info.henrycaldwell.aggregator.download;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;

/**
 * Base class for downloaders that parses common configuration.
 * 
 * This class validates downloader configuration using a shared base spec
 * combined with subclass-specific requirements.
 */
public abstract class AbstractDownloader implements Downloader {
  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs an abstract downloader.
   * 
   * @param config A {@link Config} representing the downloader block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractDownloader(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_DOWNLOADER";

    composite.validate(config, display);

    this.name = config.getString("name");
  }

  /**
   * Returns the configured downloader name.
   *
   * @return A string representing the downloader name.
   */
  @Override
  public String getName() {
    return name;
  }
}
