package info.henrycaldwell.streamline.retrieve;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.Cancellable;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Class for retrieving clips via the Twitch Helix API.
 * 
 * This class queries the Twitch Clips endpoint for clips matching a configured
 * game or broadcaster.
 */
public final class TwitchRetriever extends AbstractRetriever {

  public static final Spec SPEC = Spec.builder()
      .requiredString("clientId", "accessKey")
      .optionalString("gameId", "broadcasterId")
      .optionalNumber("window", "limit")
      .optionalStringList("languages", "tags")
      .build();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpClient http;
  private final HttpSender sender;

  private final String clientId;
  private final String accessKey;

  private final String gameId;
  private final String broadcasterId;

  private final Duration window;
  private final int limit;

  private final List<String> languages;
  private final List<String> tags;

  /**
   * Constructs a TwitchRetriever.
   * 
   * @param config A {@link Config} representing the retriever configuration.
   * @throws SpecException if the configuration violates the retriever spec.
   */
  public TwitchRetriever(Config config) {
    this(config, null);
  }

  /**
   * Constructs a TwitchRetriever with a custom HTTP sender for testing.
   *
   * @param config A {@link Config} representing the retriever configuration.
   * @param sender An {@link HttpSender} for dispatching requests, or {@code null}
   *               to use the default Twitch Helix HTTP client.
   */
  TwitchRetriever(Config config, HttpSender sender) {
    super(config, SPEC);

    this.clientId = config.getString("clientId");
    this.accessKey = config.getString("accessKey");
    this.gameId = config.hasPath("gameId") ? config.getString("gameId") : null;
    this.broadcasterId = config.hasPath("broadcasterId") ? config.getString("broadcasterId") : null;

    long window = config.hasPath("window") ? config.getNumber("window").longValue() : 24L;
    if (window <= 0) {
      throw new SpecException(Retriever.TYPE, null, name, "Invalid key value (expected window to be greater than 0)",
          MapUtils.ofNullable("key", "window", "value", window));
    }
    this.window = Duration.ofHours(window);

    int limit = config.hasPath("limit") ? config.getNumber("limit").intValue() : 20;
    if (limit <= 0) {
      throw new SpecException(Retriever.TYPE, null, name, "Invalid key value (expected limit to be greater than 0)",
          MapUtils.ofNullable("key", "limit", "value", limit));
    }
    this.limit = limit;

    List<String> languages = config.hasPath("languages") ? config.getStringList("languages") : List.of();
    for (int i = 0; i < languages.size(); i++) {
      String language = languages.get(i);

      if (language == null || language.isBlank()) {
        throw new SpecException(Retriever.TYPE, null, name, "Invalid key value (expected languages to be non-blank strings)",
            MapUtils.ofNullable("key", "languages", "value", language, "index", i));
      }
    }
    this.languages = List.copyOf(languages);

    List<String> tags = config.hasPath("tags") ? config.getStringList("tags") : List.of();
    for (int i = 0; i < tags.size(); i++) {
      String tag = tags.get(i);

      if (tag == null || tag.isBlank()) {
        throw new SpecException(Retriever.TYPE, null, name, "Invalid key value (expected tags to be non-blank strings)",
            MapUtils.ofNullable("key", "tags", "value", tag, "index", i));
      }
    }
    this.tags = List.copyOf(tags);

    if ((gameId == null) == (broadcasterId == null)) {
      throw new SpecException(Retriever.TYPE, null, name,
          "Invalid key combination (expected exactly one of gameId or broadcasterId)");
    }

    if (broadcasterId != null && !languages.isEmpty()) {
      throw new SpecException(Retriever.TYPE, null, name, "Invalid key combination (expected languages only with gameId)");
    }

    this.http = HttpClient.newHttpClient();
    this.sender = sender != null ? sender : this::defaultSend;
  }

