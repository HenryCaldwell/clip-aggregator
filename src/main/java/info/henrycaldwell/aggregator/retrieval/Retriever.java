package info.henrycaldwell.aggregator.retrieval;

import java.util.List;

import info.henrycaldwell.aggregator.core.ClipRef;

/**
 * Inteface for retrieving clips from a source.
 * 
 * This interface defines a contract for returning clip references for a game or
 * broadcaster.
 */
public interface Retriever {

  /**
   * Retrieves clips for a game or broadcaster.
   *
   * @return A list of {@link ClipRef} representing the retrieved clips.
   */
  List<ClipRef> fetch();
}
