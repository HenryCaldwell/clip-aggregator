package info.henrycaldwell.streamline.retrieve;

import java.util.List;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;

/**
 * Class for retrieving clips by performing no action.
 *
 * This class accepts configuration without retrieving clips from an external
 * source.
 */
public final class NoOpRetriever extends AbstractRetriever {

  public static final Spec SPEC = Spec.builder().build();

  /**
   * Constructs a NoOpRetriever.
   *
   * @param config A {@link Config} representing the retriever configuration.
   */
  public NoOpRetriever(Config config) {
    super(config, SPEC);
  }

  /**
   * Retrieves clips by performing no action.
   *
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link List} of {@link ClipRef} representing the retrieved clips.
   */
  @Override
  public List<ClipRef> fetch(CancellationToken token) {
    return List.of();
  }
}
