package io.waggle.waggleapiserver.common.infrastructure.s3

import io.waggle.waggleapiserver.common.storage.GeneratedUploadUrl
import io.waggle.waggleapiserver.common.storage.ImageContentType
import io.waggle.waggleapiserver.common.storage.StorageClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Component
class S3StorageClient(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${app.s3.bucket}") private val bucket: String,
    @Value("\${app.s3.base-url}") private val baseUrl: String,
    @Value("\${app.s3.presigned-url-expiration}") private val expirationSeconds: Long,
) : StorageClient {
    override fun generateUploadUrl(
        directory: String,
        contentType: ImageContentType,
    ): GeneratedUploadUrl {
        val fileName = "${UUID.randomUUID()}.${contentType.extension}"
        val objectUrl = "$baseUrl/$directory/$fileName"
        val key =
            java.net
                .URI(objectUrl)
                .path
                .removePrefix("/")

        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType.mediaType)
                .build()

        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds))
                .putObjectRequest(putObjectRequest)
                .build()

        val presignedRequest = s3Presigner.presignPutObject(presignRequest)

        return GeneratedUploadUrl(
            presignedUrl = presignedRequest.url().toString(),
            objectUrl = objectUrl,
        )
    }

    override fun delete(objectUrl: String) {
        val key =
            java.net
                .URI(objectUrl)
                .path
                .removePrefix("/")

        val deleteObjectRequest =
            DeleteObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .build()

        s3Client.deleteObject(deleteObjectRequest)
    }
}
