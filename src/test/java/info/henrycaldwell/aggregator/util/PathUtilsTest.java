package info.henrycaldwell.aggregator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PathUtilsTest {

  @Nested
  class DeriveOut {

    @Test
    void appendsSuffixBeforeExtension() {
      Path input = Path.of("work", "clip.mp4");
      Path result = PathUtils.deriveOut(input, "_out.mp4");

      assertEquals(Path.of("work", "clip_out.mp4"), result);
    }

    @Test
    void handlesFileWithNoExtension() {
      Path input = Path.of("work", "clip");
      Path result = PathUtils.deriveOut(input, "_out");

      assertEquals(Path.of("work", "clip_out"), result);
    }

    @Test
    void handlesDotFileWithNoExtension() {
      Path input = Path.of("work", ".clip");
      Path result = PathUtils.deriveOut(input, "_out");

      assertEquals(Path.of("work", ".clip_out"), result);
    }

    @Test
    void preservesSiblingDirectory() {
      Path input = Path.of("some", "nested", "dir", "clip.mp4");
      Path result = PathUtils.deriveOut(input, "_out.mp4");

      assertEquals(Path.of("some", "nested", "dir", "clip_out.mp4"), result);
    }
  }
}
