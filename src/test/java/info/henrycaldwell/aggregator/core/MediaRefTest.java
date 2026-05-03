package info.henrycaldwell.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MediaRefTest {

  private static final MediaRef MEDIA = new MediaRef(
      "clip-1",
      Path.of("input.mp4"),
      URI.create("https://example.com/input.mp4"),
      "Title",
      "Broadcaster",
      "en",
      List.of("funny", "gaming"));

  @Nested
  class WithFile {

    @Test
    void returnsMediaRefWithUpdatedFile() {
      Path file = Path.of("output.mp4");

      MediaRef result = MEDIA.withFile(file);

      assertNotSame(MEDIA, result);
      assertEquals(MEDIA.id(), result.id());
      assertEquals(file, result.file());
      assertEquals(MEDIA.uri(), result.uri());
      assertEquals(MEDIA.title(), result.title());
      assertEquals(MEDIA.broadcaster(), result.broadcaster());
      assertEquals(MEDIA.language(), result.language());
      assertEquals(MEDIA.tags(), result.tags());
    }
  }

  @Nested
  class WithUri {

    @Test
    void returnsMediaRefWithUpdatedUri() {
      URI uri = URI.create("https://example.com/output.mp4");

      MediaRef result = MEDIA.withUri(uri);

      assertNotSame(MEDIA, result);
      assertEquals(MEDIA.id(), result.id());
      assertEquals(MEDIA.file(), result.file());
      assertEquals(uri, result.uri());
      assertEquals(MEDIA.title(), result.title());
      assertEquals(MEDIA.broadcaster(), result.broadcaster());
      assertEquals(MEDIA.language(), result.language());
      assertEquals(MEDIA.tags(), result.tags());
    }
  }

  @Nested
  class WithTags {

    @Test
    void returnsMediaRefWithUpdatedTags() {
      List<String> tags = List.of("highlight", "clip");

      MediaRef result = MEDIA.withTags(tags);

      assertNotSame(MEDIA, result);
      assertEquals(MEDIA.id(), result.id());
      assertEquals(MEDIA.file(), result.file());
      assertEquals(MEDIA.uri(), result.uri());
      assertEquals(MEDIA.title(), result.title());
      assertEquals(MEDIA.broadcaster(), result.broadcaster());
      assertEquals(MEDIA.language(), result.language());
      assertEquals(tags, result.tags());
    }
  }
}
