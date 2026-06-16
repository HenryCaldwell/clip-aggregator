package info.henrycaldwell.streamline.observe;

import info.henrycaldwell.streamline.core.ClipRef;

/**
 * Interface for observing runs.
 *
 * This interface defines a contract for recording run-level and attempt-level
 * events.
 */
public interface Observer {

  /**
   * Initializes any underlying resources required by the observer.
   */
  void start();

  /**
   * Releases any resources acquired by {@link #start()}.
   */
  void stop();

  /**
   * Returns the configured observer name.
   *
   * @return A string representing the observer name.
   */
  String getName();

  /**
   * Records the start of a run.
   *
   * @param runner A string representing the runner name.
   * @param config A string representing the resolved configuration rendered as
   *               JSON, or {@code null}.
   * @return A long representing the run identifier.
   */
  long runStart(String runner, String config);

  /**
   * Records the end of a run.
   *
   * @param runId     A long representing the run identifier.
   * @param status    A {@link RunStatus} representing the terminal run status.
   * @param published An integer representing the number of clips published.
   */
  void runEnd(long runId, RunStatus status, int published);

  /**
   * Records the start of a fetch for a single retriever.
   *
   * @param runId     A long representing the run identifier.
   * @param retriever A string representing the retriever name.
   * @return A long representing the fetch identifier.
   */
  long fetchStart(long runId, String retriever);

  /**
   * Records the end of a fetch for a single retriever.
   *
   * @param fetchId   A long representing the fetch identifier.
   * @param status    An {@link AttemptStatus} representing the terminal fetch
   *                  status.
   * @param clipCount An integer representing the number of clips fetched.
   * @param error     A {@link Throwable} representing the failure cause, or
   *                  {@code null}.
   */
  void fetchEnd(long fetchId, AttemptStatus status, int clipCount, Throwable error);

  /**
   * Records the start of an attempt at a single pipeline stage for a single clip.
   *
   * @param runId     A long representing the run identifier.
   * @param worker    A string representing the worker name.
   * @param clip      A {@link ClipRef} representing the clip being processed.
   * @param stage     A {@link PipelineStage} representing the pipeline stage.
   * @param component A string representing the component name, or {@code null}.
   * @return A long representing the attempt identifier.
   */
  long attemptStart(long runId, String worker, ClipRef clip, PipelineStage stage, String component);

  /**
   * Records the end of an attempt at a single pipeline stage for a single clip.
   *
   * @param attemptId A long representing the attempt identifier.
   * @param status    An {@link AttemptStatus} representing the terminal attempt
   *                  status.
   * @param error     A {@link Throwable} representing the failure cause, or
   *                  {@code null}.
   */
  void attemptEnd(long attemptId, AttemptStatus status, Throwable error);

  /**
   * Records a heartbeat for a live run.
   * 
   * @param runId A long representing the run identifier.
   */
  void heartbeat(long runId);
}
