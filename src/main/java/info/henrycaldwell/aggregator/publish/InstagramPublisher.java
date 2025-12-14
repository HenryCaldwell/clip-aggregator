package info.henrycaldwell.aggregator.publish;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.core.PublishRef;
import info.henrycaldwell.aggregator.error.ComponentException;

/**
 * Class for publishing media to Instagram Reels via the Instagram Graph API.
 * 
 * This class publishes the input media to Instagram Reels and returns a
 * publish for the resulting short.
 */
public final class InstagramPublisher extends AbstractPublisher {

  public static final Spec SPEC = Spec.builder()
      .requiredString("accountId", "accessKey")
      .optionalString("captionText")
      .build();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpClient http;
  private final String accountId;
  private final String accessKey;
  private final String captionText;

  /**
   * Constructs an InstagramPublisher.
   *
   * @param config A {@link Config} representing the publisher configuration.
   */
  public InstagramPublisher(Config config) {
    super(config, SPEC);

    this.http = HttpClient.newHttpClient();
    this.accountId = config.getString("accountId");
    this.accessKey = config.getString("accessKey");
    this.captionText = config.hasPath("captionText") ? config.getString("captionText") : null;
  }

  /**
   * Publishes the input media as an Instagram Reel.
   *
   * @param media A {@link MediaRef} representing the media to publish.
   * @return A {@link PublishRef} representing the published short.
   * @throws ComponentException if publishing fails at any step.
   */
  @Override
  public PublishRef publish(MediaRef media) {
    URI uri = media.uri();

    if (uri == null || uri.getScheme() == null
        || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
      throw new ComponentException(name, "Media URI missing or not HTTP(S)", Map.of("uri", uri, "mediaId", media.id()));
    }

    String url = uri.toString();
    String caption = buildCaption(media);

    String containerId = createContainer(url, caption);
    awaitContainer(containerId);
    String mediaId = publishContainer(containerId);

    return new PublishRef(mediaId, null);
  }

  /**
   * Creates an Instagram Reels media container.
   *
   * @param url     A string representing the public video URL.
   * @param caption A string representing the caption, or {@code null}.
   * @return A string representing the container identifier.
   * @throws ComponentException if the Instagram Graph API call fails or the
   *                            response is invalid.
   */
  private String createContainer(String url, String caption) {
    ObjectNode root = MAPPER.createObjectNode();

    root.put("video_url", url);
    root.put("media_type", "REELS");
    if (caption != null && !caption.isBlank()) {
      root.put("caption", caption);
    }

    URI endpoint = URI.create("https://graph.instagram.com/v23.0/" + accountId + "/media");

    HttpRequest request = HttpRequest.newBuilder()
        .uri(endpoint)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + accessKey)
        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
        .build();

    String json = send(request);

