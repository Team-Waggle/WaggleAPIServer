package io.waggle.waggleapiserver.common.infrastructure.s3

import io.waggle.waggleapiserver.common.storage.GeneratedUploadUrl
import io.waggle.waggleapiserver.common.storage.ImageContentType
import io.waggle.waggleapiserver.common.storage.ImageDirectory
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
    // 키에 업로더 ID를 넣어야 저장 시 남이 올린 객체의 URL인지 가려낼 수 있음
    // presigned URL 서명이 키를 고정하므로 클라이언트가 다른 사람 경로로 올릴 수 없음
    override fun generateUploadUrl(
        directory: ImageDirectory,
        uploaderId: UUID,
        contentType: ImageContentType,
    ): GeneratedUploadUrl {
        val fileName = "${UUID.randomUUID()}.${contentType.extension}"
        val objectUrl = "${uploaderPrefix(directory, uploaderId)}$fileName"
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

    // 파일명까지 발급 형식과 일치해야 함
    // 접두사만 보면 업로더 경로 뒤에 ../를 붙인 URL도 통과함
    override fun isUploadedBy(
        objectUrl: String,
        directory: ImageDirectory,
        uploaderId: UUID,
    ): Boolean {
        val prefix = uploaderPrefix(directory, uploaderId)
        return objectUrl.startsWith(prefix) && GENERATED_FILE_NAME.matches(objectUrl.removePrefix(prefix))
    }

    // 키는 URL의 호스트를 버리고 경로만 쓰므로 우리 버킷 주소가 아니면 다른 객체를 지우게 됨
    // OAuth 가입 시 저장된 외부 프로필 이미지도 여기서 걸러짐
    override fun delete(objectUrl: String) {
        if (!objectUrl.startsWith("$baseUrl/")) {
            return
        }

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

    private fun uploaderPrefix(
        directory: ImageDirectory,
        uploaderId: UUID,
    ) = "$baseUrl/${directory.path}/$uploaderId/"

    companion object {
        private val GENERATED_FILE_NAME =
            Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z]+")
    }
}
