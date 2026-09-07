package info.henrycaldwell.streamline.transform;

import java.nio.file.Path;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.NumberConstraint;
import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.PathUtils;

/**
 * Class for formatting a video with a blurred vertical backdrop via the FFmpeg
 * command-line utility.
 * 
 * This class invokes FFmpeg as a subprocess and centers the clip on top of a
 * blurred background.
 */
public final class VerticalBlurTransformer extends FFmpegTransformer {

  public static final Spec SPEC = Spec.builder()
      .optionalNumber(NumberConstraint.greaterThan(0), "targetWidth", "targetHeight", "blurSigma", "blurSteps")
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
    this(config, null);
  }

  /**
   * Constructs a VerticalBlurTransformer with a custom process factory for
   * testing.
   *
   * @param config  A {@link Config} representing the transformer configuration.
   * @param factory A {@link ProcessFactory} for creating the transformation
   *                subprocess,
   *                or {@code null} to use the default FFmpeg command.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  VerticalBlurTransformer(Config config, ProcessFactory factory) {
    super(config, factory);

    this.targetWidth = config.hasPath("targetWidth") ? config.getNumber("targetWidth").intValue() : 1080;
    this.targetHeight = config.hasPath("targetHeight") ? config.getNumber("targetHeight").intValue() : 1920;

    this.blurSigma = config.hasPath("blurSigma") ? config.getNumber("blurSigma").doubleValue() : 40.0;
    this.blurSteps = config.hasPath("blurSteps") ? config.getNumber("blurSteps").intValue() : 2;
  }

  /**
   * Applies a vertical blur transformation to the input media.
   *
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  @Override
  public MediaRef apply(MediaRef media, CancellationToken token) {
    Path source = media.file();
    Path target = PathUtils.deriveOut(source, "-temp.mp4");

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

    runProcess(pb, media, source, target, token);
    postflight(media, source, target);

    return media.withFile(target);
  }
}
