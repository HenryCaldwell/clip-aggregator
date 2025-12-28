package info.henrycaldwell.aggregator.util;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for text operations.
 * 
 * This class provides helpers for processing and measuring text for use in
 * rendering and layout components.
 */
public final class TextUtils {

  private TextUtils() {
  }

  /**
   * Record for specifying a font resource.
   * 
   * This record defines a contract for carrying font metadata used when measuring
   * text.
   */
  public record FontSpec(Path fontPath, float fontSize) {
    public FontSpec {
      if (fontPath == null) {
        throw new IllegalArgumentException("fontPath must be provided");
      }

      if (fontSize <= 0.0f) {
        throw new IllegalArgumentException("fontSize must be greater than 0");
      }
    }
  }

  /**
   * Wraps text into lines constrained by a maximum width and line count.
   * 
   * @param text     A string representing the text to wrap.
   * @param fontSpec A {@link FontSpec} representing the font to use.
   * @param maxWidth An integer representing the maximum line width.
   * @param maxLines An integer representing the maximum number of lines.
   * @return A string representing the wrapped text.
   * @throws IllegalArgumentException if the arguments are invalid or the font
   *                                  cannot be loaded.
   */
  public static String wrap(String text, FontSpec fontSpec, int maxWidth, int maxLines) {
    if (maxWidth <= 0) {
      throw new IllegalArgumentException("maxWidth must be greater than 0");
    }

    if (maxLines <= 0) {
      throw new IllegalArgumentException("maxLines must be greater than 0");
    }

    String normalized = normalizeWhitespace(text);
    if (normalized.isBlank()) {
      return "";
    }

    Font font = loadFont(fontSpec);
    FontRenderContext frc = new FontRenderContext(null, true, true);

    List<String> tokens = tokenizeWords(normalized);
    List<String> lines = new ArrayList<>();

    StringBuilder current = new StringBuilder();

    int i = 0;
    while (i < tokens.size()) {
      String token = tokens.get(i);

      if (token.isBlank()) {
        i++;
        continue;
      }

      if (current.length() == 0) {
        if (measurePixels(token, font, frc) <= maxWidth) {
          current.append(token);
          i++;
          continue;
        }

        List<String> pieces = breakToken(token, font, frc, maxWidth);

        for (int piece = 0; piece < pieces.size(); piece++) {
          lines.add(pieces.get(piece));

          if (lines.size() == maxLines) {
            if ((piece < pieces.size() - 1) || (i < tokens.size() - 1)) {
              int lastIndex = lines.size() - 1;
              lines.set(lastIndex, ellipsize(lines.get(lastIndex), font, frc, maxWidth));
            }

            return String.join("\n", lines);
          }
        }

        i++;
        continue;
      }

      String candidate = current + " " + token;
      if (measurePixels(candidate, font, frc) <= maxWidth) {
        current.append(' ').append(token);
        i++;
        continue;
      }

      lines.add(current.toString());
      current.setLength(0);

      if (lines.size() == maxLines) {
        int lastIdx = lines.size() - 1;
        lines.set(lastIdx, ellipsize(lines.get(lastIdx) + " " + token, font, frc, maxWidth));

        return String.join("\n", lines);
      }
    }

    if (current.length() > 0) {
      lines.add(current.toString());
    }

    return String.join("\n", lines);
  }

  /**
   * Filters out unsupported characters from a string.
   * 
   * @param text A string representing the text to filter, or {@code null}.
   * @return A string representing the filtered text.
   */
  public static String filterCharacters(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();

    text.codePoints()
        .filter(cp -> cp <= 0xFFFF)
        .forEach(sb::appendCodePoint);

    return sb.toString();
  }

  private static String normalizeWhitespace(String text) {
    if (text == null) {
      return "";
    }

    String out = text;

    out = out.replace("\r\n", "\n");
    out = out.replace("\r", "\n");
    out = out.replace("\n", " ");
    out = out.replaceAll("\\s+", " ");
    out = out.trim();

    return out;
  }

  private static Font loadFont(FontSpec spec) {
    if (spec == null) {
      throw new IllegalArgumentException("spec must be provided");
    }

    try {
      Font base = Font.createFont(Font.TRUETYPE_FONT, spec.fontPath().toFile());
      return base.deriveFont(spec.fontSize());
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to load font file");
    }
  }

  private static int measurePixels(String text, Font font, FontRenderContext frc) {
    if (text == null || text.isEmpty()) {
      return 0;
    }

    TextLayout layout = new TextLayout(text, font, frc);
    return (int) Math.ceil(layout.getBounds().getWidth());
  }

  private static List<String> tokenizeWords(String text) {
    List<String> out = new ArrayList<>();

    BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
    iterator.setText(text);

    int start = iterator.first();
    for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
      String token = text.substring(start, end);

      if (token.isBlank()) {
        continue;
      }

      if (token.chars().anyMatch(character -> Character.isLetterOrDigit(character))) {
        out.add(token);
        continue;
      } else if (out.isEmpty()) {
        out.add(token);
      } else {
        int last = out.size() - 1;
        out.set(last, out.get(last) + token);
      }
    }

    return out;
  }

  private static List<String> breakToken(String token, Font font, FontRenderContext frc, int maxWidth) {
    List<String> pieces = new ArrayList<>();
    String remaining = token;

    while (!remaining.isEmpty()) {
      int left = 0;
      int right = remaining.length();

      while (left < right) {
        int mid = (left + right + 1) / 2;

        if (measurePixels(remaining.substring(0, mid), font, frc) <= maxWidth) {
          left = mid;
        } else {
          right = mid - 1;
        }
      }

      int cut = left;
      if (cut <= 0) {
        cut = 1;
      }

      pieces.add(remaining.substring(0, cut));
      remaining = remaining.substring(cut);
    }

    return pieces;
  }

  private static String ellipsize(String text, Font font, FontRenderContext frc, int maxWidth) {
    if (text == null) {
      return "";
    }

    String trimmed = text.strip();

    if (trimmed.isEmpty()) {
      return trimmed;
    }

    if (measurePixels(trimmed, font, frc) <= maxWidth) {
      return trimmed;
    }

    if (measurePixels("...", font, frc) > maxWidth) {
      return "";
    }

    int left = 0;
    int right = trimmed.length();

    while (left < right) {
      int mid = (left + right + 1) / 2;
      String cand = trimmed.substring(0, mid).stripTrailing() + "...";

      if (measurePixels(cand, font, frc) <= maxWidth) {
        left = mid;
      } else {
        right = mid - 1;
      }
    }

    String prefix = trimmed.substring(0, left).stripTrailing();

    return prefix.isEmpty() ? "..." : prefix + "...";
  }
}
