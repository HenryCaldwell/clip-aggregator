package info.henrycaldwell.aggregator.publish;

import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.core.PublishRef;

/**
 * Interface for publishing a media artifact.
 *
 * This interface defines a contract for emitting a media reference to some
 * destination.
 */
public interface Publisher {

  /**
   * Publishes a single media artifact.
   *
   * @param media A {@link MediaRef} representing the artifact to publish.
   * @return A {@link PublishRef} representing the published location.
   */
  PublishRef publish(MediaRef media);
}
