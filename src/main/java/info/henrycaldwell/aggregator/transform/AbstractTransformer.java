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
 * This class validates transformer configuration using a shared base spec
 * combined with subclass-specific requirements.
 */
public abstract class AbstractTransformer implements Transformer {
  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs an abstract transformer.
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
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   * @throws ComponentException if transforming fails at any step.
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
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   */
  protected abstract MediaRef apply(MediaRef media);
}
