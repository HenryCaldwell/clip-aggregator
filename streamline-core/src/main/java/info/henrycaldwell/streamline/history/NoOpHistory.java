package info.henrycaldwell.streamline.history;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;

/**
 * Class for tracking clips by performing no action.
 *
 * This class accepts operations without recording any state to an underlying
 * store.
 */
public final class NoOpHistory extends AbstractHistory {

  public static final Spec SPEC = Spec.builder().build();

  /**
   * Constructs a NoOpHistory.
   *
   * @param config A {@link Config} representing the history configuration.
   */
  public NoOpHistory(Config config) {
    super(config, SPEC);
  }

  /**
   * Claims a clip without recording state.
   *
   * @param clip   A {@link ClipRef} representing the clip to claim.
   * @param runner A string representing the runner name.
   * @return {@code true} always, as no state is recorded to detect duplicates.
   */
  @Override
  public boolean claim(ClipRef clip, String runner) {
    return true;
  }

  /**
   * Marks a clip as successfully prepared without recording state.
   *
   * @param media  A {@link MediaRef} representing the prepared media.
   * @param runner A string representing the runner name.
   */
  @Override
  public void prepare(MediaRef media, String runner) {
  }

  /**
   * Marks a clip as successfully published without recording state.
   *
   * @param ref       A {@link PublishRef} representing the published clip.
   * @param runner    A string representing the runner name.
   * @param publisher A string representing the publisher name.
   */
  @Override
  public void publish(PublishRef ref, String runner, String publisher) {
  }

  /**
   * Marks a clip as failed without recording state.
   *
   * @param clip   A {@link ClipRef} representing the failed clip.
   * @param runner A string representing the runner name.
   * @param error  A string representing the human-readable error message, or
   *               {@code null}.
   */
  @Override
  public void fail(ClipRef clip, String runner, String error) {
  }
}
