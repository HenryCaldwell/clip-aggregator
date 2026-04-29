package info.henrycaldwell.aggregator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TextUtilsTest {

  private static final Path FONT = Path.of("src", "test", "resources", "fonts", "test.ttf");

  @Nested
  class FontSpec {

    @Test
    void throwsOnNullFontPath() {
      assertThrows(IllegalArgumentException.class, () -> new TextUtils.FontSpec(null, 12.0f));
    }

    @Test
    void throwsOnZeroFontSize() {
      assertThrows(IllegalArgumentException.class, () -> new TextUtils.FontSpec(FONT, 0.0f));
    }

    @Test
    void throwsOnNegativeFontSize() {
      assertThrows(IllegalArgumentException.class, () -> new TextUtils.FontSpec(FONT, -1.0f));
    }
  }

  @Nested
  class Wrap {

    @Test
    void throwsOnNonPositiveMaxWidth() {
      assertThrows(IllegalArgumentException.class,
          () -> TextUtils.wrap("hello", new TextUtils.FontSpec(FONT, 24.0f), 0, 2));
    }

    @Test
    void throwsOnNonPositiveMaxLines() {
      assertThrows(IllegalArgumentException.class,
          () -> TextUtils.wrap("hello", new TextUtils.FontSpec(FONT, 24.0f), 200, 0));
    }

    @Test
    void throwsOnMissingFontFile() {
      Path missing = Path.of("src", "test", "resources", "fonts", "missing.ttf");

      assertThrows(IllegalArgumentException.class,
          () -> TextUtils.wrap("hello", new TextUtils.FontSpec(missing, 24.0f), 200, 2));
    }

    @Test
    void returnsEmptyStringForNullInput() {
      String result = TextUtils.wrap(null, new TextUtils.FontSpec(FONT, 24.0f), 200, 2);

      assertEquals("", result);
    }

    @Test
    void returnsEmptyStringForBlankInput() {
      String result = TextUtils.wrap(" \n\t ", new TextUtils.FontSpec(FONT, 24.0f), 200, 2);

      assertEquals("", result);
    }

    @Test
    void normalizesWhitespace() {
      String result = TextUtils.wrap("hello\n   world\tagain", new TextUtils.FontSpec(FONT, 24.0f), 1000, 2);

      assertEquals("hello world again", result);
    }

    @Test
    void wrapsTextAcrossMultipleLines() {
      String result = TextUtils.wrap("hello world again", new TextUtils.FontSpec(FONT, 24.0f), 90, 3);

      assertTrue(result.contains("\n"));
    }

    @Test
    void breaksLongTokenAcrossMultipleLines() {
      String text = "supercalifragilistic";
      String result = TextUtils.wrap(text, new TextUtils.FontSpec(FONT, 24.0f), 80, 10);

      assertTrue(result.contains("\n"));
      assertEquals(text, result.replace("\n", ""));
    }

    @Test
    void ellipsizesAtMaxLines() {
      String result = TextUtils.wrap("alpha beta gamma delta epsilon", new TextUtils.FontSpec(FONT, 24.0f), 120, 1);

      assertFalse(result.contains("\n"));
      assertTrue(result.endsWith("..."));
    }
  }

  @Nested
  class FilterCharacters {

    @Test
    void returnsEmptyStringForNullInput() {
      assertEquals("", TextUtils.filterCharacters(null));
    }

    @Test
    void returnsEmptyStringForEmptyInput() {
      assertEquals("", TextUtils.filterCharacters(""));
    }

    @Test
    void removesSupplementaryCharacters() {
      assertEquals("abcdef", TextUtils.filterCharacters("abc\uD83D\uDE00def"));
    }

    @Test
    void keepsBasicMultilingualPlaneCharacters() {
      assertEquals("cafe", TextUtils.filterCharacters("cafe"));
      assertEquals("caf\u00e9", TextUtils.filterCharacters("caf\u00e9"));
    }
  }
}
