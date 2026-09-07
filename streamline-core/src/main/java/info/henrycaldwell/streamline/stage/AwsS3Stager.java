package info.henrycaldwell.streamline.stage;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.Cancellable;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.util.MapUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Class for staging media via AWS S3 object storage.
 *
 * This class uploads the input media file using an S3-compatible client and
 * returns a media with a publicly accessible URL.
 */
public final class AwsS3Stager extends AbstractStager {

  public static final Spec SPEC = Spec.builder()
      .requiredString("accessKey", "secretKey", "bucket", "publicUrl")
      .optionalString("directory", "region")
      .build();

  private S3AsyncClient s3;
  private S3Operations operations;

  private final String accessKey;
  private final String secretKey;
  private final String bucket;
  private final String publicUrl;

  private final String directory;
  private final String region;

  /**
   * Constructs a AwsS3Stager.
   *
   * @param config A {@link Config} representing the stager configuration.
   */
  public AwsS3Stager(Config config) {
    this(config, null);
  }

  /**
   * Constructs a AwsS3Stager with a custom S3 operations for testing.
   *
   * @param config     A {@link Config} representing the stager configuration.
   * @param operations An {@link S3Operations} for dispatching requests, or
   *                   {@code null} to use the default AWS S3 client.
   */
  AwsS3Stager(Config config, S3Operations operations) {
    super(config);

    this.accessKey = config.getString("accessKey");
    this.secretKey = config.getString("secretKey");
    this.bucket = config.getString("bucket");
    this.publicUrl = config.getString("publicUrl");
    this.directory = config.hasPath("directory")
        ? config.getString("directory").strip().replaceAll("^/+|/+$", "")
        : null;
    this.region = config.hasPath("region") ? config.getString("region") : "us-east-1";
    this.operations = operations;
  }

  /**
   * Initializes an S3 client configured for AWS S3.
   */
  @Override
  public void start() {
    if (operations != null || s3 != null) {
      return;
    }

    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

    s3 = S3AsyncClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.of(region))
        .build();

    operations = new S3Operations() {
      @Override
      public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
        return s3.putObject(request, body);
      }

