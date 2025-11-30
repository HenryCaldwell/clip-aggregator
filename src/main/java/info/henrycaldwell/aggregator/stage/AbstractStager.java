package info.henrycaldwell.aggregator.stage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;

/**
 * Base class for stagers that parses common configuration.
 * 
 * This class validates stager configuration using a shared base spec combined
 * with subclass-specific requirements.
 */
public abstract class AbstractStager implements Stager {

  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs an abstract stager.
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
   * @param media A {@link MediaRef} representing the media to stage.
   * @return A {@link MediaRef} representing the staged media.
   * @throws ComponentException if staging fails at any step.
   */
  @Override
  public MediaRef stage(MediaRef media) {
    Path source = media.file();
    MediaRef result = apply(media);
    URI output = result.uri();

    if (output == null
        || (!"http".equalsIgnoreCase(output.getScheme()) && !"https".equalsIgnoreCase(output.getScheme()))) {
      throw new ComponentException(name, "Stager did not produce an HTTP(S) URI", Map.of("uri", output));
    }

    try {
      if (source != null && Files.isRegularFile(source)) {
        Files.delete(source);
      }
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to delete previous file", Map.of("sourcePath", source), e);
    }

    return result;
  }

  /**
   * Cleans staged resources associated with the media.
   *
   * @param media A {@link MediaRef} representing the staged media.
   */
  @Override
  public void clean(MediaRef media) {
    // No-op by default
  }

  /**
   * Applies a subclass-specific staging.
   *
   * @param media A {@link MediaRef} representing the media to stage.
   * @return A {@link MediaRef} representing the staged media.
   */
  protected abstract MediaRef apply(MediaRef media);
}
