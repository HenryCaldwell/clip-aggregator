package info.henrycaldwell.streamline.transform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.NumberConstraint;
import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.config.StringConstraint;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.FFmpegUtils;
import info.henrycaldwell.streamline.util.MapUtils;
import info.henrycaldwell.streamline.util.PathUtils;
import info.henrycaldwell.streamline.util.TextUtils;

/**
 * Class for overlaying a watermark onto a video via the FFmpeg command-line
 * utility.
 * 
 * This class invokes FFmpeg as a subprocess and overlays a watermark on the
 * clip consisting of the clip's broadcaster and an optional logo.
 */
public final class WatermarkTransformer extends FFmpegTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("fontPath")
      .optionalString(StringConstraint.oneOf(List.of("upper_center", "lower_center", "center")), "position")
      .optionalString("logoPath", "fontColor", "borderColor")
      .optionalNumber(NumberConstraint.greaterThan(0), "fontSize", "logoHeight")
      .optionalNumber(NumberConstraint.between(0, 1), "textOpacity", "logoOpacity")
      .optionalNumber(NumberConstraint.atLeast(0), "textBorderWidth")
      .optionalNumber("textOffsetX", "textOffsetY", "logoOffsetX", "logoOffsetY")
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

  private final String fontPath;
  private final String logoPath;

  private final String position;

  private final String fontColor;
  private final String borderColor;

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
    this(config, null);
  }

  /**
   * Constructs a WatermarkTransformer with a custom process factory for testing.
   *
   * @param config  A {@link Config} representing the transformer configuration.
   * @param factory A {@link ProcessFactory} for creating the transformation
   *                subprocess,
   *                or {@code null} to use the default FFmpeg command.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  WatermarkTransformer(Config config, ProcessFactory factory) {
    super(config, factory);

    this.fontPath = config.getString("fontPath");
    this.logoPath = config.hasPath("logoPath") ? config.getString("logoPath") : null;

    this.position = config.hasPath("position") ? config.getString("position") : "lower_center";

    this.fontColor = config.hasPath("fontColor") ? config.getString("fontColor") : "white";
    this.borderColor = config.hasPath("borderColor") ? config.getString("borderColor") : "black";

    this.fontSize = config.hasPath("fontSize") ? config.getNumber("fontSize").intValue() : 70;
    this.textOpacity = config.hasPath("textOpacity") ? config.getNumber("textOpacity").doubleValue() : 0.75;
    this.textBorderWidth = config.hasPath("textBorderWidth") ? config.getNumber("textBorderWidth").intValue() : 3;
    this.textOffsetX = config.hasPath("textOffsetX") ? config.getNumber("textOffsetX").intValue() : 0;
    this.textOffsetY = config.hasPath("textOffsetY") ? config.getNumber("textOffsetY").intValue() : 0;
    this.logoHeight = config.hasPath("logoHeight") ? config.getNumber("logoHeight").intValue() : 200;
    this.logoOpacity = config.hasPath("logoOpacity") ? config.getNumber("logoOpacity").doubleValue() : 0.3;
    this.logoOffsetX = config.hasPath("logoOffsetX") ? config.getNumber("logoOffsetX").intValue() : 0;
    this.logoOffsetY = config.hasPath("logoOffsetY") ? config.getNumber("logoOffsetY").intValue() : 0;
  }

  /**
   * Applies a watermark transformation to the input media.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   * @throws ComponentException if transforming fails at any step.
   */
  @Override
  public MediaRef apply(MediaRef media, CancellationToken token) {
    Path source = media.file();
    Path target = PathUtils.deriveOut(source, "-temp.mp4");

    preflight(media, source, target);

    String rawBroadcaster = media.clip().broadcaster();
    if (rawBroadcaster == null || rawBroadcaster.isBlank()) {
      throw new ComponentException(Transformer.TYPE, null, name, "Broadcaster name missing",
          MapUtils.ofNullable("clipId", media.clip().id(), "broadcaster", rawBroadcaster));
    }

    String broadcaster = TextUtils.filterCharacters(rawBroadcaster);
    if (broadcaster.isBlank()) {
      throw new ComponentException(Transformer.TYPE, null, name, "Broadcaster name empty after filtering",
          MapUtils.ofNullable("clipId", media.clip().id(), "broadcaster", rawBroadcaster));
    }

    if (logoPath != null && !Files.isRegularFile(Path.of(logoPath))) {
      throw new ComponentException(Transformer.TYPE, null, name, "Logo file missing or not a regular file",
          MapUtils.ofNullable("clipId", media.clip().id(), "logoPath", logoPath));
    }

    Path broadcasterFile = null;
    try {
      Path directory = target.toAbsolutePath().getParent();
      if (directory == null) {
        throw new ComponentException(Transformer.TYPE, null, name,
            "Failed to determine broadcaster label temporary directory",
            MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source, "targetPath", target));
      }

      broadcasterFile = Files.createTempFile(directory, "broadcaster-", ".txt");
      Files.writeString(broadcasterFile, broadcaster, StandardCharsets.UTF_8);

      String filterComplex = buildFilter(broadcasterFile);

      ProcessBuilder pb;
      if (logoPath != null) {
        pb = new ProcessBuilder(ffmpegPath, "-y",
            "-i", source.toString(),
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
            "-i", source.toString(),
            "-filter_complex", filterComplex,
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            "-b:a", "128k",
            "-ar", "48000",
            target.toString());
      }

      runProcess(pb, media, source, target, token);
      postflight(media, source, target);

      return media.withFile(target);
    } catch (IOException e) {
      throw new ComponentException(Transformer.TYPE, null, name, "Failed to write broadcaster label temp file",
          MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source, "targetPath", target), e);
    } finally {
      if (broadcasterFile != null) {
        try {
          Files.deleteIfExists(broadcasterFile);
        } catch (IOException ignored) {
        }
      }
    }
  }

  /**
   * Builds the FFmpeg filter expression.
   *
   * @param broadcaster A {@link Path} representing the broadcaster label file.
   * @return A string representing the FFmpeg filter expression.
   */
  private String buildFilter(Path broadcaster) {
    PositionExpr textPos = TEXT_POS.get(position);
    String xExpr = FFmpegUtils.addOffset(textPos.x(), textOffsetX);
    String yExpr = FFmpegUtils.addOffset(textPos.y(), textOffsetY);

    String font = FFmpegUtils.normalizePath(fontPath);
    String textFile = FFmpegUtils.escapeText(FFmpegUtils.normalizePath(broadcaster.toString()));

    if (logoPath == null) {
      return new StringBuilder()
          .append("[0:v]drawtext=")
          .append("fontfile='").append(font).append("':")
          .append("textfile='").append(textFile).append("':")
          .append("reload=0:")
          .append("fontsize=").append(fontSize).append(":")
          .append("fontcolor=").append(fontColor).append("@").append(textOpacity).append(":")
          .append("borderw=").append(textBorderWidth).append(":")
          .append("bordercolor=").append(borderColor).append("@").append(textOpacity).append(":")
          .append("x=").append(xExpr).append(":")
          .append("y=").append(yExpr)
          .toString();
    }

    PositionExpr logoPos = LOGO_POS.get(position);
    String logoX = FFmpegUtils.addOffset(logoPos.x(), logoOffsetX);
    String logoY = FFmpegUtils.addOffset(logoPos.y(), logoOffsetY);

    return new StringBuilder()
        .append("[1:v]format=rgba,scale=-1:").append(logoHeight)
        .append(",colorchannelmixer=aa=").append(logoOpacity)
        .append("[logo];")
        .append("[0:v][logo]overlay=")
        .append(logoX).append(":").append(logoY)
        .append(":format=auto[v1];")
        .append("[v1]drawtext=")
        .append("fontfile='").append(font).append("':")
        .append("textfile='").append(textFile).append("':")
        .append("reload=0:")
        .append("fontsize=").append(fontSize).append(":")
        .append("fontcolor=").append(fontColor).append("@").append(textOpacity).append(":")
        .append("borderw=").append(textBorderWidth).append(":")
        .append("bordercolor=").append(borderColor).append("@").append(textOpacity).append(":")
        .append("x=").append(xExpr).append(":")
        .append("y=").append(yExpr)
        .toString();
  }
}
