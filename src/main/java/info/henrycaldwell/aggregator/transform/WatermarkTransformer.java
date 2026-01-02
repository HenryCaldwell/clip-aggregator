package info.henrycaldwell.aggregator.transform;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;
import info.henrycaldwell.aggregator.error.SpecException;

/**
 * Class for overlaying a watermark onto a video via the FFmpeg command-line
 * utility.
 * 
 * This class invokes FFmpeg as a subprocess and overlays a watermark on the
 * clip consisting of the clip's broadcaster and an optional logo.
 */
public final class WatermarkTransformer extends AbstractTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("ffmpegPath", "fontPath")
      .optionalString("logoPath", "position")
      .optionalNumber("fontSize", "textOpacity", "textBorderWidth", "textOffsetX", "textOffsetY",
          "logoHeight", "logoOpacity", "logoOffsetX", "logoOffsetY")
      .build();

  private record PositionExpr(String x, String y) {
  }

  private static final Map<String, PositionExpr> TEXT_POS = Map.of(
      "upper_center", new PositionExpr("(w-text_w)/2", "h/4-text_h/2"),
      "lower_center", new PositionExpr("(w-text_w)/2", "3*h/4-text_h/2"),
      "center", new PositionExpr("(w-text_w)/2", "(h-text_h)/2"));

  private static final Map<String, PositionExpr> LOGO_POS = Map.of(
      "upper_center", new PositionExpr("(W-overlay_w)/2", "H/4-overlay_h/2"),
      "lower_center", new PositionExpr("(W-overlay_w)/2", "3*H/4-overlay_h/2"),
      "center", new PositionExpr("(W-overlay_w)/2", "(H-overlay_h)/2"));

  private final String ffmpegPath;
  private final String fontPath;

  private final String logoPath;
  private final String position;

  private final int fontSize;
  private final double textOpacity;
  private final int textBorderWidth;
  private final int textOffsetX;
  private final int textOffsetY;
  private final int logoHeight;
  private final double logoOpacity;
  private final int logoOffsetX;
  private final int logoOffsetY;

  /**
   * Constructs a WatermarkTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  public WatermarkTransformer(Config config) {
    super(config, SPEC);

    this.ffmpegPath = config.getString("ffmpegPath");
    this.fontPath = config.getString("fontPath");
    this.logoPath = config.hasPath("logoPath") ? config.getString("logoPath") : null;

    String position = config.hasPath("position") ? config.getString("position") : "lower_center";
    if (!TEXT_POS.containsKey(position)) {
      throw new SpecException(name,
          "Invalid key value (expected position to be one of upper_center, lower_center, center)",
          Map.of("key", "position", "value", position));
    }
    this.position = position;

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

    int logoHeight = config.hasPath("logoHeight") ? config.getNumber("logoHeight").intValue() : 200;
    if (logoHeight <= 0) {
      throw new SpecException(name, "Invalid key value (expected logoHeight to be greater than 0)",
          Map.of("key", "logoHeight", "value", logoHeight));
    }
    this.logoHeight = logoHeight;

    double logoOpacity = config.hasPath("logoOpacity") ? config.getNumber("logoOpacity").doubleValue() : 0.3;
    if (logoOpacity < 0.0 || logoOpacity > 1.0) {
      throw new SpecException(name, "Invalid key value (expected logoOpacity to be between 0.0 and 1.0)",
          Map.of("key", "logoOpacity", "value", logoOpacity));
    }
    this.logoOpacity = logoOpacity;

    this.logoOffsetX = config.hasPath("logoOffsetX") ? config.getNumber("logoOffsetX").intValue() : 0;
    this.logoOffsetY = config.hasPath("logoOffsetY") ? config.getNumber("logoOffsetY").intValue() : 0;
  }

  /**
   * Applies a watermark transformation to the input media.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   * @throws ComponentException if transforming fails at any step.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path src = media.file();

    if (src == null || !Files.isRegularFile(src)) {
      throw new ComponentException(name, "Input file missing or not a regular file", Map.of("sourcePath", src));
    }

    Path target = deriveOut(src, "-watermark.mp4");
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

    String broadcaster = media.broadcaster();
    if (broadcaster == null || broadcaster.isBlank()) {
      throw new ComponentException(name, "Broadcaster name missing",
          Map.of("clipId", media.id(), "broadcaster", broadcaster));
    }

    if (logoPath != null && !Files.isRegularFile(Path.of(logoPath))) {
      throw new ComponentException(name, "Logo file missing or not a regular file", Map.of("logoPath", logoPath));
    }

    String filterComplex = buildFilter(broadcaster);

    Process process;
    try {
      ProcessBuilder pb;
      if (logoPath != null) {
        pb = new ProcessBuilder(ffmpegPath, "-y",
            "-i", src.toString(),
            "-i", logoPath,
            "-filter_complex", filterComplex,
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            "-b:a", "128k",
            "-ar", "48000",
            target.toString());
      } else {
        pb = new ProcessBuilder(ffmpegPath, "-y",
            "-i", src.toString(),
            "-filter_complex", filterComplex,
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            "-b:a", "128k",
            "-ar", "48000",
            target.toString());
      }

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
  }

  /**
   * Builds the FFmpeg filter expression.
   *
   * @param broadcaster A string representing the broadcaster label.
   * @return A string representing the FFmpeg filter expression.
   */
  private String buildFilter(String broadcaster) {
    PositionExpr textPos = TEXT_POS.get(position);
    String xExpr = addOffset(textPos.x(), textOffsetX);
    String yExpr = addOffset(textPos.y(), textOffsetY);

    String font = normalizePath(fontPath);
    String text = escapeText(broadcaster);

    if (logoPath == null) {
      return new StringBuilder()
          .append("[0:v]drawtext=")
          .append("fontfile='").append(font).append("':")
          .append("text='").append(text).append("':")
          .append("fontsize=").append(fontSize).append(":")
          .append("fontcolor=white@").append(textOpacity).append(":")
          .append("borderw=").append(textBorderWidth).append(":")
          .append("bordercolor=black:")
          .append("x=").append(xExpr).append(":")
          .append("y=").append(yExpr)
          .toString();
    }

    PositionExpr logoPos = LOGO_POS.get(position);
    String logoX = addOffset(logoPos.x(), logoOffsetX);
    String logoY = addOffset(logoPos.y(), logoOffsetY);

    return new StringBuilder()
        .append("[1:v]format=rgba,scale=-1:").append(logoHeight)
        .append(",colorchannelmixer=aa=").append(logoOpacity)
        .append("[logo];")
        .append("[0:v][logo]overlay=")
        .append(logoX).append(":").append(logoY)
        .append(":format=auto[v1];")
        .append("[v1]drawtext=")
        .append("fontfile='").append(font).append("':")
        .append("text='").append(text).append("':")
        .append("fontsize=").append(fontSize).append(":")
        .append("fontcolor=white@").append(textOpacity).append(":")
        .append("borderw=").append(textBorderWidth).append(":")
        .append("bordercolor=black:")
        .append("x=").append(xExpr).append(":")
        .append("y=").append(yExpr)
        .toString();
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
   * Escapes a string for safe use in an FFmpeg filter argument.
   *
   * @param text A string representing the text to escape.
   * @return A string representing the escaped text.
   */
  private static String escapeText(String text) {
    if (text == null) {
      return null;
    }

    String out = text;

    out = out.replace("\\", "\\\\");
    out = out.replace("'", "\\'");
    out = out.replace("%", "\\%");
    out = out.replace("\n", "\\n");

    return out;
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
