package info.henrycaldwell.streamline.stage;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentType;

/**
 * Interface for staging media.
 *
 * This interface defines a contract for producing remote media from local
 * media.
 */
public interface Stager {

  ComponentType TYPE = ComponentType.STAGER;

  /**
   * Initializes any underlying resources required by the stager.
   */
  void start();

  /**
   * Releases any resources acquired by {@link #start()}.
   */
  void stop();

  /**
   * Returns the configured stager name.
   *
   * @return A string representing the stager name.
   */
  String getName();

  /**
   * Stages the input media to a remote location.
   *
   * @param media A {@link MediaRef} representing the media to stage.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the staged media.
   */
  MediaRef stage(MediaRef media, CancellationToken token);

  /**
   * Cleans staged resources associated with the media.
   *
   * @param media A {@link MediaRef} representing the staged media.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   */
  void clean(MediaRef media, CancellationToken token);

  /**
   * Purges all staged resources.
   * 
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   */
  void purge(CancellationToken token);
}
