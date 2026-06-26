package info.henrycaldwell.streamline.history;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.ClipRef;

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
   * Checks whether a clip has been published without consulting state.
   *
   * @param clip   A {@link ClipRef} representing the clip to check.
   * @param runner A string representing the runner name.
   * @return {@code false} always, as no state is recorded to detect duplicates.
   */
  @Override
  public boolean contains(ClipRef clip, String runner) {
    return false;
  }

  /**
   * Records a clip as published without recording state.
   *
   * @param clip   A {@link ClipRef} representing the published clip.
   * @param runner A string representing the runner name.
   */
  @Override
  public void add(ClipRef clip, String runner) {
  }
}
