package io.waggle.waggleapiserver.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer

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
abstract class IntegrationTestSupport {
    companion object {
        // 여러 @SpringBootTest 클래스가 공유하는 싱글턴 컨테이너.
        // @Testcontainers/@Container는 클래스마다 컨테이너를 stop시켜 캐시된 스프링 컨텍스트가
        // 죽은 포트를 물게 만든다 → 수동 start 후 stop하지 않는 싱글턴 패턴 사용(JVM 종료 시 Ryuk가 정리).
        private val mysql =
            MySQLContainer("mysql:8.0").apply {
                withDatabaseName("waggle")
                withCommand("--ngram-token-size=2")
            }

        private val redis: GenericContainer<*> = GenericContainer("redis:7-alpine").withExposedPorts(6379)

        init {
            mysql.start()
            redis.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun containerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }
    }
}
