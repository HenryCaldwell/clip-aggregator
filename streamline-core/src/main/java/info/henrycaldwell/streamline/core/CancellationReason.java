package info.henrycaldwell.streamline.core;

import info.henrycaldwell.streamline.observe.RunStatus;

/**
 * Enumeration of cancellation reasons.
 *
 * Each value indicates how the cancellation occurred.
 */
public enum CancellationReason {

  POSTS_REACHED(RunStatus.SUCCESS),
  PREPARATION_FAILURE_LIMIT(RunStatus.FAILURE),
  PUBLISHER_FAILURE_LIMIT(RunStatus.FAILURE),
  USER_CANCELED(RunStatus.CANCELED);

  private final RunStatus status;

  /**
   * Constructs a CancellationReason.
   *
   * @param status A {@link RunStatus} representing the terminal run status.
   */
  private CancellationReason(RunStatus status) {
    this.status = status;
  }

  /**
   * Returns the terminal run status.
   *
   * @return A {@link RunStatus} representing the terminal run status.
   */
  public RunStatus status() {
    return status;
  }
}
