package info.henrycaldwell.aggregator.retrieval;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.Clip;
import com.github.twitch4j.helix.domain.ClipList;

import info.henrycaldwell.aggregator.core.ClipRef;

/**
 * Class for retrieving clips from Twitch Helix.
 * 
 * This class queries the Clips endpoint for a game or broadcaster.
 */
public class TwitchRetriever implements Retriever {

  private final TwitchClient twitch;
  private final String token;

  /**
   * Constructs a TwitchRetriever using a Helix-enabled Twitch client.
   * 
   * @param token A string representing the app access token.
   */
  public TwitchRetriever(String token) {
    this.twitch = TwitchClientBuilder.builder().withEnableHelix(true).build();
    this.token = token;
  }

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
  @Override
  public List<ClipRef> fetchGame(String id, Duration window, int limit, String language) {
    Instant end = Instant.now();
    Instant start = end.minus(window);
    List<Clip> candidates = pageClips(id, null, start, end, limit);

    return candidates.stream()
        .sorted(Comparator.comparingInt(Clip::getViewCount).reversed())
        .filter(c -> language == null || language.equalsIgnoreCase(c.getLanguage()))
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
   * Retrieves recent clips for a broadcaster within a time window.
   * 
   * @param id     A string representing the broadcaster id.
   * @param window A duration representing how far back to look (e.g., 24
   *               hours).
   * @param limit  An integer representing the maximum number of clips to
   *               return.
   * @return A list of {@link ClipRef} suitable for downloading.
   */
  @Override
  public List<ClipRef> fetchBroadcaster(String id, Duration window, int limit) {
    Instant end = Instant.now();
    Instant start = end.minus(window);
    List<Clip> candidates = pageClips(null, id, start, end, limit);

    return candidates.stream()
        .sorted(Comparator.comparingInt(Clip::getViewCount).reversed())
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
