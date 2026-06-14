package info.henrycaldwell.streamline.transform;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;

/**
 * Interface for transforming media.
 * 
 * This interface defines a contract for producing new media from input media.
 */
public interface Transformer {

  /**
   * Returns the configured transformer name.
   *
   * @return A string representing the transformer name.
   */
  String getName();

  /**
   * Transforms the input media.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  MediaRef transform(MediaRef media, CancellationToken token);
}
