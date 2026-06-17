package info.henrycaldwell.streamline.core;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.stage.AwsS3Stager;
import info.henrycaldwell.streamline.stage.CloudflareR2Stager;
import info.henrycaldwell.streamline.stage.NoOpStager;
import info.henrycaldwell.streamline.stage.Stager;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing stagers from configuration.
 * 
 * This class validates a stager configuration block and instantiates a concrete
 * stager implementation.
 */
public final class StagerFactory {

  private StagerFactory() {
  }

  /**
   * Builds a stager from the given configuration block.
   *
   * @param config A {@link Config} representing the stager configuration.
   * @return A {@link Stager} representing the configured stager.
   * @throws SpecException if the configuration is invalid or the stager type is
   *                       unknown.
   */
  public static Stager fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_STAGER", "Missing required key", MapUtils.ofNullable("key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(name, "Missing required key", MapUtils.ofNullable("key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "cloudflare-r2" -> {
        return new CloudflareR2Stager(config);
      }
      case "aws-s3" -> {
        return new AwsS3Stager(config);
      }
      case "no_op" -> {
        return new NoOpStager(config);
      }
      default -> throw new SpecException(name, "Unknown stager type", MapUtils.ofNullable("type", type));
    }
  }
}
