package info.henrycaldwell.streamline.retrieve;

import java.util.List;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;

/**
 * Interface for retrieving clips.
 * 
 * This interface defines a contract for producing clips from external sources.
 */
public interface Retriever {

  /**
   * Returns the configured retriever name.
   *
   * @return A string representing the retriever name.
   */
  String getName();

  /**
   * Returns the configured pipeline name.
   *
   * @return A string representing the pipeline name, or {@code null}.
   */
  String getPipeline();

  /**
   * Retrieves clips from the configured source.
   *
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link List} of {@link ClipRef} representing the retrieved clips.
   */
  List<ClipRef> fetch(CancellationToken token);
}
