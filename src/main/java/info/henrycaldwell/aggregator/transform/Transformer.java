package info.henrycaldwell.aggregator.transform;

import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Interface for transforming a media artifact.
 * 
 * This interface defines a contract for producing a new media reference from an
 * input media reference.
 */
public interface Transformer {

  /**
   * Applies a transformation to the input media.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   */
  MediaRef apply(MediaRef media);
}