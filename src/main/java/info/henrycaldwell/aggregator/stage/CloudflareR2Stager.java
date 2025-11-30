package info.henrycaldwell.aggregator.stage;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Class for staging media via Cloudflare R2 object storage.
 *
 * This class uploads the input media file using an S3-compatible client and
 * returns a media with a publicly accessible URL.
 */
public final class CloudflareR2Stager extends AbstractStager {

  public static final Spec SPEC = Spec.builder()
      .requiredString("accountId", "accessKey", "secretKey", "bucket", "publicUrl")
      .optionalString("region", "endpoint")
      .build();

  private S3Client s3;

  private final String accountId;
  private final String accessKey;
  private final String secretKey;
  private final String bucket;
  private final String publicUrl;
  private final String region;
  private final String endpoint;

  /**
   * Constructs a CloudflareR2Stager.
   *
   * @param config A {@link Config} representing the stager configuration.
   */
  public CloudflareR2Stager(Config config) {
    super(config, SPEC);

    this.accountId = config.getString("accountId");
    this.accessKey = config.getString("accessKey");
    this.secretKey = config.getString("secretKey");
    this.bucket = config.getString("bucket");
    this.publicUrl = config.getString("publicUrl");
    this.region = config.hasPath("region") ? config.getString("region") : "auto";
    this.endpoint = config.hasPath("endpoint")
        ? config.getString("endpoint")
        : "https://" + accountId + ".r2.cloudflarestorage.com";
  }

  /**
   * Initializes an S3 client configured for Cloudflare R2.
   */
  @Override
  public void start() {
    if (s3 != null) {
      return;
    }

    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

    S3Configuration configuration = S3Configuration.builder()
        .pathStyleAccessEnabled(true)
        .chunkedEncodingEnabled(false)
        .build();

    s3 = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.of(region))
        .serviceConfiguration(configuration)
        .build();
  }

  /**
   * Releases the S3 client acquired by {@link #start()}.
   */
  @Override
  public void stop() {
    if (s3 != null) {
      s3.close();
      s3 = null;
    }
  }

  /**
   * Uploads the input media to Cloudflare R2 and updates its remote URI.
   *
   * @param media A {@link MediaRef} representing the media to stage.
   * @return A {@link MediaRef} representing the staged media.
   * @throws ComponentException if staging fails at any step.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    if (s3 == null) {
      throw new ComponentException(name, "Stager not started");
    }

    Path src = media.file();

    if (src == null || !Files.isRegularFile(src)) {
      throw new ComponentException(name, "Input file missing or not a regular file", Map.of("sourcePath", src));
    }

    String key = src.getFileName().toString();

    try {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType("video/mp4")
          .build();

      s3.putObject(request, RequestBody.fromFile(src));
    } catch (Exception e) {
      throw new ComponentException(name, "Failed to upload object to R2",
          Map.of("bucket", bucket, "objectKey", key, "sourcePath", src), e);
    }

    URI base = URI.create(publicUrl.endsWith("/") ? publicUrl : publicUrl + "/");
    URI uri = URI.create(base + key);
    return media.withUri(uri).withFile(null);
  }

  /**
   * Deletes the staged media from Cloudflare R2.
   *
   * @param media A {@link MediaRef} representing the staged media.
   * @throws ComponentException if deletion fails at any step.
   */
  @Override
  public void clean(MediaRef media) {
    if (s3 == null) {
      throw new ComponentException(name, "Stager not started");
    }

    if (media == null || media.uri() == null) {
      return;
    }

    URI uri = media.uri();
    String path = uri.getPath();

    if (path == null || path.isBlank()) {
      return;
    }

    String key = path.startsWith("/") ? path.substring(1) : path;
    if (key.isBlank()) {
      return;
    }

    try {
      DeleteObjectRequest request = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();

      s3.deleteObject(request);
    } catch (Exception e) {
      throw new ComponentException(name, "Failed to delete object from R2",
          Map.of("bucket", bucket, "objectKey", key, "uri", uri.toString()), e);
    }
  }
}
