package info.henrycaldwell.aggregator.transform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;
import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.util.TextUtils;
import info.henrycaldwell.aggregator.util.TextUtils.FontSpec;

/**
 * Class for overlaying arbitrary text onto a video via the FFmpeg command-line
 * utility.
 *
 * This class invokes FFmpeg as a subprocess and overlays configured text on the
 * clip.
 */
public final class TextTransformer extends FFmpegTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("fontPath", "text")
      .optionalString("position", "textAlign", "fontColor", "borderColor", "boxColor")
      .requiredNumber("targetWidth")
      .optionalNumber("fontSize", "textOpacity", "textBorderWidth", "textOffsetX", "textOffsetY", "lineSpacing",
          "maxLines", "boxOpacity", "boxBorderWidth")
      .build();

  private record PositionExpr(String x, String y) {
  }

  private static final Map<String, PositionExpr> POS = Map.of(
      "top_left", new PositionExpr("0", "0"),
      "top_right", new PositionExpr("w-text_w", "0"),
      "bottom_left", new PositionExpr("0", "h-text_h"),
      "bottom_right", new PositionExpr("w-text_w", "h-text_h"),
      "top_center", new PositionExpr("(w-text_w)/2", "0"),
      "bottom_center", new PositionExpr("(w-text_w)/2", "h-text_h"),
      "center", new PositionExpr("(w-text_w)/2", "(h-text_h)/2"));

  private final String fontPath;
  private final String text;

  private final String position;
  private final String textAlign;
  private final String fontColor;
  private final String borderColor;
  private final String boxColor;

  private final int targetWidth;

  private final int fontSize;
  private final double textOpacity;
  private final int textBorderWidth;
  private final int textOffsetX;
  private final int textOffsetY;
  private final int lineSpacing;
  private final int maxLines;
  private final double boxOpacity;
  private final int boxBorderWidth;

  /**
   * Constructs a TextTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  public TextTransformer(Config config) {
    super(config, SPEC);

    this.fontPath = config.getString("fontPath");

    String text = config.getString("text");
    if (text.isBlank()) {
      throw new SpecException(name, "Invalid key value (expected text to be a non-blank string)",
          Map.of("key", "text", "value", text));
    }
    this.text = text;

    String position = config.hasPath("position") ? config.getString("position") : "center";
    if (!POS.containsKey(position)) {
      throw new SpecException(name,
          "Invalid key value (expected position to be one of top_left, top_right, bottom_left, bottom_right, top_center, bottom_center, center)",
          Map.of("key", "position", "value", position));
    }
    this.position = position;

    String rawAlign = config.hasPath("textAlign") ? config.getString("textAlign") : "center";
    String textAlign;
    switch (rawAlign) {
      case "left":
        textAlign = "L";
        break;
      case "center":
        textAlign = "C";
        break;
      case "right":
        textAlign = "R";
        break;
      default:
        throw new SpecException(name, "Invalid key value (expected textAlign to be one of left, center, right)",
            Map.of("key", "textAlign", "value", rawAlign));
    }
    this.textAlign = textAlign;

    this.fontColor = config.hasPath("fontColor") ? config.getString("fontColor") : "white";
    this.borderColor = config.hasPath("borderColor") ? config.getString("borderColor") : "black";
    this.boxColor = config.hasPath("boxColor") ? config.getString("boxColor") : "black";

    int targetWidth = config.getNumber("targetWidth").intValue();
    if (targetWidth <= 0) {
      throw new SpecException(name, "Invalid key value (expected targetWidth to be greater than 0)",
          Map.of("key", "targetWidth", "value", targetWidth));
    }
    this.targetWidth = targetWidth;

    int fontSize = config.hasPath("fontSize") ? config.getNumber("fontSize").intValue() : 70;
    if (fontSize <= 0) {
      throw new SpecException(name, "Invalid key value (expected fontSize to be greater than 0)",
          Map.of("key", "fontSize", "value", fontSize));
    }
    this.fontSize = fontSize;

    double textOpacity = config.hasPath("textOpacity") ? config.getNumber("textOpacity").doubleValue() : 0.75;
    if (textOpacity < 0.0 || textOpacity > 1.0) {
      throw new SpecException(name, "Invalid key value (expected textOpacity to be between 0.0 and 1.0)",
          Map.of("key", "textOpacity", "value", textOpacity));
    }
    this.textOpacity = textOpacity;

    int textBorderWidth = config.hasPath("textBorderWidth") ? config.getNumber("textBorderWidth").intValue() : 3;
    if (textBorderWidth < 0) {
      throw new SpecException(name, "Invalid key value (expected textBorderWidth to be greater than or equal to 0)",
          Map.of("key", "textBorderWidth", "value", textBorderWidth));
    }
    this.textBorderWidth = textBorderWidth;

    this.textOffsetX = config.hasPath("textOffsetX") ? config.getNumber("textOffsetX").intValue() : 0;
    this.textOffsetY = config.hasPath("textOffsetY") ? config.getNumber("textOffsetY").intValue() : 0;
    this.lineSpacing = config.hasPath("lineSpacing") ? config.getNumber("lineSpacing").intValue() : 10;

    int maxLines = config.hasPath("maxLines") ? config.getNumber("maxLines").intValue() : 4;
    if (maxLines <= 0) {
      throw new SpecException(name, "Invalid key value (expected maxLines to be greater than 0)",
          Map.of("key", "maxLines", "value", maxLines));
    }
    this.maxLines = maxLines;

    double boxOpacity = config.hasPath("boxOpacity") ? config.getNumber("boxOpacity").doubleValue() : 0.0;
    if (boxOpacity < 0.0 || boxOpacity > 1.0) {
      throw new SpecException(name, "Invalid key value (expected boxOpacity to be between 0.0 and 1.0)",
          Map.of("key", "boxOpacity", "value", boxOpacity));
    }
    this.boxOpacity = boxOpacity;

    int boxBorderWidth = config.hasPath("boxBorderWidth") ? config.getNumber("boxBorderWidth").intValue() : 0;
    if (boxBorderWidth < 0) {
      throw new SpecException(name, "Invalid key value (expected boxBorderWidth to be greater than or equal to 0)",
          Map.of("key", "boxBorderWidth", "value", boxBorderWidth));
    }
    this.boxBorderWidth = boxBorderWidth;
  }

  /**
   * Applies a text transformation to the input media.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   * @throws ComponentException if transforming fails at any step.
   */
  @Override
  protected MediaRef apply(MediaRef media) {
    Path source = media.file();
    Path target = deriveOut(source, "-temp.mp4");

    preflight(media, source, target);

    String safeText = TextUtils.filterCharacters(text);

    String caption = TextUtils.wrap(safeText, new FontSpec(Paths.get(fontPath), (float) fontSize), targetWidth,
        maxLines);
    if (caption.isBlank()) {
      throw new ComponentException(name, "Text empty after formatting",
          Map.of("clipId", media.id(), "text", text, "formattedText", caption));
    }

    Path captionFile = null;
    try {
      Path directory = target.toAbsolutePath().getParent();
      if (directory == null) {
        throw new ComponentException(name, "Failed to determine caption temporary directory",
            Map.of("clipId", media.id(), "sourcePath", source, "targetPath", target));
      }

      captionFile = Files.createTempFile(directory, "caption-", ".txt");
      Files.writeString(captionFile, caption, StandardCharsets.UTF_8);

      String filterComplex = buildFilter(captionFile);

      ProcessBuilder pb = new ProcessBuilder(
          ffmpegPath,
          "-y",
          "-i", source.toString(),
          "-filter_complex", filterComplex,
          "-c:v", "libx264",
          "-pix_fmt", "yuv420p",
          "-c:a", "aac",
          "-b:a", "128k",
          "-ar", "48000",
          target.toString());

      runProcess(pb, media, source, target);
      postflight(media, source, target);

      return media.withFile(target);
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to write caption temp file",
          Map.of("clipId", media.id(), "sourcePath", source, "targetPath", target), e);
    } finally {
      if (captionFile != null) {
        try {
          Files.deleteIfExists(captionFile);
        } catch (IOException ignored) {
        }
      }
    }
  }

  /**
   * Builds the FFmpeg filter expression.
   *
   * @param caption A {@link Path} representing the caption file.
   * @return A string representing the FFmpeg filter expression.
   */
  private String buildFilter(Path caption) {
    PositionExpr pos = POS.get(position);
    String xExpr = addOffset(pos.x(), textOffsetX);
    String yExpr = addOffset(pos.y(), textOffsetY);

    String font = normalizePath(fontPath);
    String textFile = escapeText(normalizePath(caption.toString()));

    String textAlignExpr = "M+" + textAlign;

    StringBuilder sb = new StringBuilder()
        .append("[0:v]drawtext=")
        .append("fontfile='").append(font).append("':")
        .append("textfile='").append(textFile).append("':")
        .append("reload=0:")
        .append("text_align=").append(textAlignExpr).append(":")
        .append("line_spacing=").append(lineSpacing).append(":")
        .append("fontsize=").append(fontSize).append(":")
        .append("fontcolor=").append(fontColor).append("@").append(textOpacity).append(":")
        .append("borderw=").append(textBorderWidth).append(":")
        .append("bordercolor=").append(borderColor).append(":");

    if (boxOpacity > 0.0) {
      sb.append("box=1:")
          .append("boxcolor=").append(boxColor).append("@").append(boxOpacity).append(":")
          .append("boxborderw=").append(boxBorderWidth).append(":");
    }

    sb.append("x=").append(xExpr).append(":")
        .append("y=").append(yExpr).append(":")
        .append("fix_bounds=1");

    return sb.toString();
  }

  /**
   * Adds an integer pixel offset to an FFmpeg position expression.
   *
   * @param expr   A string representing the base FFmpeg expression.
   * @param offset An integer representing the pixel offset.
   * @return A string representing the updated FFmpeg expression.
   */
  private static String addOffset(String expr, int offset) {
    if (offset == 0)
      return expr;
    return expr + (offset > 0 ? "+" : "") + offset;
  }

  /**
   * Normalizes a path for safe use in an FFmpeg filter argument.
   *
   * @param path A string representing the path to normalize.
   * @return A string representing the normalized path.
   */
  private static String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return path;
    }

    return path.replace("\\", "/").replace(":", "\\:");
  }

  /**
   * Escapes a string for safe use in an FFmpeg filter argument.
   *
   * @param text A string representing the text to escape.
   * @return A string representing the escaped text.
   */
  private static String escapeText(String text) {
    if (text == null) {
      return null;
    }

    return text.replace("'", "\\'");
  }

  /**
   * Derives an output path by appending a suffix before the original file
   * extension.
   *
   * @param in     A {@link Path} representing the input file.
   * @param suffix A string representing the suffix to append to the base
   *               filename.
   * @return A {@link Path} representing the derived sibling file.
   */
  private static Path deriveOut(Path in, String suffix) {
    String name = in.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String base = (dot > 0) ? name.substring(0, dot) : name;

    return in.resolveSibling(base + suffix);
  }
}
