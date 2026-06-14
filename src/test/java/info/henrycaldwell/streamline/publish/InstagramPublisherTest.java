package info.henrycaldwell.streamline.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationReason;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;

public class InstagramPublisherTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, null, null, null, 0, null);

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);

      assertDoesNotThrow(() -> new InstagramPublisher(config));
    }

    @Test
    void acceptsConfiguredCaptionText() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          captionText = some caption
          """);

      assertDoesNotThrow(() -> new InstagramPublisher(config));
    }

    @Test
    void acceptsConfiguredTimeout() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          timeout = 60
          """);

      assertDoesNotThrow(() -> new InstagramPublisher(config));
    }

    @Test
    void acceptsConfiguredInterval() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          interval = 10
          """);

      assertDoesNotThrow(() -> new InstagramPublisher(config));
    }

    @Test
    void throwsOnMissingAccountId() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accessKey = key-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=accountId"));
    }

    @Test
    void throwsOnWrongTypeForAccountId() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = [account-1]
          accessKey = key-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=accountId"));
    }

    @Test
    void throwsOnMissingAccessKey() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=accessKey"));
    }

    @Test
    void throwsOnWrongTypeForAccessKey() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = [key-1]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=accessKey"));
    }

    @Test
    void throwsOnWrongTypeForCaptionText() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          captionText = [some-caption]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=captionText"));
    }

    @Test
    void throwsOnWrongTypeForTimeout() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          timeout = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=timeout"));
    }

    @Test
    void throwsOnInvalidTimeout() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          timeout = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=timeout"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForInterval() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          interval = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=interval"));
    }

    @Test
    void throwsOnInvalidInterval() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          interval = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=interval"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new InstagramPublisher(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Publish {

    @Test
    void returnsPublishRefOnSuccess() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        case 1 -> "{\"status_code\": \"FINISHED\"}";
        case 2 -> "{\"id\": \"media-1\"}";
        default -> "{\"permalink\": \"https://www.instagram.com/p/abc/\"}";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      PublishRef result = assertDoesNotThrow(() -> publisher.publish(media, new CancellationToken()));

      assertEquals(CLIP, result.clip());
      assertEquals(URI.create("https://www.instagram.com/p/abc/"), result.uri());
    }

    @Test
    void throwsWhenMediaUriIsNull() {
      MediaRef media = new MediaRef(CLIP, null, null);
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      HttpSender sender = (request, token) -> "";
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Media URI missing or not HTTP(S)"));
      assertTrue(exception.getMessage().contains("mediaId=clip-1"));
    }

    @Test
    void throwsWhenMediaUriSchemeIsNotHttp() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("ftp://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      HttpSender sender = (request, token) -> "";
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Media URI missing or not HTTP(S)"));
      assertTrue(exception.getMessage().contains("mediaId=clip-1"));
    }

    @Test
    void throwsWhenApiFails() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      HttpSender sender = (request, token) -> {
        throw new ComponentException("publisher", "HTTP call failed");
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      assertThrows(ComponentException.class, () -> publisher.publish(media, new CancellationToken()));
    }

    @Test
    void throwsOnInvalidJson() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      HttpSender sender = (request, token) -> "not-valid-json";
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to parse Instagram media container id"));
    }

    @Test
    void throwsWhenContainerCreationReturnsNoId() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      HttpSender sender = (request, token) -> "{\"id\": \"\"}";
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Instagram media container creation did not return an id"));
    }

    @Test
    void throwsOnInvalidJsonDuringStatusCheck() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        default -> "not-valid-json";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to parse Instagram media container status"));
    }

    @Test
    void throwsWhenContainerStatusIsMissing() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        default -> "{}";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Instagram media container status missing status code"));
    }

    @Test
    void throwsWhenContainerStatusEntersErrorState() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        default -> "{\"status_code\": \"ERROR\", \"error_message\": \"Video processing failed\"}";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Instagram media container status entered error state"));
      assertTrue(exception.getMessage().contains("error=Video processing failed"));
    }

    @Test
    void throwsWhenContainerTimesOut() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          timeout = 1
          interval = 2
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        default -> "{\"status_code\": \"IN_PROGRESS\"}";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Timed out while waiting for Instagram media container"));
      assertTrue(exception.getMessage().contains("containerId=container-1"));
    }

    @Test
    void throwsWhenCanceledWhileAwaitingContainer() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, t) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        default -> "{\"status_code\": \"IN_PROGRESS\"}";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);
      CancellationToken token = new CancellationToken();
      Thread canceler = new Thread(() -> {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        token.cancel(CancellationReason.USER_CANCELED);
      });
      canceler.start();

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, token));

      assertTrue(exception.getMessage().contains("Canceled while waiting for Instagram media container"));
      assertTrue(exception.getMessage().contains("containerId=container-1"));
    }

    @Test
    void throwsOnInvalidJsonDuringPublish() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        case 1 -> "{\"status_code\": \"FINISHED\"}";
        default -> "not-valid-json";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to parse Instagram media id"));
    }

    @Test
    void throwsWhenPublishReturnsNoId() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        case 1 -> "{\"status_code\": \"FINISHED\"}";
        default -> "{\"id\": \"\"}";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Instagram media publish did not return an id"));
    }

    @Test
    void throwsOnInvalidJsonDuringPermalinkFetch() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        case 1 -> "{\"status_code\": \"FINISHED\"}";
        case 2 -> "{\"id\": \"media-1\"}";
        default -> "not-valid-json";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to parse Instagram media permalink"));
    }

    @Test
    void throwsWhenPermalinkIsMissing() {
      MediaRef media = new MediaRef(CLIP, null, URI.create("https://cdn.example.com/video.mp4"));
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = instagram
          accountId = account-1
          accessKey = key-1
          """);
      int[] call = { 0 };
      HttpSender sender = (request, token) -> switch (call[0]++) {
        case 0 -> "{\"id\": \"container-1\"}";
        case 1 -> "{\"status_code\": \"FINISHED\"}";
        case 2 -> "{\"id\": \"media-1\"}";
        default -> "{}";
      };
      InstagramPublisher publisher = new InstagramPublisher(config, sender);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> publisher.publish(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Instagram media permalink did not return a permalink"));
    }
  }
}
