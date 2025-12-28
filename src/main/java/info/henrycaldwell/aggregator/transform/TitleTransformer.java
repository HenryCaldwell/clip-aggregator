package info.henrycaldwell.aggregator.transform;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;
import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.util.TextUtils;
import info.henrycaldwell.aggregator.util.TextUtils.FontSpec;

/**
 * Class for overlaying a title onto a video via the FFmpeg command-line
 * utility.
 * 
 * This class invokes FFmpeg as a subprocess and overlays a caption on the clip
 * consisting of the clip's title.
 */
public final class TitleTransformer extends AbstractTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("ffmpegPath", "fontPath")
      .optionalString("position", "textAlign")
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

  private final String ffmpegPath;
  private final String fontPath;

  private final String position;
  private final String textAlign;

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
   * Constructs a TitleTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  public TitleTransformer(Config config) {
    super(config, SPEC);

    this.ffmpegPath = config.getString("ffmpegPath");
    this.fontPath = config.getString("fontPath");

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
   * Applies a title transformation to the input media.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   * @throws ComponentException if transforming fails at any step.
   */
  @Override
  protected MediaRef apply(MediaRef media) {
    Path src = media.file();

    if (src == null || !Files.isRegularFile(src)) {
      throw new ComponentException(name, "Input file missing or not a regular file", Map.of("sourcePath", src));
    }

    Path target = deriveOut(src, "-title.mp4");
    Path parent = target.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new ComponentException(name, "Failed to create parent directories",
            Map.of("targetPath", target, "parentPath", parent), e);
      }
    }

    if (Files.exists(target)) {
      throw new ComponentException(name, "Target file already exists", Map.of("targetPath", target));
    }

    String title = media.title();
    if (title == null || title.isBlank()) {
      throw new ComponentException(name, "Title missing",
          Map.of("clipId", media.id(), "title", title));
    }

    String caption = TextUtils.wrap(title, new FontSpec(Paths.get(fontPath), (float) fontSize), targetWidth, maxLines);
    if (caption.isBlank()) {
      throw new ComponentException(name, "Title empty after wrapping",
          Map.of("clipId", media.id(), "title", title));
    }

    Path captionFile = null;
    try {
      Path directory = target.toAbsolutePath().getParent();
      if (directory == null) {
        throw new ComponentException(name, "Failed to determine caption temporary directory",
            Map.of("sourcePath", src, "targetPath", target));
      }

      captionFile = Files.createTempFile(directory, "caption-", ".txt");
      Files.writeString(captionFile, caption, StandardCharsets.UTF_8);

      String filterComplex = buildFilter(captionFile);

      Process process;
      try {
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-y",
            "-i", src.toString(),
            "-filter_complex", filterComplex,
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            "-b:a", "128k",
            "-ar", "48000",
            target.toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.DISCARD);
        process = pb.start();
      } catch (IOException e) {
        throw new ComponentException(name, "Failed to start ffmpeg process",
            Map.of("ffmpegPath", ffmpegPath, "sourcePath", src, "targetPath", target), e);
      }

      boolean complete;
      try {
        complete = process.waitFor(2, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        process.destroyForcibly();
        throw new ComponentException(name, "Interrupted while waiting for ffmpeg process", Map.of("clipId", media.id()),
            e);
      }

      if (!complete) {
        process.destroyForcibly();
        throw new ComponentException(name, "Timed out while waiting for ffmpeg process", Map.of("clipId", media.id()));
      }

      int code = process.exitValue();
      if (code != 0) {
        throw new ComponentException(name, "ffmpeg process exited with non-zero code",
            Map.of("clipId", media.id(), "exitCode", code));
      }

      if (!Files.exists(target)) {
        throw new ComponentException(name, "Output file missing after transform", Map.of("targetPath", target));
      }

      try {
        long size = Files.size(target);

        if (size <= 0) {
          throw new ComponentException(name, "Output file empty after transform",
              Map.of("targetPath", target, "sizeBytes", size));
        }
      } catch (IOException e) {
        throw new ComponentException(name, "Failed to stat output file", Map.of("targetPath", target), e);
      }

      return media.withFile(target);

    } catch (IOException e) {
      throw new ComponentException(name, "Failed to write caption temp file",
          Map.of("sourcePath", src, "targetPath", target), e);
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
        .append("fontcolor=white@").append(textOpacity).append(":")
        .append("borderw=").append(textBorderWidth).append(":")
        .append("bordercolor=black:");

    if (boxOpacity > 0.0) {
      sb.append("box=1:")
          .append("boxcolor=black@").append(boxOpacity).append(":")
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
