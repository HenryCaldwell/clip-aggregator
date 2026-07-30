package info.henrycaldwell.streamline.transform;

import java.util.List;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.RunSession;
import info.henrycaldwell.streamline.error.ComponentType;
import info.henrycaldwell.streamline.observe.AttemptStatus;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.PipelineStage;

/**
 * Class for running media transformers in sequence.
 *
 * This class applies transformers to media in the configured order.
 */
public final class Pipeline {

  public static final ComponentType TYPE = ComponentType.PIPELINE;

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
   * @param session  A {@link RunSession} representing the state of the run.
   * @param worker   A string representing the worker name.
   * @return A {@link MediaRef} representing the transformed media.
   */
  public MediaRef run(MediaRef media, Observer observer, RunSession session, String worker) {
    ClipRef clip = media.clip();
    MediaRef curr = media;
    long runId = session.runId();
    CancellationToken token = session.token();

    for (Transformer transformer : transformers) {
      if (token.getReason() != null) {
        return curr;
      }

      long transformAttemptId = -1;
      if (observer != null) {
        transformAttemptId = observer.attemptStart(runId, clip, PipelineStage.TRANSFORM, transformer.getName(), worker);
      }

      try {
        curr = transformer.transform(curr, token);

        if (observer != null) {
          observer.attemptEnd(transformAttemptId, AttemptStatus.SUCCESS, null);
        }
      } catch (RuntimeException e) {
        if (observer != null) {
          AttemptStatus status = (token.getReason() != null) ? AttemptStatus.CANCELED : AttemptStatus.FAILURE;
          observer.attemptEnd(transformAttemptId, status, e);
        }

        throw e;
      }
    }

    return curr;
  }
}
