package info.henrycaldwell.aggregator.retrieve;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.Clip;
import com.github.twitch4j.helix.domain.ClipList;
import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.ClipRef;
import info.henrycaldwell.aggregator.error.SpecException;

/**
 * Class for retrieving clips via the Twitch Helix API.
 * 
 * This class queries the Twitch Clips endpoint for clips matching a configured
 * game or broadcaster.
 */
public final class TwitchRetriever extends AbstractRetriever {

  public static final Spec SPEC = Spec.builder()
      .requiredString("token")
      .optionalString("gameId", "broadcasterId", "language")
      .optionalNumber("window", "limit")
      .optionalStringList("tags")
      .build();

  private final TwitchClient twitch;

  private final String token;
  private final String gameId;
  private final String broadcasterId;
  private final String language;
  private final Duration window;
  private final int limit;
  private final List<String> tags;

  /**
   * Constructs a TwitchRetriever.
   * 
   * @param config A {@link Config} representing the retriever configuration.
   * @throws SpecException if the configuration violates the retriever spec.
   */
  public TwitchRetriever(Config config) {
    super(config, SPEC);

    this.token = config.getString("token");
    this.gameId = config.hasPath("gameId") ? config.getString("gameId") : null;
    this.broadcasterId = config.hasPath("broadcasterId") ? config.getString("broadcasterId") : null;
    this.language = config.hasPath("language") ? config.getString("language") : null;

    long window = config.hasPath("window") ? config.getNumber("window").longValue() : 24L;
    if (window <= 0) {
      throw new SpecException(name, "Invalid key value (expected window to be greater than 0)",
          Map.of("key", "window", "value", window));
    }
    this.window = Duration.ofHours(window);

    int limit = config.hasPath("limit") ? config.getNumber("limit").intValue() : 20;
    if (limit <= 0) {
      throw new SpecException(name, "Invalid key value (expected limit to be greater than 0)",
          Map.of("key", "limit", "value", limit));
    }
    this.limit = limit;

    List<String> tags = config.hasPath("tags") ? config.getStringList("tags") : List.of();
    for (int i = 0; i < tags.size(); i++) {
      String tag = tags.get(i);

      if (tag == null || tag.isBlank()) {
        throw new SpecException(name, "Invalid key value (expected tags to be non-blank strings)",
            Map.of("key", "tags", "value", tag, "index", i));
      }
    }
    this.tags = List.copyOf(tags);

    if ((gameId == null) == (broadcasterId == null)) {
      throw new SpecException(name,
          "Invalid key combination (expected exactly one of gameId or broadcasterId)");
    }

    if (broadcasterId != null && language != null) {
      throw new SpecException(name, "Invalid key combination (expected language only with gameId)");
    }

    this.twitch = TwitchClientBuilder.builder().withEnableHelix(true).build();
  }

  /**
   * Retrieves recent clips for a game or broadcaster.
   *
   * @return A {@link List} of {@link ClipRef} representing the retrieved clips.
   */
  @Override
  public List<ClipRef> fetch() {
    Instant end = Instant.now();
    Instant start = end.minus(window);
    List<Clip> candidates = (gameId != null)
        ? pageClips(gameId, null, start, end, limit, language)
        : pageClips(null, broadcasterId, start, end, limit, null);

    return candidates.stream()
        .sorted(Comparator.comparingInt(Clip::getViewCount).reversed())
        .map(clip -> new ClipRef(
            clip.getId(),
            clip.getUrl(),
            clip.getTitle(),
            clip.getBroadcasterName(),
            clip.getLanguage(),
            clip.getViewCount() != null ? clip.getViewCount() : 0,
            tags))
        .toList();
  }

  /**
   * Pages through Helix clips for the given identifiers and time range.
   *
   * @param gameId        A string representing the game identifier, or
   *                      {@code null}.
   * @param broadcasterId A string representing the broadcaster identifier, or
   *                      {@code null}.
   * @param start         An {@link Instant} representing the inclusive start
   *                      time.
   * @param end           An {@link Instant} representing the exclusive end time.
   * @param limit         An integer representing the maximum number of clips to
   *                      return.
   * @param language      A string representing the clip language, or
   *                      {@code null}.
   * @return A {@link List} of {@link Clip} values gathered across pages.
   */
  private List<Clip> pageClips(
      String gameId,
      String broadcasterId,
      Instant start,
      Instant end,
      int limit,
      String language) {
    List<Clip> matches = new ArrayList<>();
    String cursor = null;

    while (matches.size() < limit) {
      ClipList page = twitch.getHelix()
          .getClips(
              token,
              broadcasterId,
              gameId,
              null,
              cursor,
              null,
              100,
              start,
              end,
              null)
          .execute();

      if (page.getData() == null || page.getData().isEmpty()) {
        break;
      }

      for (Clip clip : page.getData()) {
        if (language == null || language.equalsIgnoreCase(clip.getLanguage())) {
          matches.add(clip);

          if (matches.size() >= limit) {
            break;
          }
        }
      }

      cursor = (page.getPagination() != null) ? page.getPagination().getCursor() : null;

      if (cursor == null) {
        break;
      }
    }

    return matches;
  }
}
