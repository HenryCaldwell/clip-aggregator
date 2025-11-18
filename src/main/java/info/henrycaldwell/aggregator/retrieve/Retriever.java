package info.henrycaldwell.aggregator.retrieve;

import java.util.List;

import info.henrycaldwell.aggregator.core.ClipRef;

/**
 * Interface for retrieving clips from a source.
 * 
 * This interface defines a contract for returning clip references for a game or
 * broadcaster.
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
   * Retrieves clips for a game or broadcaster.
   *
   * @return A list of {@link ClipRef} representing the retrieved clips.
   */
  List<ClipRef> fetch();
}
