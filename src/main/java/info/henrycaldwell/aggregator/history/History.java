package info.henrycaldwell.aggregator.history;

import java.io.Closeable;

/**
 * Interface for tracking which clips have been published.
 *
 * This interface defines a contract for claiming clips to prevent reposts.
 */
public interface History extends Closeable {

  /**
   * Closes the history and releases any resources.
   */
  @Override
  void close();

  /**
   * Returns the configured history name.
   *
   * @return A string representing the history name.
   */
  String getName();

  /**
   * Attempts to claim a clip.
   * 
   * @param id        A string representing the clip identifier.
   * @param runner    A string representing the runner name.
   * @param publisher A string representing the publisher name.
   * @return {@code true} if the clip was successfully claimed, {@code false} if
   *         the clip was already claimed.
   */
  boolean claim(
      String id,
      String runner,
      String publisher
  );
}
