package info.henrycaldwell.streamline.publish;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.Cancellable;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Class for publishing media to Instagram Reels via the Instagram Graph API.
 * 
 * This class publishes the input media to Instagram Reels and returns a
 * publish for the resulting clip.
 */
public final class InstagramPublisher extends AbstractPublisher {

  public static final Spec SPEC = Spec.builder()
      .requiredString("accountId", "accessKey")
      .optionalString("captionText")
      .optionalNumber("timeout", "interval")
      .build();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpClient http;
  private final HttpSender sender;

  private final String accountId;
  private final String accessKey;

  private final String captionText;

  private final long timeout;
  private final long interval;

  /**
   * Constructs an InstagramPublisher.
   *
   * @param config A {@link Config} representing the publisher configuration.
   */
  public InstagramPublisher(Config config) {
    this(config, null);
  }

  /**
   * Constructs an InstagramPublisher with a custom HTTP sender for testing.
   *
   * @param config A {@link Config} representing the publisher configuration.
   * @param sender An {@link HttpSender} for dispatching requests, or {@code null}
   *               to use the default Instagram HTTP client.
   * @throws SpecException if the configuration violates the publisher spec.
   */
  InstagramPublisher(Config config, HttpSender sender) {
    super(config, SPEC);

    this.http = HttpClient.newHttpClient();
    this.accountId = config.getString("accountId");
    this.accessKey = config.getString("accessKey");
    this.captionText = config.hasPath("captionText") ? config.getString("captionText") : null;

    long timeout = config.hasPath("timeout") ? config.getNumber("timeout").longValue() : 180L;
    if (timeout <= 0) {
      throw new SpecException(Publisher.TYPE, null, name, "Invalid key value (expected timeout to be greater than 0)",
          MapUtils.ofNullable("key", "timeout", "value", timeout));
    }
    this.timeout = timeout;

    long interval = config.hasPath("interval") ? config.getNumber("interval").longValue() : 30L;
    if (interval <= 0) {
      throw new SpecException(Publisher.TYPE, null, name, "Invalid key value (expected interval to be greater than 0)",
          MapUtils.ofNullable("key", "interval", "value", interval));
    }
    this.interval = interval;

    this.sender = sender != null ? sender : this::defaultSend;
  }

  /**
   * Publishes the input media as an Instagram Reel.
   *
   * @param media A {@link MediaRef} representing the media to publish.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link PublishRef} representing the published clip.
   * @throws ComponentException if publishing fails at any step.
   */
  @Override
  public PublishRef publish(MediaRef media, CancellationToken token) {
    URI uri = media.uri();

    if (uri == null || uri.getScheme() == null
        || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
      throw new ComponentException(Publisher.TYPE, null, name, "Media URI missing or not HTTP(S)",
          MapUtils.ofNullable("uri", uri, "mediaId", media.clip().id()));
    }

    String url = uri.toString();
    String caption = buildCaption(media);

    String containerId = createContainer(url, caption, token);
    awaitContainer(containerId, token);
    String mediaId = publishContainer(containerId, token);
    String permalink = fetchPermalink(mediaId, token);

    return new PublishRef(media.clip(), URI.create(permalink));
  }

  /**
   * Creates an Instagram Reels media container.
   *
   * @param url     A string representing the public video URL.
   * @param caption A string representing the caption, or {@code null}.
   * @param token   A {@link CancellationToken} representing the cancellation
   *                signal.
   * @return A string representing the container identifier.
   * @throws ComponentException if the Instagram Graph API call fails or the
   *                            response is invalid.
   */
  private String createContainer(String url, String caption, CancellationToken token) {
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

    String json = sender.send(request, token);

    String id;
    try {
      id = MAPPER.readTree(json).at("/id").asText(null);
    } catch (IOException e) {
      throw new ComponentException(Publisher.TYPE, null, name, "Failed to parse Instagram media container id",
          MapUtils.ofNullable("endpoint", endpoint.toString(), "responseBody", json), e);
    }

    if (id == null || id.isBlank()) {
      throw new ComponentException(Publisher.TYPE, null, name,
          "Instagram media container creation did not return an id",
          MapUtils.ofNullable("endpoint", endpoint.toString(), "responseBody", json));
    }

    return id;
  }

