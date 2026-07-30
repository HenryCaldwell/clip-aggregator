package info.henrycaldwell.streamline.history;

import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.ComponentType;

/**
 * Interface for tracking clips.
 *
 * This interface defines a contract for recording published clips to prevent
 * reposts.
 */
public interface History {

  ComponentType TYPE = ComponentType.HISTORY;

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
   * Checks whether a clip has been published.
   * 
   * @param clip   A {@link ClipRef} representing the clip to check.
   * @param runner A string representing the runner name.
   * @return {@code true} if the clip has been published, {@code false} if the
   *         clip has not been published.
   */
  boolean contains(ClipRef clip, String runner);

  /**
   * Records a clip as published.
   * 
   * @param clip   A {@link ClipRef} representing the published clip.
   * @param runner A string representing the runner name.
   */
  void add(ClipRef clip, String runner);
}
