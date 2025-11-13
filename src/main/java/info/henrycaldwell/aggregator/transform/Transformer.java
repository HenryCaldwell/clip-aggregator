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
   * Returns the configured transformer name.
   *
   * @return A string representing the transformer name.
   */
  String getName();

  /**
   * Transforms the input media.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   */
  MediaRef transform(MediaRef media);
}
