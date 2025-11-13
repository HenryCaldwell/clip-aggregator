package info.henrycaldwell.aggregator.stage;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Base class for stagers that parses common configuration.
 * 
 * This class reads the required stager base properties, and validates
 * using a composite Spec of the base keys and the subclass-specific keys.
 */
public abstract class AbstractStager implements Stager {

  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs a base stager.
   *
   * @param config A {@link Config} representing the stager block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractStager(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_STAGER";

    composite.validate(config, display);

    this.name = config.getString("name");
  }

  /**
   * Initializes any underlying resources required by the stager.
   */
  @Override
  public void start() {
    // No-op by default
  }

  /**
   * Releases any resources acquired by {@link #start()}.
   */
  @Override
  public void stop() {
    // No-op by default
  }

  /**
   * Returns the configured stager name.
   *
   * @return A string representing the stager name.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Stages the input media and replaces the previous file.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the staged artifact.
   * @throws IllegalStateException if the stager does not produce a new remote URI
   *                               or the URI is not HTTP or HTTPS.
   * @throws RuntimeException      if deleting the previous file fails.
   */
  @Override
  public MediaRef stage(MediaRef media) {
    Path source = media.file();
    MediaRef result = apply(media);
    URI output = result.uri();

    if (output == null) {
      throw new IllegalStateException("Stager must produce a new output URI + (" + name + ")");
    }

    if (!"http".equalsIgnoreCase(output.getScheme()) && !"https".equalsIgnoreCase(output.getScheme())) {
      throw new IllegalStateException("Stager must produce a remote URI + (URI: " + output + ")");
    }

    try {
      if (source != null && Files.isRegularFile(source)) {
        Files.delete(source);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to delete previous file (path: " + source + ")", e);
    }

    return result;
  }

  /**
   * Applies a subclass-specific staging.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the staged artifact.
   */
  protected abstract MediaRef apply(MediaRef media);
}
