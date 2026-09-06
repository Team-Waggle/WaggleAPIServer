package io.waggle.waggleapiserver.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "COOKIE_DOMAIN=localhost",
        "OAUTH2_REDIRECT_URI=http://localhost",
        "S3_BASE_URL=http://localhost",
        "S3_BUCKET=test-bucket",
        "JWT_SECRET=test-secret-test-secret-test-secret-test-secret",
        "GOOGLE_CLIENT_ID=test",
        "GOOGLE_CLIENT_SECRET=test",
        "KAKAO_CLIENT_ID=test",
        "KAKAO_CLIENT_SECRET=test",
    ],
)
@ActiveProfiles("mysql-test")
@Import(TestcontainersConfig::class)
abstract class IntegrationTestSupport
