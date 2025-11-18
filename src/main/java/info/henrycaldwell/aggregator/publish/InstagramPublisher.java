package info.henrycaldwell.aggregator.publish;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.core.PublishRef;

/**
 * Class for publishing media artifacts to Instagram Reels.
 * 
 * This class publishes the input media reference to Instagram Reels
 * using the Instagram Graph API.
 */
public final class InstagramPublisher extends AbstractPublisher {

  public static final Spec SPEC = Spec.builder()
      .requiredString("accountId", "accessKey")
      .build();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpClient http;
  private final String accountId;
  private final String accessKey;

  /**
   * Constructs an InstagramPublisher.
   *
   * @param config A {@link Config} representing the publisher block.
   */
  public InstagramPublisher(Config config) {
    super(config, SPEC);

    this.http = HttpClient.newHttpClient();
    this.accountId = config.getString("accountId");
    this.accessKey = config.getString("accessKey");
  }

  /**
   * Publishes a media artifact as an Instagram reel.
   *
   * @param media A {@link MediaRef} representing the artifact to publish.
   * @return A {@link PublishRef} representing the published location.
   * @throws IllegalArgumentException if the media URI is missing or not HTTP or
   *                                  HTTPS.
   * @throws RuntimeException         if the Instagram Graph API calls fail.
   */
  @Override
  public PublishRef publish(MediaRef media) {
    URI uri = media.uri();

    if (uri == null || uri.getScheme() == null
        || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
      throw new IllegalArgumentException("Media URI must be HTTP or HTTPS (uri: " + uri + ")");
    }

    String url = uri.toString();
    String caption = media.title();

    String containerId = createContainer(url, caption);
    awaitContainer(containerId);
    String mediaId = publishContainer(containerId);

    return new PublishRef(mediaId, null);
  }

  /**
   * Creates a reels media container.
   *
   * @param url     A string representing the public video URL.
   * @param caption A string representing the caption to associate with the reel,
   *                or {@code null}.
   * @return A string representing the container identifier.
   * @throws RuntimeException if the Instagram Graph API call fails or the
   *                          response is invalid.
   */
  private String createContainer(String url, String caption) {
    ObjectNode root = MAPPER.createObjectNode();

    root.put("video_url", url);
    root.put("media_type", "REELS");
    if (caption != null && !caption.isBlank()) {
      root.put("caption", caption);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://graph.instagram.com/v23.0/" + accountId + "/media"))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + accessKey)
        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
        .build();

    String json = send(request);

    String id;
    try {
      id = MAPPER.readTree(json).at("/id").asText(null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse id", e);
    }

    if (id == null || id.isBlank()) {
      throw new RuntimeException("Instagram media creation did not return an id (response: " + json + ")");
    }

    return id;
  }

  /**
   * Waits for the reels media container to become ready for publishing.
   *
   * @param containerId A string representing the container identifier.
   * @throws RuntimeException if the container does not become ready within the
   *                          timeout or enters an error state.
   */
  private void awaitContainer(String containerId) {
    long timeout = TimeUnit.MINUTES.toNanos(10);
    long start = System.nanoTime();

    while (System.nanoTime() - start < timeout) {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("https://graph.instagram.com/v23.0/" + containerId + "?fields=status_code"))
          .header("Authorization", "Bearer " + accessKey)
          .GET()
          .build();

      String json = send(request);

      String status;
      try {
        status = MAPPER.readTree(json).at("/status_code").asText(null);
      } catch (Exception e) {
        throw new RuntimeException("Failed to parse status_code", e);
      }

      if (status == null || status.isBlank()) {
        throw new RuntimeException("Instagram container status response did not include status_code (id: " + containerId
            + ", response: " + json + ")");
      }

      if ("FINISHED".equalsIgnoreCase(status)) {
        return;
      }

      if ("ERROR".equalsIgnoreCase(status)) {
        throw new RuntimeException(
            "Reels container entered error state (id: " + containerId + ", response: " + json + ")");
      }

      try {
        Thread.sleep(2000L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for reels container (id: " + containerId + ")", e);
      }
    }

    throw new RuntimeException("Timed out waiting for reels container (id: " + containerId + ")");
  }

  /**
   * Publishes a reels media container.
   *
   * @param containerId A string representing the container identifier.
   * @return A string representing the Instagram media identifier.
   * @throws RuntimeException if the Instagram Graph API call fails or the
   *                          response is invalid.
   */
  private String publishContainer(String containerId) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("creation_id", containerId);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://graph.instagram.com/v23.0/" + accountId + "/media_publish"))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + accessKey)
        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
        .build();

    String json = send(request);

    String id;
    try {
      id = MAPPER.readTree(json).at("/id").asText(null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse id", e);
    }

    if (id == null || id.isBlank()) {
      throw new RuntimeException("Instagram media publish did not return an id (response: " + json + ")");
    }

    return id;
  }

  /**
   * Sends an HTTP request and returns the response body as a string.
   *
   * @param request A {@link HttpRequest} representing the request to send.
   * @return A string representing the response body.
   * @throws RuntimeException if the request fails or returns a non-2xx status
   *                          code.
   */
  private String send(HttpRequest request) {
    HttpResponse<String> response;
    try {
      response = http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new RuntimeException("Failed to call Instagram Graph API (I/O error)", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while calling Instagram Graph API", e);
    }

    int status = response.statusCode();
    if (status < 200 || status >= 300) {
      throw new RuntimeException(
          "Instagram Graph API returned non-2xx status " + status + " (body: " + response.body() + ")");
    }

    return response.body();
  }
}
