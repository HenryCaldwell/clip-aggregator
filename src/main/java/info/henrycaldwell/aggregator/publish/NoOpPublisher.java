package info.henrycaldwell.aggregator.publish;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.core.PublishRef;

/**
 * Class for a publisher that performs no external action.
 * 
 * This class consumes a media reference without publishing it.
 */
public class NoOpPublisher extends AbstractPublisher {

  public static final Spec SPEC = Spec.builder()
      .build();

  /**
   * Constructs a NoOpPublisher.
   *
   * @param config A {@link Config} representing the publisher block.
   */
  public NoOpPublisher(Config config) {
    super(config, SPEC);
  }

  /**
   * Publishes a media artifact by performing no operation.
   *
   * @param media A {@link MediaRef} representing the artifact to publish.
   * @return A {@link PublishRef} representing the published location.
   */
  @Override
  public PublishRef publish(MediaRef media) {
    return new PublishRef(media.id(), null);
  }
}