  /**
   * Waits for the Instagram Reels media container to become ready for publishing.
   *
   * @param containerId A string representing the container identifier.
   * @param token       A {@link CancellationToken} representing the cancellation
   *                    signal.
   * @throws ComponentException if the container does not become ready within the
   *                            timeout or enters an error state.
   */
  private void awaitContainer(String containerId, CancellationToken token) {
    long start = System.nanoTime();

    while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(timeout)) {
      URI endpoint = URI
          .create("https://graph.instagram.com/v23.0/" + containerId + "?fields=status_code,error_message");

      HttpRequest request = HttpRequest.newBuilder()
          .uri(endpoint)
          .header("Authorization", "Bearer " + accessKey)
          .GET()
          .build();

      String json = sender.send(request, token);

      String status;
      String error;
      try {
        status = MAPPER.readTree(json).at("/status_code").asText(null);
        error = MAPPER.readTree(json).at("/error_message").asText(null);
      } catch (IOException e) {
        throw new ComponentException(Publisher.TYPE, null, name, "Failed to parse Instagram media container status",
            MapUtils.ofNullable("containerId", containerId, "endpoint", endpoint.toString(), "responseBody", json), e);
      }

      if (status == null || status.isBlank()) {
        throw new ComponentException(Publisher.TYPE, null, name, "Instagram media container status missing status code",
            MapUtils.ofNullable("containerId", containerId, "endpoint", endpoint.toString(), "responseBody", json));
      }

      if ("FINISHED".equalsIgnoreCase(status)) {
        return;
      }

      if ("ERROR".equalsIgnoreCase(status)) {
        throw new ComponentException(Publisher.TYPE, null, name, "Instagram media container status entered error state",
            MapUtils.ofNullable("containerId", containerId, "endpoint", endpoint.toString(), "responseBody", json,
                "error", String.valueOf(error)));
      }

      boolean canceled;
      try {
        canceled = token.awaitCancellation(interval);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ComponentException(Publisher.TYPE, null, name,
            "Interrupted while waiting for Instagram media container",
            MapUtils.ofNullable("containerId", containerId), e);
      }

      if (canceled) {
        throw new ComponentException(Publisher.TYPE, null, name, "Canceled while waiting for Instagram media container",
            MapUtils.ofNullable("containerId", containerId));
      }
    }

