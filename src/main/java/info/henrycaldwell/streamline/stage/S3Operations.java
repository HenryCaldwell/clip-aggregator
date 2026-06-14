package info.henrycaldwell.streamline.stage;

import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Interface for performing S3-compatible object storage operations.
 *
 * This interface defines a contract for dispatching S3 operations used by
 * stagers, allowing real S3 calls to be substituted in tests.
 */
interface S3Operations {

  /**
   * Uploads an object to a bucket.
   *
   * @param request A {@link PutObjectRequest} representing the upload request.
   * @param body    An {@link AsyncRequestBody} representing the object content.
   * @return A {@link CompletableFuture} representing the upload result.
   */
  CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body);

  /**
   * Deletes an object from a bucket.
   *
   * @param request A {@link DeleteObjectRequest} representing the delete request.
   * @return A {@link CompletableFuture} representing the delete result.
   */
  CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest request);

  /**
   * Deletes multiple objects from a bucket.
   *
   * @param request A {@link DeleteObjectsRequest} representing the batch delete
   *                request.
   * @return A {@link CompletableFuture} representing the delete result.
   */
  CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest request);

  /**
   * Lists objects in a bucket.
   *
   * @param request A {@link ListObjectsV2Request} representing the list request.
   * @return A {@link CompletableFuture} representing the listed objects.
   */
  CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request request);
}
