package info.henrycaldwell.aggregator.publish;

import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.core.PublishRef;

/**
 * Interface for publishing media.
 *
 * This interface defines a contract for publishing media to external platforms.
 */
public interface Publisher {

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
   * @return A {@link PublishRef} representing the published media.
   */
  PublishRef publish(MediaRef media);
}
