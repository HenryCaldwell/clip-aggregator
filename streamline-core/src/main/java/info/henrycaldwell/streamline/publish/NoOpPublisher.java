package info.henrycaldwell.streamline.publish;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;

/**
 * Class for publishing media by performing no action.
 * 
 * This class accept a media without publishing it to an external platform.
 */
public final class NoOpPublisher extends AbstractPublisher {

  public static final Spec SPEC = Spec.builder()
      .build();

  /**
   * Constructs a NoOpPublisher.
   *
   * @param config A {@link Config} representing the publisher configuration.
   */
  public NoOpPublisher(Config config) {
    super(config);
  }

  /**
   * Publishes the input media by performing no action.
   *
   * @param media A {@link MediaRef} representing the media to publish.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link PublishRef} representing the published media.
   */
  @Override
  public PublishRef publish(MediaRef media, CancellationToken token) {
    return new PublishRef(media.clip(), null);
  }
}
