package info.henrycaldwell.streamline.download;

import java.util.List;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.error.SpecException;

/**
 * Base class for downloaders that parses common configuration.
 * 
 * This class validates downloader configuration using a shared base spec
 * combined with subclass-specific requirements.
 */
public abstract class AbstractDownloader implements Downloader {
  public static final Spec SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs an abstract downloader.
   * 
   * @param config A {@link Config} representing the downloader block.
   */
  protected AbstractDownloader(Config config) {
    Spec composite = Spec.collect(getClass());

    String name = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_DOWNLOADER";

    List<SpecException> exceptions = composite.validate(config, Downloader.TYPE, null, name);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

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