  /**
   * Retrieves recent clips for a game or broadcaster.
   *
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link List} of {@link ClipRef} representing the retrieved clips.
   * @throws ComponentException if fetching fails at any step.
   */
  @Override
  public List<ClipRef> fetch(CancellationToken token) {
    Instant end = Instant.now();
    Instant start = end.minus(window);

    List<Clip> candidates = (gameId != null)
        ? pageClips(gameId, null, start, end, token)
        : pageClips(null, broadcasterId, start, end, token);

    return candidates.stream()
        .sorted(Comparator.comparingInt(Clip::viewCount).reversed())
        .map(c -> new ClipRef(c.id(), c.url(), c.title(), c.broadcasterName(), c.language(), c.viewCount(), tags))
        .toList();
  }

  /**
   * Pages through Twitch clips for the given identifiers and time range.
   *
   * @param gameId        A string representing the game identifier, or
   *                      {@code null}.
   * @param broadcasterId A string representing the broadcaster identifier, or
   *                      {@code null}.
   * @param start         An {@link Instant} representing the inclusive start
   *                      time.
   * @param end           An {@link Instant} representing the exclusive end time.
   * @param token         A {@link CancellationToken} representing the
   *                      cancellation
   *                      signal.
   * @return A {@link List} of {@link Clip} values gathered across pages.
   * @throws ComponentException if an API call fails or the response is invalid.
   */
  private List<Clip> pageClips(String gameId, String broadcasterId, Instant start, Instant end,
      CancellationToken token) {
    List<Clip> matches = new ArrayList<>();
    String cursor = null;

    while (matches.size() < limit) {
      StringBuilder url = new StringBuilder("https://api.twitch.tv/helix/clips?");
      if (gameId != null) {
        url.append("game_id=").append(gameId);
      } else {
        url.append("broadcaster_id=").append(broadcasterId);
      }
      url.append("&started_at=").append(start);
      url.append("&ended_at=").append(end);
      url.append("&first=100");
      if (cursor != null) {
        url.append("&after=").append(cursor);
      }

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url.toString()))
          .header("Authorization", "Bearer " + accessKey)
          .header("Client-Id", clientId)
          .GET()
          .build();

      String json = sender.send(request, token);

      JsonNode root;
      try {
        root = MAPPER.readTree(json);
      } catch (IOException e) {
        throw new ComponentException(Retriever.TYPE, null, name, "Failed to parse Twitch clips",
            MapUtils.ofNullable("responseBody", json), e);
      }

      JsonNode data = root.path("data");
      if (!data.isArray() || data.isEmpty()) {
        break;
      }

      for (JsonNode node : data) {
        String language = node.path("language").asText(null);
        if (!languages.isEmpty() && !languages.contains(language)) {
          continue;
        }

        matches.add(new Clip(
            node.path("id").asText(null),
            node.path("url").asText(null),
            node.path("title").asText(null),
            node.path("broadcaster_name").asText(null),
            language,
            node.path("view_count").asInt(0)));

        if (matches.size() >= limit) {
          break;
        }
      }

      JsonNode paginationCursor = root.path("pagination").path("cursor");
      cursor = paginationCursor.isMissingNode() || paginationCursor.isNull()
          ? null
          : paginationCursor.asText(null);

      if (cursor == null) {
        break;
      }
    }

    return matches;
  }

  /**
   * Sends an HTTP request using the default Twitch HTTP client.
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
      throw new ComponentException(Retriever.TYPE, null, name, "Canceled while calling Twitch Helix API",
          MapUtils.ofNullable("method", method, "uri", uri.toString()), e);
    } catch (ExecutionException e) {
      throw new ComponentException(Retriever.TYPE, null, name, "Failed to call Twitch Helix API",
          MapUtils.ofNullable("method", method, "uri", uri.toString()), e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      throw new ComponentException(Retriever.TYPE, null, name, "Interrupted while calling Twitch Helix API",
          MapUtils.ofNullable("method", method, "uri", uri.toString()), e);
    } finally {
      token.unregister(abort);
    }

    int status = response.statusCode();
    String body = response.body();
    if (status < 200 || status >= 300) {
      throw new ComponentException(Retriever.TYPE, null, name, "Twitch Helix API returned non-2xx status",
          MapUtils.ofNullable("method", method, "uri", uri.toString(), "statusCode", status, "responseBody", body));
    }

    return body;
  }

  private record Clip(
      String id,
      String url,
      String title,
      String broadcasterName,
      String language,
      int viewCount) {
  }
}
