package info.henrycaldwell.aggregator.publish;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.core.PublishRef;

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
    super(config, SPEC);
  }

  /**
   * Publishes the input media by performing no external action.
   *
   * @param media A {@link MediaRef} representing the media to publish.
   * @return A {@link PublishRef} representing the published short.
   */
  @Override
  public PublishRef publish(MediaRef media) {
    return new PublishRef(media.id(), null);
  }
}
