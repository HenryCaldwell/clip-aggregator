package info.henrycaldwell.streamline.stage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Base class for stagers that parses common configuration.
 * 
 * This class validates stager configuration using a shared base spec combined
 * with subclass-specific requirements.
 */
public abstract class AbstractStager implements Stager {

  public static final Spec BASE_SPEC = Spec.builder()
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

    String name = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_STAGER";

    List<SpecException> exceptions = composite.validate(config, Stager.TYPE, null, name);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

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
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the staged media.
   * @throws ComponentException if staging fails at any step.
   */
  @Override
  public MediaRef stage(MediaRef media, CancellationToken token) {
    Path source = media.file();
    MediaRef result = apply(media, token);
    URI output = result.uri();

    if (output == null
        || (!"http".equalsIgnoreCase(output.getScheme()) && !"https".equalsIgnoreCase(output.getScheme()))) {
      throw new ComponentException(Stager.TYPE, null, name, "Stager did not produce an HTTP(S) URI",
          MapUtils.ofNullable("uri", output));
    }

    try {
      if (source != null && Files.isRegularFile(source)) {
        Files.delete(source);
      }
    } catch (IOException e) {
      throw new ComponentException(Stager.TYPE, null, name, "Failed to delete previous file",
          MapUtils.ofNullable("sourcePath", source), e);
    }

    return result;
  }

  /**
   * Cleans staged resources associated with the media.
   *
   * @param media A {@link MediaRef} representing the staged media.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   */
  @Override
  public void clean(MediaRef media, CancellationToken token) {
    // No-op by default
  }

  /**
   * Purges all staged resources.
   * 
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   */
  @Override
  public void purge(CancellationToken token) {
    // No-op by default
  }

  /**
   * Applies a subclass-specific staging.
   *
   * @param media A {@link MediaRef} representing the media to stage.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the staged media.
   */
  protected abstract MediaRef apply(MediaRef media, CancellationToken token);
}
