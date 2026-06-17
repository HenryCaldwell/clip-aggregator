package info.henrycaldwell.streamline.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.net.URI;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MediaRefTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
  private static final MediaRef MEDIA = new MediaRef(CLIP, Path.of("input.mp4"),
      URI.create("https://example.com/input.mp4"));

  @Nested
  class WithFile {

    @Test
    void returnsMediaRefWithUpdatedFile() {
      Path file = Path.of("output.mp4");

      MediaRef result = MEDIA.withFile(file);

      assertNotSame(MEDIA, result);
      assertEquals(MEDIA.clip(), result.clip());
      assertEquals(file, result.file());
      assertEquals(MEDIA.uri(), result.uri());
    }
  }

  @Nested
  class WithUri {

    @Test
    void returnsMediaRefWithUpdatedUri() {
      URI uri = URI.create("https://example.com/output.mp4");

      MediaRef result = MEDIA.withUri(uri);

      assertNotSame(MEDIA, result);
      assertEquals(MEDIA.clip(), result.clip());
      assertEquals(MEDIA.file(), result.file());
      assertEquals(uri, result.uri());
    }
  }
}
