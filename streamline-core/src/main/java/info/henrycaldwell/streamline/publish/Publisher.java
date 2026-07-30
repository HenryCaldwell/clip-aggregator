package info.henrycaldwell.streamline.publish;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.ComponentType;

/**
 * Interface for publishing media.
 *
 * This interface defines a contract for publishing media to external platforms.
 */
public interface Publisher {

  ComponentType TYPE = ComponentType.PUBLISHER;

  /**
   * Returns the configured publisher name.
   *
   * @return A string representing the publisher name.
   */
  String getName();

  /**
   * Publishes the input media.
   *
   * @param media A {@link MediaRef} representing the media to publish.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link PublishRef} representing the published media.
   */
  PublishRef publish(MediaRef media, CancellationToken token);
}
