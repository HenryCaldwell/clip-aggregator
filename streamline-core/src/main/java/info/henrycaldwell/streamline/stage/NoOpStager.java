package info.henrycaldwell.streamline.stage;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;

/**
 * Class for staging media by performing no action.
 *
 * This class accepts media without uploading it to an external store.
 */
public final class NoOpStager extends AbstractStager {

  public static final Spec SPEC = Spec.builder().build();

  /**
   * Constructs a NoOpStager.
   *
   * @param config A {@link Config} representing the stager configuration.
   */
  public NoOpStager(Config config) {
    super(config);
  }

  /**
   * Stages the input media by performing no action.
   *
   * @param media A {@link MediaRef} representing the media to stage.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the staged media.
   */
  @Override
  public MediaRef stage(MediaRef media, CancellationToken token) {
    return media;
  }

  /**
   * Stages the input media by performing no action.
   *
   * @param media A {@link MediaRef} representing the media to stage.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the staged media.
   */
  @Override
  protected MediaRef apply(MediaRef media, CancellationToken token) {
    return media;
  }
}
