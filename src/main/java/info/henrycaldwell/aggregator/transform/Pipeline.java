package info.henrycaldwell.aggregator.transform;

import java.util.List;

import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Class for executing an ordered set of media transformations.
 * 
 * This class applies each transformer in sequence to a given media file.
 */
public final class Pipeline {

  private final String name;
  private final List<Transformer> transformers;

  /**
   * Constructs a pipeline with an ordered list of transformers.
   *
   * @param name         A string representing the pipeline name.
   * @param transformers A list of {@link Transformer} representing the changes to
   *                     apply in order.
   */
  public Pipeline(String name, List<Transformer> transformers) {
    this.name = name;
    this.transformers = transformers;
  }

  /**
   * Returns the configured pipeline name.
   *
   * @return A string representing the pipeline name.
   */
  public String getName() {
    return name;
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
      curr = transformer.transform(curr);
    }

    return curr;
  }
}
