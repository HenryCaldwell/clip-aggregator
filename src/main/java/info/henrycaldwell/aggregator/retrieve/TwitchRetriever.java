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
 * Class for retrieving clips from Twitch Helix.
 * 
 * This class queries the Twitch Clips endpoint for a game or broadcaster.
 */
public final class TwitchRetriever extends AbstractRetriever {

  public static final Spec SPEC = Spec.builder()
      .requiredString("token")
      .optionalString("gameId", "broadcasterId", "language")
      .optionalNumber("window", "limit")
      .build();

  private final TwitchClient twitch;

  private final String token;
  private final String gameId;
  private final String broadcasterId;
  private final String language;
  private final Duration window;
  private final int limit;

  /**
   * Constructs a TwitchRetriever.
   * 
   * @param config A {@link Config} representing the retriever block.
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
   * Retrieves clips for a game or broadcaster.
   *
   * @return A list of {@link ClipRef} representing the retrieved clips.
   */
  @Override
  public List<ClipRef> fetch() {
    Instant end = Instant.now();
    Instant start = end.minus(window);
    List<Clip> candidates = (gameId != null)
        ? pageClips(gameId, null, start, end, limit)
        : pageClips(null, broadcasterId, start, end, limit);

    return candidates.stream()
        .sorted(Comparator.comparingInt(Clip::getViewCount).reversed())
        .filter(c -> gameId == null || language == null || language.equalsIgnoreCase(c.getLanguage()))
        .limit(limit)
        .map(c -> new ClipRef(
            c.getId(),
            c.getUrl(),
            c.getTitle(),
            c.getBroadcasterName(),
            c.getLanguage()))
        .toList();
  }

  /**
   * Pages through Helix clips for the given identifiers and time range.
   *
   * @param gameId        A string representing the game id, or {@code null}.
   * @param broadcasterId A string representing the broadcaster id, or
   *                      {@code null}.
   * @param start         An instant representing the inclusive start time.
   * @param end           An instant representing the exclusive end time.
   * @param limit         An integer representing the maximum number of clips to
   *                      return.
   * @return A list of {@link Clip} values gathered across pages.
   */
  private List<Clip> pageClips(String gameId, String broadcasterId, Instant start, Instant end, int limit) {
    List<Clip> all = new ArrayList<>();
    String cursor = null;

    while (all.size() < limit) {
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

      all.addAll(page.getData());
      cursor = (page.getPagination() != null) ? page.getPagination().getCursor() : null;

      if (cursor == null) {
        break;
      }
    }

    return all;
  }
}
