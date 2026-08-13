package info.henrycaldwell.streamline.transform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Base class for transformers that parses common configuration.
 * 
 * This class validates transformer configuration using a shared base spec
 * combined with subclass-specific requirements.
 */
public abstract class AbstractTransformer implements Transformer {
  public static final Spec BASE_SPEC = Spec.builder()
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

    String name = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_TRANSFORMER";

    List<SpecException> exceptions = composite.validate(config, Transformer.TYPE, null, name);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

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
   * Transforms the input media and replaces the original file.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   * @throws ComponentException if transforming fails at any step.
   */
  @Override
  public MediaRef transform(MediaRef media, CancellationToken token) {
    Path source = media.file();
    MediaRef result = apply(media, token);
    Path output = result.file();

    if (source == null || output == null || source.equals(output)) {
      throw new ComponentException(Transformer.TYPE, null, name, "Transformer did not produce a new output file",
          MapUtils.ofNullable("sourcePath", source, "outputPath", output));
    }

    if (!Files.isRegularFile(output)) {
      throw new ComponentException(Transformer.TYPE, null, name, "Transformer produced a non-regular output file",
          MapUtils.ofNullable("outputPath", output));
    }

    try {
      try {
        Files.move(output, source, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
        Files.move(output, source, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new ComponentException(Transformer.TYPE, null, name, "Failed to replace original file",
          MapUtils.ofNullable("sourcePath", source, "outputPath", output), e);
    }

    return result.withFile(source);
  }

  /**
   * Applies a subclass-specific transformation.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  protected abstract MediaRef apply(MediaRef media, CancellationToken token);
}