      @Override
      public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
        return s3.deleteObject(request);
      }

      @Override
      public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
        return s3.deleteObjects(request);
      }

      @Override
      public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
        return s3.listObjectsV2(request);
      }
    };
  }

  /**
   * Releases the S3 client acquired by {@link #start()}.
   */
  @Override
  public void stop() {
    if (s3 != null) {
      s3.close();
    }

    s3 = null;
    operations = null;
  }

  /**
   * Uploads the input media to AWS S3 and updates its remote URI.
   *
   * @param media A {@link MediaRef} representing the media to stage.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the staged media.
   * @throws ComponentException if staging fails at any step.
   */
  @Override
  public MediaRef apply(MediaRef media, CancellationToken token) {
    if (operations == null) {
      throw new ComponentException(Stager.TYPE, null, name, "Stager not started");
    }

    Path source = media.file();

    if (source == null || !Files.isRegularFile(source)) {
      throw new ComponentException(Stager.TYPE, null, name, "Input file missing or not a regular file",
          MapUtils.ofNullable("sourcePath", source));
    }

    String filename = source.getFileName().toString();
    String key = directory != null ? directory + "/" + filename : filename;

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType("video/mp4")
        .build();

    CompletableFuture<PutObjectResponse> future = operations.putObject(request, AsyncRequestBody.fromFile(source));
    Cancellable abort = () -> future.cancel(true);
    token.register(abort);

    try {
      future.get();
    } catch (CancellationException e) {
      throw new ComponentException(Stager.TYPE, null, name, "Canceled while uploading object to S3",
          MapUtils.ofNullable("bucket", bucket, "objectKey", key, "sourcePath", source), e);
    } catch (ExecutionException e) {
      throw new ComponentException(Stager.TYPE, null, name, "Failed to upload object to S3",
          MapUtils.ofNullable("bucket", bucket, "objectKey", key, "sourcePath", source), e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      throw new ComponentException(Stager.TYPE, null, name, "Interrupted while uploading object to S3",
          MapUtils.ofNullable("bucket", bucket, "objectKey", key, "sourcePath", source), e);
    } finally {
      token.unregister(abort);
    }

    URI base = URI.create(publicUrl.endsWith("/") ? publicUrl : publicUrl + "/");
    URI uri = URI.create(base + key);
    return media.withUri(uri).withFile(null);
  }

  /**
   * Deletes the staged media from AWS S3.
   *
   * @param media A {@link MediaRef} representing the staged media.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @throws ComponentException if deletion fails at any step.
   */
  @Override
  public void clean(MediaRef media, CancellationToken token) {
    if (operations == null) {
      throw new ComponentException(Stager.TYPE, null, name, "Stager not started");
    }

    URI uri = media.uri();

    if (uri == null) {
      throw new ComponentException(Stager.TYPE, null, name, "Staged media URI missing",
          MapUtils.ofNullable("clipId", media.clip().id()));
    }

    String path = uri.getPath();

    if (path == null || path.isBlank()) {
      throw new ComponentException(Stager.TYPE, null, name, "Staged media URI path missing",
          MapUtils.ofNullable("clipId", media.clip().id(), "uri", uri.toString()));
    }

    String key = path.startsWith("/") ? path.substring(1) : path;

    if (key.isBlank()) {
      throw new ComponentException(Stager.TYPE, null, name, "Staged media URI object key empty",
          MapUtils.ofNullable("clipId", media.clip().id(), "uri", uri.toString()));
    }

    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

    CompletableFuture<DeleteObjectResponse> future = operations.deleteObject(request);
    Cancellable abort = () -> future.cancel(true);
    token.register(abort);

    try {
      future.get();
    } catch (CancellationException e) {
      throw new ComponentException(Stager.TYPE, null, name, "Canceled while deleting object from S3",
          MapUtils.ofNullable("bucket", bucket, "objectKey", key, "uri", uri.toString()), e);
    } catch (ExecutionException e) {
      throw new ComponentException(Stager.TYPE, null, name, "Failed to delete object from S3",
          MapUtils.ofNullable("bucket", bucket, "objectKey", key, "uri", uri.toString()), e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      throw new ComponentException(Stager.TYPE, null, name, "Interrupted while deleting object from S3",
          MapUtils.ofNullable("bucket", bucket, "objectKey", key, "uri", uri.toString()), e);
    } finally {
      token.unregister(abort);
    }
  }

  /**
   * Deletes all staged resources from AWS S3.
   *
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @throws ComponentException if deletion fails at any step.
   */
  @Override
  public void purge(CancellationToken token) {
    if (operations == null) {
      throw new ComponentException(Stager.TYPE, null, name, "Stager not started");
    }

    String cursor = null;

    do {
      ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
          .bucket(bucket)
          .prefix(directory != null ? directory + "/" : null)
          .continuationToken(cursor)
          .build();

      CompletableFuture<ListObjectsV2Response> listFuture = operations.listObjectsV2(listRequest);
      Cancellable listAbort = () -> listFuture.cancel(true);
      token.register(listAbort);

      ListObjectsV2Response response;
      try {
        response = listFuture.get();
      } catch (CancellationException e) {
        throw new ComponentException(Stager.TYPE, null, name, "Canceled while listing objects in S3",
            MapUtils.ofNullable("bucket", bucket), e);
      } catch (ExecutionException e) {
        throw new ComponentException(Stager.TYPE, null, name, "Failed to list objects in S3",
            MapUtils.ofNullable("bucket", bucket), e.getCause());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        listFuture.cancel(true);
        throw new ComponentException(Stager.TYPE, null, name, "Interrupted while listing objects in S3",
            MapUtils.ofNullable("bucket", bucket), e);
      } finally {
        token.unregister(listAbort);
      }

      List<S3Object> objects = response.contents();

      if (!objects.isEmpty()) {
        List<ObjectIdentifier> identifiers = objects.stream()
            .map(o -> ObjectIdentifier.builder().key(o.key()).build())
            .collect(Collectors.toList());

        Delete delete = Delete.builder().objects(identifiers).build();
        DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(delete)
            .build();

        CompletableFuture<DeleteObjectsResponse> deleteFuture = operations.deleteObjects(deleteRequest);
        Cancellable deleteAbort = () -> deleteFuture.cancel(true);
        token.register(deleteAbort);

        try {
          deleteFuture.get();
        } catch (CancellationException e) {
          throw new ComponentException(Stager.TYPE, null, name, "Canceled while deleting objects from S3",
              MapUtils.ofNullable("bucket", bucket), e);
        } catch (ExecutionException e) {
          throw new ComponentException(Stager.TYPE, null, name, "Failed to delete objects from S3",
              MapUtils.ofNullable("bucket", bucket), e.getCause());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          deleteFuture.cancel(true);
          throw new ComponentException(Stager.TYPE, null, name, "Interrupted while deleting objects from S3",
              MapUtils.ofNullable("bucket", bucket), e);
        } finally {
          token.unregister(deleteAbort);
        }
      }

      cursor = response.nextContinuationToken();
    } while (cursor != null);
  }
}