    String id;
    try {
      id = MAPPER.readTree(json).at("/id").asText(null);
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to parse Instagram media container id",
          Map.of("endpoint", endpoint.toString(), "responseBody", json), e);
    }

    if (id == null || id.isBlank()) {
      throw new ComponentException(name, "Instagram media container creation did not return an id",
          Map.of("endpoint", endpoint.toString(), "responseBody", json));
    }

    return id;
  }

  /**
   * Waits for the Instagram Reels media container to become ready for publishing.
   *
   * @param containerId A string representing the container identifier.
   * @throws ComponentException if the container does not become ready within the
   *                            timeout or enters an error state.
   */
  private void awaitContainer(String containerId) {
    long timeout = TimeUnit.MINUTES.toNanos(5);
    long start = System.nanoTime();

    while (System.nanoTime() - start < timeout) {
      URI endpoint = URI.create("https://graph.instagram.com/v23.0/" + containerId + "?fields=status_code");

      HttpRequest request = HttpRequest.newBuilder()
          .uri(endpoint)
          .header("Authorization", "Bearer " + accessKey)
          .GET()
          .build();

      String json = send(request);

      String status;
      try {
        status = MAPPER.readTree(json).at("/status_code").asText(null);
      } catch (IOException e) {
        throw new ComponentException(name, "Failed to parse Instagram media container status",
            Map.of("containerId", containerId, "endpoint", endpoint.toString(), "responseBody", json), e);
      }

      if (status == null || status.isBlank()) {
        throw new ComponentException(name, "Instagram media container status missing status code",
            Map.of("containerId", containerId, "endpoint", endpoint.toString(), "responseBody", json));
      }

      if ("FINISHED".equalsIgnoreCase(status)) {
        return;
      }

      if ("ERROR".equalsIgnoreCase(status)) {
        throw new ComponentException(name, "Instagram media container status entered error state",
            Map.of("containerId", containerId, "endpoint", endpoint.toString(), "responseBody", json));
      }

      try {
        Thread.sleep(30000L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ComponentException(name, "Interrupted while waiting for Instagram media container",
            Map.of("containerId", containerId), e);
      }
    }

    throw new ComponentException(name, "Timed out while waiting for Instagram media container",
        Map.of("containerId", containerId));
  }

  /**
   * Publishes an Instagram Reels media container.
   *
   * @param containerId A string representing the container identifier.
   * @return A string representing the Instagram media identifier.
   * @throws ComponentException if the Instagram Graph API call fails or the
   *                            response is invalid.
   */
  private String publishContainer(String containerId) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("creation_id", containerId);

    URI endpoint = URI.create("https://graph.instagram.com/v23.0/" + accountId + "/media_publish");

    HttpRequest request = HttpRequest.newBuilder()
        .uri(endpoint)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + accessKey)
        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
        .build();

    String json = send(request);

    String id;
    try {
      id = MAPPER.readTree(json).at("/id").asText(null);
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to parse Instagram media id",
          Map.of("endpoint", endpoint.toString(), "responseBody", json), e);
    }

    if (id == null || id.isBlank()) {
      throw new ComponentException(name, "Instagram media publish did not return an id",
          Map.of("endpoint", endpoint.toString(), "responseBody", json));
    }

    return id;
  }

  /**
   * Sends an HTTP request and returns the response body as a string.
   *
   * @param request A {@link HttpRequest} representing the request to send.
   * @return A string representing the response body.
   * @throws ComponentException if the request fails or returns a non-2xx status
   *                            code.
   */
  private String send(HttpRequest request) {
    URI uri = request.uri();
    String method = request.method();

    HttpResponse<String> response;
    try {
      response = http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to call Instagram Graph API",
          Map.of("method", method, "uri", uri.toString()), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ComponentException(name, "Interrupted while calling Instagram Graph API",
          Map.of("method", method, "uri", uri.toString()), e);
    }

    int status = response.statusCode();
    String body = response.body();
    if (status < 200 || status >= 300) {
      throw new ComponentException(name, "Instagram Graph API returned non-2xx status",
          Map.of("method", method, "uri", uri.toString(), "statusCode", status, "responseBody", body));
    }

    return body;
  }

  /**
   * Builds the Instagram caption.
   * 
   * @param media A {@link MediaRef} representing the media to caption.
   * @return A string representing the Instagram caption.
   */
  private String buildCaption(MediaRef media) {
    String broadcaster = media.broadcaster();
    String title = media.title();

    StringBuilder sb = new StringBuilder();
    sb.append(broadcaster != null ? broadcaster : "N/A")
        .append(" - ")
        .append(title != null ? title : "N/A");

    if (captionText != null && !captionText.isBlank()) {
      sb.append("\n\n").append(captionText);
    }

    if (media.tags() != null && !media.tags().isEmpty()) {
      sb.append("\n\n");

      boolean first = true;
      for (String tag : media.tags()) {
        if (tag == null || tag.isBlank()) {
          continue;
        }

        if (!first) {
          sb.append(" ");
        }

        sb.append("#").append(tag);
        first = false;
      }
    }

    return sb.toString();
  }

}
