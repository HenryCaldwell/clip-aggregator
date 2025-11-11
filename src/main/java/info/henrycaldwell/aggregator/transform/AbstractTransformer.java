package info.henrycaldwell.aggregator.transform;

import java.nio.file.Files;
import java.nio.file.Path;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Base class for transformers that parses common configuration.
 * 
 * This class reads the required transformer base properties, and validates
 * using a composite Spec of the base keys and the subclass-specific keys.
 */
public abstract class AbstractTransformer implements Transformer {
  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs a base transformer.
   *
   * @param config A {@link Config} representing the transformer block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractTransformer(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_TRANSFORMER";

    composite.validate(config, display);

    this.name = config.getString("name");
  }

  /**
   * Returns the configured transformer name.
   *
   * @return A string representing the transformer name.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Applies this transformer and replaces the previous file.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   * @throws IllegalStateException if the transformer does not produce a new
   *                               output file or the output is missing.
   * @throws RuntimeException      if deleting the previous file fails.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path source = media.file();
    MediaRef result = transform(media);
    Path output = result.file();

    if (source == null || output == null || source.equals(output)) {
      throw new IllegalStateException("Transformer must produce a new output file + (" + name + ")");
    }

    if (!Files.isRegularFile(output)) {
      throw new IllegalStateException("Transformer produced a non-existent output (path: " + output + ")");
    }

    try {
      if (Files.isRegularFile(source)) {
        Files.delete(source);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to delete previous file (path: " + source + ")", e);
    }

    return result;
  }

  /**
   * Applies a subclass-specific transformation.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   */
  protected abstract MediaRef transform(MediaRef media);
}