    throw new ComponentException(Publisher.TYPE, null, name, "Timed out while waiting for Instagram media container",
        MapUtils.ofNullable("containerId", containerId));
  }

  /**
   * Publishes an Instagram Reels media container.
   *
   * @param containerId A string representing the container identifier.
   * @param token       A {@link CancellationToken} representing the cancellation
   *                    signal.
   * @return A string representing the media identifier.
   * @throws ComponentException if the Instagram Graph API call fails or the
   *                            response is invalid.
   */
  private String publishContainer(String containerId, CancellationToken token) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("creation_id", containerId);

    URI endpoint = URI.create("https://graph.instagram.com/v23.0/" + accountId + "/media_publish");

    HttpRequest request = HttpRequest.newBuilder()
        .uri(endpoint)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + accessKey)
        .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
        .build();

    String json = sender.send(request, token);

    String id;
    try {
      id = MAPPER.readTree(json).at("/id").asText(null);
    } catch (IOException e) {
      throw new ComponentException(Publisher.TYPE, null, name, "Failed to parse Instagram media id",
          MapUtils.ofNullable("endpoint", endpoint.toString(), "responseBody", json), e);
    }

    if (id == null || id.isBlank()) {
      throw new ComponentException(Publisher.TYPE, null, name, "Instagram media publish did not return an id",
          MapUtils.ofNullable("endpoint", endpoint.toString(), "responseBody", json));
    }

    return id;
  }

  /**
   * Fetches the permalink for a published Instagram media object.
   * 
   * @param mediaId A string representing the media identifier.
   * @param token   A {@link CancellationToken} representing the cancellation
   *                signal.
   * @return A string representing the permalink URL.
   * @throws ComponentException if the Instagram Graph API call fails or the
   *                            response is invalid.
   */
  private String fetchPermalink(String mediaId, CancellationToken token) {
    URI endpoint = URI.create("https://graph.instagram.com/v23.0/" + mediaId + "?fields=permalink");

    HttpRequest request = HttpRequest.newBuilder()
        .uri(endpoint)
        .header("Authorization", "Bearer " + accessKey)
        .GET()
        .build();

    String json = sender.send(request, token);

    String permalink;
    try {
      permalink = MAPPER.readTree(json).at("/permalink").asText(null);
    } catch (IOException e) {
      throw new ComponentException(Publisher.TYPE, null, name, "Failed to parse Instagram media permalink",
          MapUtils.ofNullable("endpoint", endpoint.toString(), "responseBody", json), e);
    }

    if (permalink == null || permalink.isBlank()) {
      throw new ComponentException(Publisher.TYPE, null, name, "Instagram media permalink did not return a permalink",
          MapUtils.ofNullable("endpoint", endpoint.toString(), "responseBody", json));
    }

    return permalink;
  }

  /**
   * Builds the Instagram caption.
   * 
   * @param media A {@link MediaRef} representing the media to caption.
   * @return A string representing the Instagram caption.
   */
  private String buildCaption(MediaRef media) {
    String broadcaster = media.clip().broadcaster();
    String title = media.clip().title();

    StringBuilder sb = new StringBuilder();
    sb.append(broadcaster != null ? broadcaster : "N/A")
        .append(" - ")
        .append(title != null ? title : "N/A");

    if (captionText != null && !captionText.isBlank()) {
      sb.append("\n\n").append(captionText);
    }

    if (media.clip().tags() != null && !media.clip().tags().isEmpty()) {
      sb.append("\n\n");

      boolean first = true;
      for (String tag : media.clip().tags()) {
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

  /**
   * Sends an HTTP request using the default Instagram HTTP client.
   *
   * @param request A {@link HttpRequest} representing the request to send.
   * @param token   A {@link CancellationToken} representing the cancellation
   *                signal.
   * @return A string representing the response body.
   * @throws ComponentException if the request fails or returns a non-2xx status
   *                            code.
   */
  private String defaultSend(HttpRequest request, CancellationToken token) {
    URI uri = request.uri();
    String method = request.method();

    CompletableFuture<HttpResponse<String>> future = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    Cancellable abort = () -> future.cancel(true);
    token.register(abort);

    HttpResponse<String> response;
    try {
      response = future.get();
    } catch (CancellationException e) {
      throw new ComponentException(Publisher.TYPE, null, name, "Canceled while calling Instagram Graph API",
          MapUtils.ofNullable("method", method, "uri", uri.toString()), e);
    } catch (ExecutionException e) {
      throw new ComponentException(Publisher.TYPE, null, name, "Failed to call Instagram Graph API",
          MapUtils.ofNullable("method", method, "uri", uri.toString()), e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      throw new ComponentException(Publisher.TYPE, null, name, "Interrupted while calling Instagram Graph API",
          MapUtils.ofNullable("method", method, "uri", uri.toString()), e);
    } finally {
      token.unregister(abort);
    }

    int status = response.statusCode();
    String body = response.body();
    if (status < 200 || status >= 300) {
      throw new ComponentException(Publisher.TYPE, null, name, "Instagram Graph API returned non-2xx status",
          MapUtils.ofNullable("method", method, "uri", uri.toString(), "statusCode", status, "responseBody", body));
    }

    return body;
  }
}
