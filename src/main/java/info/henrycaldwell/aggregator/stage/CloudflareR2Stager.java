package info.henrycaldwell.aggregator.stage;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Class for staging media artifacts to Cloudflare R2.
 *
 * This class uploads the input file to Cloudflare R2 using an S3-compatible
 * client and returns a media reference with a publicly accessible URL.
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
   * @param config A {@link Config} representing the stager block.
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
   * Uploads the input file to Cloudflare R2.
   *
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the staged artifact.
   * @throws IllegalStateException    if the stager is not started.
   * @throws RuntimeException         if the upload fails.
   * @throws IllegalArgumentException if the input file is missing or not a
   *                                  regular file.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    if (s3 == null) {
      throw new IllegalStateException("Stager not started (" + name + ")");
    }

    Path src = media.file();

    if (src == null || !Files.isRegularFile(src)) {
      throw new IllegalArgumentException("Input file missing or not a regular file (path: " + src + ")");
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
      throw new RuntimeException("Failed to upload to R2 (bucket: " + bucket + ", key: " + key + ") (" + name + ")", e);
    }

    URI base = URI.create(publicUrl.endsWith("/") ? publicUrl : publicUrl + "/");
    URI uri = URI.create(base + key);
    return media.withUri(uri).withFile(null);
  }

  /**
   * Deletes the input file from Cloudflare R2.
   *
   * @param media A {@code MediaRef} representing the staged artifact.
   * @throws IllegalStateException if the stager is not started.
   * @throws RuntimeException      if the delete fails.
   */
  @Override
  public void clean(MediaRef media) {
    if (s3 == null) {
      throw new IllegalStateException("Stager not started (" + name + ")");
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
      throw new RuntimeException(
          "Failed to delete staged object from R2 (bucket: " + bucket + ", key: " + key + ") (" + name + ")", e);
    }
  }
}
