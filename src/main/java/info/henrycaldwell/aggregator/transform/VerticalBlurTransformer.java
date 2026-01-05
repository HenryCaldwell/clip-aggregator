package info.henrycaldwell.aggregator.transform;

import java.nio.file.Path;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.SpecException;

/**
 * Class for formatting a video with a blurred vertical backdrop via the FFmpeg
 * command-line utility.
 * 
 * This class invokes FFmpeg as a subprocess and centers the clip on top of a
 * blurred background.
 */
public final class VerticalBlurTransformer extends FFmpegTransformer {

  public static final Spec SPEC = Spec.builder()
      .optionalNumber("targetWidth", "targetHeight", "blurSigma", "blurSteps")
      .build();

  private final int targetWidth;
  private final int targetHeight;
  private final double blurSigma;
  private final int blurSteps;

  /**
   * Constructs a VerticalBlurTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  public VerticalBlurTransformer(Config config) {
    super(config, SPEC);

    int targetWidth = config.hasPath("targetWidth") ? config.getNumber("targetWidth").intValue() : 1080;
    if (targetWidth <= 0) {
      throw new SpecException(name, "Invalid key value (expected targetWidth to be greater than 0)",
          Map.of("key", "targetWidth", "value", targetWidth));
    }
    this.targetWidth = targetWidth;

    int targetHeight = config.hasPath("targetHeight") ? config.getNumber("targetHeight").intValue() : 1920;
    if (targetHeight <= 0) {
      throw new SpecException(name, "Invalid key value (expected targetHeight to be greater than 0)",
          Map.of("key", "targetHeight", "value", targetHeight));
    }
    this.targetHeight = targetHeight;

    double blurSigma = config.hasPath("blurSigma") ? config.getNumber("blurSigma").doubleValue() : 40.0;
    if (blurSigma <= 0.0) {
      throw new SpecException(name, "Invalid key value (expected blurSigma to be greater than 0)",
          Map.of("key", "blurSigma", "value", blurSigma));
    }
    this.blurSigma = blurSigma;

    int blurSteps = config.hasPath("blurSteps") ? config.getNumber("blurSteps").intValue() : 2;
    if (blurSteps <= 0) {
      throw new SpecException(name, "Invalid key value (expected blurSteps to be greater than 0)",
          Map.of("key", "blurSteps", "value", blurSteps));
    }
    this.blurSteps = blurSteps;
  }

  /**
   * Applies a vertical blur transformation to the input media.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path source = media.file();
    Path target = deriveOut(source, "-temp.mp4");

    preflight(media, source, target);

    String backgroundChain = String.format(
        java.util.Locale.ROOT,
        "scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d,gblur=sigma=%.2f:steps=%d",
        targetWidth, targetHeight, targetWidth, targetHeight, blurSigma, blurSteps);
    String filterComplex = String.format(
        "[0:v]%s[bg];[0:v]scale=%d:-2[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2,format=yuv420p",
        backgroundChain, targetWidth);

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
