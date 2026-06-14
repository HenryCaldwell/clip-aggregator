package info.henrycaldwell.streamline.stage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AwsS3StagerTest {

  private static final ClipRef CLIP = new ClipRef(null, null, null, null, null, 0, null);

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);

      assertDoesNotThrow(() -> new AwsS3Stager(config));
    }

    @Test
    void acceptsConfiguredDirectory() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          directory = clips
          """);

      assertDoesNotThrow(() -> new AwsS3Stager(config));
    }

    @Test
    void acceptsConfiguredRegion() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          region = us-west-2
          """);

      assertDoesNotThrow(() -> new AwsS3Stager(config));
    }

    @Test
    void throwsOnMissingAccessKey() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=accessKey"));
    }

    @Test
    void throwsOnWrongTypeForAccessKey() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = [key-1]
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=accessKey"));
    }

    @Test
    void throwsOnMissingSecretKey() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=secretKey"));
    }

    @Test
    void throwsOnWrongTypeForSecretKey() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = [secret-1]
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=secretKey"));
    }

    @Test
    void throwsOnMissingBucket() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=bucket"));
    }

    @Test
    void throwsOnWrongTypeForBucket() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = [my-bucket]
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=bucket"));
    }

    @Test
    void throwsOnMissingPublicUrl() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=publicUrl"));
    }

    @Test
    void throwsOnWrongTypeForPublicUrl() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = ["https://cdn.example.com"]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=publicUrl"));
    }

    @Test
    void throwsOnWrongTypeForDirectory() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          directory = [clips]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=directory"));
    }

    @Test
    void throwsOnWrongTypeForRegion() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          region = [us-east-1]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=region"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new AwsS3Stager(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Start {

    @Test
    void allowsApplyAfterStart() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      AwsS3Stager stager = new AwsS3Stager(config);
      stager.start();

      MediaRef media = new MediaRef(null, null, null);
      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.apply(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Input file missing or not a regular file"));
      stager.stop();
    }

    @Test
    void isIdempotentWhenStarted() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      AwsS3Stager stager = new AwsS3Stager(config);

      assertDoesNotThrow(() -> {
        stager.start();
        stager.start();
      });
      stager.stop();
    }
  }

  @Nested
  class Stop {

    @Test
    void stopsStartedStager() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      AwsS3Stager stager = new AwsS3Stager(config);
      stager.start();
      stager.stop();

      MediaRef media = new MediaRef(null, null, null);
      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.apply(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Stager not started"));
    }

    @Test
    void isIdempotentWhenNotStarted() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      AwsS3Stager stager = new AwsS3Stager(config);

      assertDoesNotThrow(() -> stager.stop());
    }
  }

  @Nested
  class Apply {

    @Test
    void returnsMediaRefWithPublicUriOnSuccess() throws IOException {
      Path source = tempDir.resolve("clip.mp4");
      Files.writeString(source, "data");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      MediaRef result = assertDoesNotThrow(() -> stager.apply(media, new CancellationToken()));

      assertEquals(URI.create("https://cdn.example.com/clip.mp4"), result.uri());
      assertNull(result.file());
    }

    @Test
    void returnsMediaRefWithDirectoryPrefixedUriOnSuccess() throws IOException {
      Path source = tempDir.resolve("clip.mp4");
      Files.writeString(source, "data");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          directory = clips
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      MediaRef result = assertDoesNotThrow(() -> stager.apply(media, new CancellationToken()));

      assertEquals(URI.create("https://cdn.example.com/clips/clip.mp4"), result.uri());
      assertNull(result.file());
    }

    @Test
    void throwsWhenNotStarted() {
      MediaRef media = new MediaRef(null, null, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      AwsS3Stager stager = new AwsS3Stager(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.apply(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Stager not started"));
    }

    @Test
    void throwsWhenSourceIsNull() {
      MediaRef media = new MediaRef(null, null, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.apply(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Input file missing or not a regular file"));
    }

    @Test
    void throwsWhenSourceIsMissing() {
      Path source = tempDir.resolve("nonexistent.mp4");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.apply(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Input file missing or not a regular file"));
      assertTrue(exception.getMessage().contains("sourcePath="));
    }

    @Test
    void throwsWhenSourceIsNotARegularFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.createDirectory(source);

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.apply(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Input file missing or not a regular file"));
      assertTrue(exception.getMessage().contains("sourcePath=" + source));
    }

    @Test
    void throwsWhenUploadFails() throws IOException {
      Path source = tempDir.resolve("clip.mp4");
      Files.writeString(source, "data");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.failedFuture(new RuntimeException("Upload failed"));
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.apply(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to upload object to S3"));
    }
  }

  @Nested
  class Clean {

    @Test
    void deletesObjectOnSuccess() {
      boolean[] deleted = { false };

      MediaRef media = new MediaRef(null, null, URI.create("https://cdn.example.com/clip.mp4"));
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          deleted[0] = true;
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      assertDoesNotThrow(() -> stager.clean(media, new CancellationToken()));

      assertTrue(deleted[0]);
    }

    @Test
    void throwsWhenUriIsNull() {
      MediaRef media = new MediaRef(CLIP, null, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.clean(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Staged media URI missing"));
    }

    @Test
    void throwsWhenUriPathIsBlank() {
      MediaRef media = new MediaRef(CLIP, null,
          URI.create("https://cdn.example.com"));
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.clean(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Staged media URI path missing"));
    }

    @Test
    void throwsWhenUriObjectKeyIsEmpty() {
      MediaRef media = new MediaRef(CLIP, null,
          URI.create("https://cdn.example.com/"));
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.clean(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Staged media URI object key empty"));
    }

    @Test
    void throwsWhenNotStarted() {
      MediaRef media = new MediaRef(null, null, URI.create("https://cdn.example.com/clip.mp4"));
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      AwsS3Stager stager = new AwsS3Stager(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.clean(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Stager not started"));
    }

    @Test
    void throwsWhenDeleteFails() {
      MediaRef media = new MediaRef(null, null, URI.create("https://cdn.example.com/clip.mp4"));
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.failedFuture(new RuntimeException("Delete failed"));
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.clean(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to delete object from S3"));
    }
  }

  @Nested
  class Purge {

    @Test
    void deletesAllObjectsOnSuccess() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      List<String> deletedKeys = new ArrayList<>();
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          request.delete().objects().forEach(o -> deletedKeys.add(o.key()));
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder()
              .contents(
                  S3Object.builder().key("clip-1.mp4").build(),
                  S3Object.builder().key("clip-2.mp4").build())
              .isTruncated(false)
              .build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      assertDoesNotThrow(() -> stager.purge(new CancellationToken()));

      assertEquals(2, deletedKeys.size());
      assertTrue(deletedKeys.contains("clip-1.mp4"));
      assertTrue(deletedKeys.contains("clip-2.mp4"));
    }

    @Test
    void deletesAllPagesOnSuccess() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      List<String> deletedKeys = new ArrayList<>();
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          request.delete().objects().forEach(o -> deletedKeys.add(o.key()));
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          if (request.continuationToken() == null) {
            return CompletableFuture.completedFuture(ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("clip-1.mp4").build())
                .isTruncated(true)
                .nextContinuationToken("token-1")
                .build());
          }

          return CompletableFuture.completedFuture(ListObjectsV2Response.builder()
              .contents(S3Object.builder().key("clip-2.mp4").build())
              .isTruncated(false)
              .build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      assertDoesNotThrow(() -> stager.purge(new CancellationToken()));

      assertEquals(2, deletedKeys.size());
      assertTrue(deletedKeys.contains("clip-1.mp4"));
      assertTrue(deletedKeys.contains("clip-2.mp4"));
    }

    @Test
    void doesNothingWhenBucketIsEmpty() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      boolean[] deleteObjectsCalled = { false };
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          deleteObjectsCalled[0] = true;
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      assertDoesNotThrow(() -> stager.purge(new CancellationToken()));
      assertFalse(deleteObjectsCalled[0]);
    }

    @Test
    void scopesListToDirectoryPrefixWhenConfigured() {
      String[] capturedPrefix = { null };

      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          directory = clips
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          capturedPrefix[0] = request.prefix();
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder().isTruncated(false).build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      assertDoesNotThrow(() -> stager.purge(new CancellationToken()));

      assertEquals("clips/", capturedPrefix[0]);
    }

    @Test
    void throwsWhenNotStarted() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      AwsS3Stager stager = new AwsS3Stager(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.purge(new CancellationToken()));

      assertTrue(exception.getMessage().contains("Stager not started"));
    }

    @Test
    void throwsWhenListFails() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.failedFuture(new RuntimeException("List failed"));
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.purge(new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to list objects in S3"));
    }

    @Test
    void throwsWhenDeleteFails() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = aws-s3
          accessKey = key-1
          secretKey = secret-1
          bucket = my-bucket
          publicUrl = "https://cdn.example.com"
          """);
      S3Operations operations = new S3Operations() {
        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
          return CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request) {
          return CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());
        }

        @Override
        public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request) {
          return CompletableFuture.failedFuture(new RuntimeException("Delete failed"));
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request) {
          return CompletableFuture.completedFuture(ListObjectsV2Response.builder()
              .contents(S3Object.builder().key("clip-1.mp4").build())
              .isTruncated(false)
              .build());
        }
      };
      AwsS3Stager stager = new AwsS3Stager(config, operations);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.purge(new CancellationToken()));

      assertTrue(exception.getMessage().contains("Failed to delete objects from S3"));
    }
  }
}
