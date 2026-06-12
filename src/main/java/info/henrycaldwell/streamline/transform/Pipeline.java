package info.henrycaldwell.streamline.transform;

import java.util.List;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.observe.AttemptStatus;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.PipelineStage;

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
   * @param observer An {@link Observer} representing the observer, or
   *                 {@code null}.
   * @param runId    A long representing the run identifier.
   * @param worker   A string representing the worker name.
   * @param token    A {@link CancellationToken} representing the cancellation
   *                 signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  public MediaRef run(MediaRef media, Observer observer, long runId, String worker, CancellationToken token) {
    ClipRef clip = media.clip();
    MediaRef curr = media;

    for (Transformer transformer : transformers) {
      if (token.getReason() != null) {
        return curr;
      }

      long transformAttemptId = -1;
      if (observer != null) {
        transformAttemptId = observer.attemptStart(runId, worker, clip, PipelineStage.TRANSFORM, transformer.getName());
      }

      try {
        curr = transformer.transform(curr);

        if (observer != null) {
          observer.attemptEnd(transformAttemptId, AttemptStatus.SUCCESS, null);
        }
      } catch (RuntimeException e) {
        if (observer != null) {
          observer.attemptEnd(transformAttemptId, AttemptStatus.FAILURE, e);
        }

        throw e;
      }
    }

    return curr;
  }
}
