package info.henrycaldwell.aggregator.transform;

import java.nio.file.Path;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.util.MapUtils;
import info.henrycaldwell.aggregator.util.PathUtils;

/**
 * Class for converting a video's frame rate via the FFmpeg command-line
 * utility.
 * 
 * This class invokes FFmpeg as a subprocess and re-samples the clip to a target
 * frames-per-second value.
 */
public final class FpsTransformer extends FFmpegTransformer {

  public static final Spec SPEC = Spec.builder()
      .optionalNumber("targetFps")
      .build();

  private final int targetFps;

  /**
   * Constructs an FpsTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  public FpsTransformer(Config config) {
    super(config, SPEC);

    int targetFps = config.hasPath("targetFps") ? config.getNumber("targetFps").intValue() : 30;
    if (targetFps <= 0) {
      throw new SpecException(name, "Invalid key value (expected targetFps to be greater than 0)",
          MapUtils.ofNullable("key", "targetFps", "value", targetFps));
    }

    this.targetFps = targetFps;
  }

  /**
   * Applies a frame rate transformation to the input media.
   * 
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path source = media.file();
    Path target = PathUtils.deriveOut(source, "-temp.mp4");

    preflight(media, source, target);

    ProcessBuilder pb = new ProcessBuilder(
        ffmpegPath,
        "-y",
        "-i", source.toString(),
        "-r", Integer.toString(targetFps),
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
}
