package info.henrycaldwell.aggregator.core;

import java.util.List;

import info.henrycaldwell.aggregator.transform.Transformer;

/**
 * Class for executing an ordered set of media transformations.
 * 
 * This class applies each transformer in sequence to a given media file.
 */
public final class Pipeline {

  private List<Transformer> transformers;

  /**
   * Constructs a pipeline with an ordered list of transformers.
   *
   * @param transformers A list of {@link Transformer} representing the changes to
   *                     apply in order.
   */
  public Pipeline(List<Transformer> transformers) {
    this.transformers = transformers;
  }

  /**
   * Applies the configured transformers to the provided media.
   *
   * @param media A {@link MediaRef} representing the input media.
   * @return A {@link MediaRef} representing the transformed media.
   */
  public MediaRef run(MediaRef media) {
    MediaRef curr = media;

    for (Transformer transformer : transformers) {
      curr = transformer.apply(curr);
    }

    return curr;
  }
}
