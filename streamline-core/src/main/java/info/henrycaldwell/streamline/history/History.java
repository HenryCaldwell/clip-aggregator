package info.henrycaldwell.streamline.history;

import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;

/**
 * Interface for tracking clips.
 *
 * This interface defines a contract for recording claimed clips to prevent
 * reposts.
 */
public interface History {

  /**
   * Initializes any underlying resources required by the history.
   */
  void start();

  /**
   * Releases any resources acquired by {@link #start()}.
   */
  void stop();

  /**
   * Returns the configured history name.
   *
   * @return A string representing the history name.
   */
  String getName();

  /**
   * Attempts to claim a clip.
   *
   * @param clip   A {@link ClipRef} representing the clip to claim.
   * @param runner A string representing the runner name.
   * @return {@code true} if the clip was successfully claimed, {@code false} if
   *         the clip was already published.
   */
  boolean claim(ClipRef clip, String runner);

  /**
   * Marks a clip as successfully prepared.
   *
   * @param media  A {@link MediaRef} representing the prepared media.
   * @param runner A string representing the runner name.
   */
  void prepare(MediaRef media, String runner);

  /**
   * Marks a clip as successfully published.
   *
   * @param ref       A {@link PublishRef} representing the published clip.
   * @param runner    A string representing the runner name.
   * @param publisher A string representing the publisher name.
   */
  void publish(PublishRef ref, String runner, String publisher);

  /**
   * Marks a clip as failed.
   *
   * @param clip   A {@link ClipRef} representing the failed clip.
   * @param runner A string representing the runner name.
   * @param error  A string representing the human-readable error message, or
   *               {@code null}.
   */
  void fail(ClipRef clip, String runner, String error);
}
