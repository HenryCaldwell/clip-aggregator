package info.henrycaldwell.aggregator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FFmpegUtilsTest {

  @Nested
  class AddOffset {

    @Test
    void returnsExpressionUnchangedWhenOffsetIsZero() {
      assertEquals("W/2", FFmpegUtils.addOffset("W/2", 0));
    }

    @Test
    void appendsPositiveOffset() {
      assertEquals("W/2+10", FFmpegUtils.addOffset("W/2", 10));
    }

    @Test
    void appendsNegativeOffset() {
      assertEquals("W/2-10", FFmpegUtils.addOffset("W/2", -10));
    }
  }

  @Nested
  class NormalizePath {

    @Test
    void replacesBackslashesWithForwardSlashes() {
      assertEquals("Users/test/clip.mp4", FFmpegUtils.normalizePath("Users\\test\\clip.mp4"));
    }

    @Test
    void escapesColons() {
      assertEquals("Users\\:/test\\:/clip.mp4", FFmpegUtils.normalizePath("Users:/test:/clip.mp4"));
    }

    @Test
    void handlesBothBackslashesAndColons() {
      assertEquals("C\\:/Users/test/clip.mp4", FFmpegUtils.normalizePath("C:\\Users\\test\\clip.mp4"));
    }

    @Test
    void returnsNullForNullInput() {
      assertNull(FFmpegUtils.normalizePath(null));
    }

    @Test
    void returnsBlankForBlankInput() {
      assertEquals("   ", FFmpegUtils.normalizePath("   "));
    }
  }

  @Nested
  class EscapeText {

    @Test
    void escapesSingleQuotes() {
      assertEquals("it\\'s a \\'test\\'", FFmpegUtils.escapeText("it's a 'test'"));
    }

    @Test
    void returnsStringUnchangedWhenNoQuotes() {
      assertEquals("hello world", FFmpegUtils.escapeText("hello world"));
    }

    @Test
    void returnsNullForNullInput() {
      assertNull(FFmpegUtils.escapeText(null));
    }
  }
}
