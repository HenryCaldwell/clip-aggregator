package info.henrycaldwell.aggregator.retrieval;

import java.time.Duration;
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
   * Retrieves recent clips for a game within a time window.
   * 
   * @param id       A string representing the game id.
   * @param window   A duration representing how far back to look (e.g., 24
   *                 hours).
   * @param limit    An integer representing the maximum number of clips to
   *                 return.
   * @param language A string representing the ISO language filter (e.g., "en"),
   *                 or {@code null} for any.
   * @return A list of {@link ClipRef} suitable for downloading.
   */
  List<ClipRef> fetchGame(String id, Duration window, int limit, String language);

  /**
   * Retrieves recent clips for a broadcaster within a time window.
   * 
   * @param id     A string representing the broadcaster id.
   * @param window A duration representing how far back to look (e.g., 24
   *               hours).
   * @param limit  An integer representing the maximum number of clips to
   *               return.
   * @return A list of {@link ClipRef} suitable for downloading.
   */
  List<ClipRef> fetchBroadcaster(String id, Duration window, int limit);
}
