package info.henrycaldwell.streamline.retrieve;

import java.net.http.HttpRequest;

import info.henrycaldwell.streamline.core.CancellationToken;

/**
 * Functional interface for sending an HTTP request.
 *
 * This interface defines a contract for dispatching HTTP requests used by
 * retrievers, allowing real HTTP calls to be substituted in tests.
 */
@FunctionalInterface
interface HttpSender {

  /**
   * Sends an HTTP request and returns the response body.
   *
   * @param request A {@link HttpRequest} representing the request to send.
   * @param token   A {@link CancellationToken} representing the cancellation
   *                signal.
   * @return A string representing the response body.
   */
  String send(HttpRequest request, CancellationToken token);
}
