package info.henrycaldwell.aggregator.history;

/**
 * Interface for tracking clips.
 *
 * This interface defines a contract for recording claimed clips to prevent
 * reposts.
 */
public interface History {

  /**
   * Initializes any underlying resources required by the history.
   */
  void start();

  /**
   * Releases any resources acquired by {@link #start()}.
   */
  void stop();

  /**
   * Returns the configured history name.
   *
   * @return A string representing the history name.
   */
  String getName();

  /**
   * Attempts to claim a clip.
   * 
   * @param id     A string representing the clip identifier.
   * @param runner A string representing the runner name.
   * @return {@code true} if the clip was successfully claimed, {@code false} if
   *         the clip was already claimed.
   */
  boolean claim(String id, String runner);
}
