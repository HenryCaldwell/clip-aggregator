package info.henrycaldwell.aggregator.transform;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Class for formatting a video with a blurred vertical backdrop.
 * 
 * This class creates a 9:16 output by generating a blurred background from the
 * source and centering the original clip on top.
 */
public final class VerticalBlurTransformer extends AbstractTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("ffmpegPath")
      .optionalNumber("targetWidth", "targetHeight", "blurSigma", "blurSteps")
      .build();

  private final String ffmpegPath;
  private final int targetWidth;
  private final int targetHeight;
  private final double blurSigma;
  private final int blurSteps;

  /**
   * Constructs a VerticalBlurTransformer.
   *
   * @param config A {@link Config} representing the transformer block.
   */
  public VerticalBlurTransformer(Config config) {
    super(config, SPEC);

    this.ffmpegPath = config.getString("ffmpegPath");
    this.targetWidth = config.hasPath("targetWidth") ? config.getNumber("targetWidth").intValue() : 1080;
    this.targetHeight = config.hasPath("targetHeight") ? config.getNumber("targetHeight").intValue() : 1920;
    this.blurSigma = config.hasPath("blurSigma") ? config.getNumber("blurSigma").doubleValue() : 40.0;
    this.blurSteps = config.hasPath("blurSteps") ? config.getNumber("blurSteps").intValue() : 2;
  }

  /**
   * Applies a blurred background vertical layout with a centered foreground.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   * @throws RuntimeException         if the transform fails at any step.
   * @throws IllegalArgumentException if the input file is missing or not a
   *                                  regular file.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path src = media.file();

    if (src == null || !Files.isRegularFile(src)) {
      throw new IllegalArgumentException("Input file missing or not a regular file (path: " + src + ")");
    }

    Path target = deriveOut(src, "-vertical-blur.mp4");
    Path parent = target.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create parent directories (target: " + target + ")", e);
      }
    }

    if (Files.exists(target)) {
      throw new IllegalStateException("Target already exists (target: " + target + ")");
    }

    String backgroundChain = String.format(
        java.util.Locale.ROOT,
        "scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d,gblur=sigma=%.2f:steps=%d",
        targetWidth, targetHeight, targetWidth, targetHeight, blurSigma, blurSteps);
    String filterComplex = String.format(
        "[0:v]%s[bg];[0:v]scale=%d:-2[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2,format=yuv420p",
        backgroundChain, targetWidth);

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
      throw new RuntimeException("Failed to start ffmpeg (path: " + ffmpegPath + ")", e);
    }

    boolean complete;
    try {
      complete = process.waitFor(2, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new RuntimeException("Interrupted while waiting for ffmpeg (clip: " + media.id() + ")", e);
    }

    if (!complete) {
      process.destroyForcibly();
      throw new RuntimeException("Timed out waiting for ffmpeg (clip: " + media.id() + ")");
    }

    int code = process.exitValue();
    if (code != 0) {
      throw new RuntimeException("Exited ffmpeg with non-zero code " + code + " (clip: " + media.id() + ")");
    }

    if (!Files.exists(target)) {
      throw new RuntimeException("Output file missing (path: " + target + ")");
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new RuntimeException("Output file empty (path: " + target + ")");
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to stat output file (path: " + target + ")");
    }

    return media.withFile(target);
  }

  /**
   * Derives an output path by appending a suffix before the original file
   * extension.
   *
   * @param in     A path representing the input file.
   * @param suffix A string representing the suffix to append to the base
   *               filename.
   * @return A path representing the derived sibling file.
   */
  private static Path deriveOut(Path in, String suffix) {
    String name = in.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String base = (dot > 0) ? name.substring(0, dot) : name;

    return in.resolveSibling(base + suffix);
  }
}
