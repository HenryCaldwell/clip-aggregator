package info.henrycaldwell.aggregator.transform;

import java.util.List;
import java.util.function.BooleanSupplier;

import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Class for running media transformers in sequence.
 * 
 * This class applies transformers to media in the configured order.
 */
public final class Pipeline {

  private final String name;
  private final List<Transformer> transformers;

  /**
   * Constructs a pipeline.
   *
   * @param name         A string representing the pipeline name.
   * @param transformers A {@link List} of {@link Transformer} representing the
   *                     changes to apply in order.
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
   * Applies the configured transformers to the input media.
   *
   * @param media    A {@link MediaRef} representing the media to transform.
   * @param canceled A {@link BooleanSupplier} representing the cancelation
   *                 signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  public MediaRef run(MediaRef media, BooleanSupplier canceled) {
    MediaRef curr = media;

    for (Transformer transformer : transformers) {
      if (canceled.getAsBoolean()) {
        System.out.println("EARLY EXIT");
        return curr;
      }

      curr = transformer.transform(curr);
    }

    return curr;
  }
}
