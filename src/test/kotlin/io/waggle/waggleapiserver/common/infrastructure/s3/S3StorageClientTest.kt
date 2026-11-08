package io.waggle.waggleapiserver.common.infrastructure.s3

import io.waggle.waggleapiserver.common.storage.ImageContentType
import io.waggle.waggleapiserver.common.storage.ImageDirectory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.util.UUID

class S3StorageClientTest {
    private val s3Client = mock(S3Client::class.java)
    private val storageClient =
        S3StorageClient(
            s3Client = s3Client,
            s3Presigner =
                S3Presigner
                    .builder()
                    .region(Region.AP_NORTHEAST_2)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                    .build(),
            bucket = BUCKET,
            baseUrl = BASE_URL,
            expirationSeconds = 600,
        )
    private val uploaderId = UUID.randomUUID()

    @Test
    fun `업로드 URL은 업로더 경로 아래로 발급되고 그 업로더가 올린 것으로 판정된다`() {
        val generated = storageClient.generateUploadUrl(ImageDirectory.USER_PROFILE, uploaderId, ImageContentType.PNG)

        assertThat(generated.objectUrl).startsWith("$BASE_URL/users/$uploaderId/").endsWith(".png")
        assertThat(generated.presignedUrl).contains("/prod/users/$uploaderId/")
        assertThat(storageClient.isUploadedBy(generated.objectUrl, ImageDirectory.USER_PROFILE, uploaderId)).isTrue()
        assertThat(
            storageClient.isUploadedBy(generated.objectUrl, ImageDirectory.USER_PROFILE, UUID.randomUUID()),
        ).isFalse()
        assertThat(storageClient.isUploadedBy(generated.objectUrl, ImageDirectory.TEAM_PROFILE, uploaderId)).isFalse()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            // 업로더 ID가 없는 과거 형식
            "$BASE_URL/users/00000000-0000-0000-0000-000000000000.png",
            // 호스트만 바꾼 URL
            "https://evil.example.com/prod/users/{uploader}/00000000-0000-0000-0000-000000000000.png",
            // 업로더 경로 뒤에서 상위 경로로 빠져나가는 URL
            "$BASE_URL/users/{uploader}/../../users/00000000-0000-0000-0000-000000000000.png",
            "$BASE_URL/users/{uploader}/nested/00000000-0000-0000-0000-000000000000.png",
        ],
    )
    fun `발급 형식이 아닌 URL은 업로더가 올린 것으로 판정하지 않는다`(url: String) {
        val objectUrl = url.replace("{uploader}", uploaderId.toString())

        assertThat(storageClient.isUploadedBy(objectUrl, ImageDirectory.USER_PROFILE, uploaderId)).isFalse()
    }

    @Test
    fun `우리 버킷 주소의 객체는 경로를 키로 삭제한다`() {
        storageClient.delete("$BASE_URL/users/$uploaderId/00000000-0000-0000-0000-000000000000.png")

        val captor = ArgumentCaptor.forClass(DeleteObjectRequest::class.java)
        verify(s3Client).deleteObject(captor.capture())
        assertThat(captor.value.bucket()).isEqualTo(BUCKET)
        assertThat(captor.value.key()).isEqualTo("prod/users/$uploaderId/00000000-0000-0000-0000-000000000000.png")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://evil.example.com/prod/users/00000000-0000-0000-0000-000000000000.png",
            "https://lh3.googleusercontent.com/a/profile-photo",
            "http://k.kakaocdn.net/dn/profile/img_640x640.jpg",
        ],
    )
    fun `우리 버킷 주소가 아닌 URL은 삭제 요청을 보내지 않는다`(url: String) {
        storageClient.delete(url)

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest::class.java))
    }

    companion object {
        private const val BUCKET = "waggle-test"
        private const val BASE_URL = "https://waggle-test.s3.ap-northeast-2.amazonaws.com/prod"
    }
}
