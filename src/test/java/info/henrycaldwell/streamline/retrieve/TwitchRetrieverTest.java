package info.henrycaldwell.streamline.retrieve;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;

public class TwitchRetrieverTest {

  @Nested
  class Constructor {

    @Test
    void acceptsConfiguredGameId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          """);

      assertDoesNotThrow(() -> new TwitchRetriever(config));
    }

    @Test
    void acceptsConfiguredBroadcasterId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          broadcasterId = broadcaster-1
          """);

      assertDoesNotThrow(() -> new TwitchRetriever(config));
    }

    @Test
    void acceptsConfiguredWindow() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          window = 48
          """);

      assertDoesNotThrow(() -> new TwitchRetriever(config));
    }

    @Test
    void acceptsConfiguredLimit() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          limit = 5
          """);

      assertDoesNotThrow(() -> new TwitchRetriever(config));
    }

    @Test
    void acceptsConfiguredLanguages() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          languages = [en, fr]
          """);

      assertDoesNotThrow(() -> new TwitchRetriever(config));
    }

    @Test
    void acceptsConfiguredTags() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          tags = [funny, highlights]
          """);

      assertDoesNotThrow(() -> new TwitchRetriever(config));
    }

    @Test
    void throwsOnMissingClientId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          accessKey = key-1
          gameId = game-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=clientId"));
    }

    @Test
    void throwsOnWrongTypeForClientId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = [client-1]
          accessKey = key-1
          gameId = game-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=clientId"));
    }

    @Test
    void throwsOnMissingToken() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          gameId = game-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=accessKey"));
    }

    @Test
    void throwsOnWrongTypeForToken() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = [key-1]
          gameId = game-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=accessKey"));
    }

    @Test
    void throwsOnWrongTypeForGameId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = [game-1]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=gameId"));
    }

    @Test
    void throwsOnWrongTypeForBroadcasterId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          broadcasterId = [broadcaster-1]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=broadcasterId"));
    }

    @Test
    void throwsOnWrongTypeForWindow() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          window = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=window"));
    }

    @Test
    void throwsOnInvalidWindow() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          window = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=window"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForLimit() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          limit = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=limit"));
    }

    @Test
    void throwsOnInvalidLimit() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          limit = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=limit"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForLanguages() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          languages = en
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list<string>)"));
      assertTrue(exception.getMessage().contains("key=languages"));
    }

    @Test
    void throwsOnBlankLanguage() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          languages = [""]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=languages"));
    }

    @Test
    void throwsOnWrongTypeForTags() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          tags = funny
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list<string>)"));
      assertTrue(exception.getMessage().contains("key=tags"));
    }

    @Test
    void throwsOnBlankTag() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          tags = [""]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=tags"));
    }

    @Test
    void throwsOnMissingGameIdAndBroadcasterId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Invalid key combination"));
      assertTrue(exception.getMessage().contains("gameId"));
      assertTrue(exception.getMessage().contains("broadcasterId"));
    }

    @Test
    void throwsOnBothGameIdAndBroadcasterId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          broadcasterId = broadcaster-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Invalid key combination"));
      assertTrue(exception.getMessage().contains("gameId"));
      assertTrue(exception.getMessage().contains("broadcasterId"));
    }

    @Test
    void throwsOnLanguagesWithBroadcasterId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          broadcasterId = broadcaster-1
          languages = [en]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Invalid key combination"));
      assertTrue(exception.getMessage().contains("languages"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TwitchRetriever(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Fetch {

    @Test
    void returnsSortedClipRefsForGameId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          """);
      HttpSender sender = (request, token) -> """
          {
            "data": [
              {
                "id": "clip-2",
                "url": "https://clips.twitch.tv/clip-2",
                "title": "Title Two",
                "broadcaster_name": "Broadcaster Two",
                "language": "en",
                "view_count": 50
              },
              {
                "id": "clip-1",
                "url": "https://clips.twitch.tv/clip-1",
                "title": "Title One",
                "broadcaster_name": "Broadcaster One",
                "language": "en",
                "view_count": 100
              }
            ],
            "pagination": {}
          }
          """;
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      List<ClipRef> result = assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertEquals(2, result.size());
      assertEquals("clip-1", result.get(0).id());
      assertEquals("https://clips.twitch.tv/clip-1", result.get(0).url());
      assertEquals("Title One", result.get(0).title());
      assertEquals("Broadcaster One", result.get(0).broadcaster());
      assertEquals("en", result.get(0).language());
      assertEquals(100, result.get(0).views());
      assertEquals("clip-2", result.get(1).id());
      assertEquals("https://clips.twitch.tv/clip-2", result.get(1).url());
      assertEquals("Title Two", result.get(1).title());
      assertEquals("Broadcaster Two", result.get(1).broadcaster());
      assertEquals("en", result.get(1).language());
      assertEquals(50, result.get(1).views());
    }

    @Test
    void returnsSortedClipRefsForBroadcasterId() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          broadcasterId = broadcaster-1
          """);
      HttpSender sender = (request, token) -> """
          {
            "data": [
              {
                "id": "clip-1",
                "url": "https://clips.twitch.tv/clip-1",
                "title": "Title One",
                "broadcaster_name": "Broadcaster One",
                "language": "en",
                "view_count": 100
              }
            ],
            "pagination": {}
          }
          """;
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      List<ClipRef> result = assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertEquals(1, result.size());
      assertEquals("clip-1", result.get(0).id());
      assertEquals("https://clips.twitch.tv/clip-1", result.get(0).url());
      assertEquals("Title One", result.get(0).title());
      assertEquals("Broadcaster One", result.get(0).broadcaster());
      assertEquals("en", result.get(0).language());
      assertEquals(100, result.get(0).views());
    }

    @Test
    void returnsEmptyListWhenNoClipsReturned() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          """);
      HttpSender sender = (request, token) -> "{\"data\": [], \"pagination\": {}}";
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      List<ClipRef> result = assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertTrue(result.isEmpty());
    }

    @Test
    void pagesForMoreClips() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> """
            {
              "data": [
                {
                  "id": "clip-1",
                  "url": "https://clips.twitch.tv/clip-1",
                  "title": "Title One",
                  "broadcaster_name": "Broadcaster One",
                  "language": "en",
                  "view_count": 100
                }
              ],
              "pagination": {"cursor": "next-page"}
            }
            """;
        default -> """
            {
              "data": [
                {
                  "id": "clip-2",
                  "url": "https://clips.twitch.tv/clip-2",
                  "title": "Title Two",
                  "broadcaster_name": "Broadcaster Two",
                  "language": "en",
                  "view_count": 50
                }
              ],
              "pagination": {}
            }
            """;
      };
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      List<ClipRef> result = assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertEquals(2, result.size());
      assertEquals(2, call[0]);
    }

    @Test
    void limitsByWindow() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          window = 48
          """);
      Duration[] capturedWindow = { null };
      HttpSender sender = (request, token) -> {
        String query = request.uri().getQuery();
        Map<String, String> params = Arrays.stream(query.split("&"))
            .map(p -> p.split("=", 2))
            .filter(kv -> kv.length == 2)
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
        Instant startedAt = Instant.parse(params.get("started_at"));
        Instant endedAt = Instant.parse(params.get("ended_at"));
        capturedWindow[0] = Duration.between(startedAt, endedAt);
        return "{\"data\": [], \"pagination\": {}}";
      };
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertEquals(Duration.ofHours(48), capturedWindow[0]);
    }

    @Test
    void limitsByCount() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          limit = 1
          """);
      HttpSender sender = (request, token) -> """
          {
            "data": [
              {
                "id": "clip-1",
                "url": "https://clips.twitch.tv/clip-1",
                "title": "Title One",
                "broadcaster_name": "Broadcaster One",
                "language": "en",
                "view_count": 100
              },
              {
                "id": "clip-2",
                "url": "https://clips.twitch.tv/clip-2",
                "title": "Title Two",
                "broadcaster_name": "Broadcaster Two",
                "language": "en",
                "view_count": 50
              }
            ],
            "pagination": {}
          }
          """;
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      List<ClipRef> result = assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertEquals(1, result.size());
    }

    @Test
    void filtersByLanguage() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          languages = [en]
          """);
      HttpSender sender = (request, token) -> """
          {
            "data": [
              {
                "id": "clip-1",
                "url": "https://clips.twitch.tv/clip-1",
                "title": "Title One",
                "broadcaster_name": "Broadcaster One",
                "language": "en",
                "view_count": 100
              },
              {
                "id": "clip-2",
                "url": "https://clips.twitch.tv/clip-2",
                "title": "Title Two",
                "broadcaster_name": "Broadcaster Two",
                "language": "fr",
                "view_count": 50
              }
            ],
            "pagination": {}
          }
          """;
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      List<ClipRef> result = assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertEquals(1, result.size());
      assertEquals("clip-1", result.get(0).id());
    }

    @Test
    void propagatesTags() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          tags = [funny, highlights]
          """);
      HttpSender sender = (request, token) -> """
          {
            "data": [
              {
                "id": "clip-1",
                "url": "https://clips.twitch.tv/clip-1",
                "title": "Title One",
                "broadcaster_name": "Broadcaster One",
                "language": "en",
                "view_count": 100
              }
            ],
            "pagination": {}
          }
          """;
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      List<ClipRef> result = assertDoesNotThrow(() -> retriever.fetch(new CancellationToken()));

      assertEquals(List.of("funny", "highlights"), result.get(0).tags());
    }

    @Test
    void throwsWhenApiFails() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          """);
      HttpSender sender = (request, token) -> {
        throw new ComponentException("retriever", "HTTP call failed");
      };
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      assertThrows(ComponentException.class, () -> retriever.fetch(new CancellationToken()));
    }

    @Test
    void throwsOnInvalidJson() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = twitch
          clientId = client-1
          accessKey = key-1
          gameId = game-1
          """);
      HttpSender sender = (request, token) -> "not-valid-json";
      TwitchRetriever retriever = new TwitchRetriever(config, sender);

      ComponentException exception = assertThrows(ComponentException.class, () -> retriever.fetch(new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to parse Twitch clips"));
    }
  }
}
