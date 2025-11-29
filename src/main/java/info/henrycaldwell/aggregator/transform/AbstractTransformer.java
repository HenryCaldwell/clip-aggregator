package info.henrycaldwell.aggregator.transform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;

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
   * Transforms the input media and replaces the previous file.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   * @throws IllegalStateException if the transformer does not produce a new
   *                               output file or the output is missing.
   * @throws RuntimeException      if deleting the previous file fails.
   */
  @Override
  public MediaRef transform(MediaRef media) {
    Path source = media.file();
    MediaRef result = apply(media);
    Path output = result.file();

    if (source == null || output == null || source.equals(output)) {
      throw new ComponentException(name, "Transformer did not produce a new output file",
          Map.of("sourcePath", source, "outputPath", output));
    }

    if (!Files.isRegularFile(output)) {
      throw new ComponentException(name, "Transformer produced a non-regular output file",
          Map.of("outputPath", output));
    }

    try {
      if (Files.isRegularFile(source)) {
        Files.delete(source);
      }
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to delete previous file", Map.of("sourcePath", source), e);
    }

    return result;
  }

  /**
   * Applies a subclass-specific transformation.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   */
  protected abstract MediaRef apply(MediaRef media);
}
