package info.henrycaldwell.streamline.transform;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;

/**
 * Class for transforming media by performing no action.
 *
 * This class accepts media without applying any transformation.
 */
public final class NoOpTransformer extends AbstractTransformer {

  public static final Spec SPEC = Spec.builder().build();

  /**
   * Constructs a NoOpTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   */
  public NoOpTransformer(Config config) {
    super(config);
  }

  /**
   * Transforms the input media by performing no action.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  @Override
  public MediaRef transform(MediaRef media, CancellationToken token) {
    return media;
  }

  /**
   * Transforms the input media by performing no action.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  @Override
  protected MediaRef apply(MediaRef media, CancellationToken token) {
    return media;
  }
}
