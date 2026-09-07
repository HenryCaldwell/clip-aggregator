package info.henrycaldwell.streamline.observe;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.ClipRef;

/**
 * Class for observing runs by performing no action.
 *
 * This class accepts events without recording any state to an underlying store.
 */
public final class NoOpObserver extends AbstractObserver {

  public static final Spec SPEC = Spec.builder().build();

  /**
   * Constructs a NoOpObserver.
   *
   * @param config A {@link Config} representing the observer configuration.
   */
  public NoOpObserver(Config config) {
    super(config);
  }

  /**
   * Records the start of a run without recording state.
   *
   * @param runner A string representing the runner name.
   * @param config A string representing the resolved configuration rendered as
   *               JSON, or {@code null}.
   * @return {@code 0} always, as no state is recorded to identify the run.
   */
  @Override
  public long runStart(String runner, String config) {
    return 0;
  }

  /**
   * Records the end of a run without recording state.
   *
   * @param runId     A long representing the run identifier.
   * @param status    A {@link RunStatus} representing the terminal run status.
   * @param published An integer representing the number of clips published.
   */
  @Override
  public void runEnd(long runId, RunStatus status, int published) {
  }

  /**
   * Records the start of a fetch without recording state.
   *
   * @param runId     A long representing the run identifier.
   * @param retriever A string representing the retriever name.
   * @param worker    A string representing the worker name.
   * @return {@code 0} always, as no state is recorded to identify the fetch.
   */
  @Override
  public long fetchStart(long runId, String retriever, String worker) {
    return 0;
  }

  /**
   * Records the end of a fetch without recording state.
   *
   * @param fetchId A long representing the fetch identifier.
   * @param status  An {@link AttemptStatus} representing the terminal fetch
   *                status.
   * @param clips   An integer representing the number of clips fetched.
   * @param error   A {@link Throwable} representing the failure cause, or
   *                {@code null}.
   */
  @Override
  public void fetchEnd(long fetchId, AttemptStatus status, int clips, Throwable error) {
  }

  /**
   * Records the start of an attempt without recording state.
   *
   * @param runId     A long representing the run identifier.
   * @param clip      A {@link ClipRef} representing the clip being processed.
   * @param stage     A {@link PipelineStage} representing the pipeline stage.
   * @param component A string representing the component name, or {@code null}.
   * @param worker    A string representing the worker name.
   * @return {@code 0} always, as no state is recorded to identify the attempt.
   */
  @Override
  public long attemptStart(long runId, ClipRef clip, PipelineStage stage, String component, String worker) {
    return 0;
  }

  /**
   * Records the end of an attempt without recording state.
   *
   * @param attemptId A long representing the attempt identifier.
   * @param status    An {@link AttemptStatus} representing the terminal attempt
   *                  status.
   * @param error     A {@link Throwable} representing the failure cause, or
   *                  {@code null}.
   */
  @Override
  public void attemptEnd(long attemptId, AttemptStatus status, Throwable error) {
  }

  /**
   * Records a successful publish without recording state.
   * 
   * @param runId     A long representing the run identifier.
   * @param clipId    A string representing the clip identifier.
   * @param publisher A string representing the publisher name.
   * @param uri       A string representing the published URI, or {@code null}.
   */
  @Override
  public void publish(long runId, String clipId, String publisher, String uri) {
  }
}
