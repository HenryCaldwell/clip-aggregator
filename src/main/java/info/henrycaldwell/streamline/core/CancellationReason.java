package info.henrycaldwell.streamline.core;

/**
 * Enumeration of cancellation reasons.
 *
 * Each value indicates how the cancellation occurred.
 */
public enum CancellationReason {

  POSTS_REACHED,
  PREPARATION_FAILURE_LIMIT,
  PUBLISHER_FAILURE_LIMIT,
  USER_CANCELED,

}
