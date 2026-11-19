package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.RestTemplate
import java.time.Instant

class CustomOAuth2UserServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `탈퇴 중 같은 이메일로 다른 소셜 계정이 가입했으면 탈퇴한 계정을 복원하지 않는다`() {
        val googleUser = createUser("google-user")
        userService.deactivateUser(googleUser)
        userRepository.save(
            User(provider = "kakao", providerId = "kakao-user", email = googleUser.email, profileImageUrl = null),
        )

        assertThatThrownBy { loadGoogleUser(googleUser) }
            .isInstanceOf(OAuth2AuthenticationException::class.java)
            .extracting("error.errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_RESOURCE.name)
        assertThat(count("SELECT COUNT(*) FROM users WHERE email = ? AND deleted_at IS NULL", googleUser.email))
            .isEqualTo(1L)
    }

    @Test
    fun `같은 이메일의 활성 계정이 없으면 탈퇴한 계정을 복원한다`() {
        val googleUser = createUser("google-user")
        userService.deactivateUser(googleUser)

        loadGoogleUser(googleUser)

        assertThat(userRepository.findById(googleUser.id)).isPresent()
    }

    // 실제 서비스는 Google 사용자 정보 API를 호출하므로 응답만 바꿔 끼운 인스턴스로 로그인함
    private fun loadGoogleUser(user: User) {
        val service =
            CustomOAuth2UserService(followRepository, userRepository).apply {
                setRestOperations(googleUserInfoResponse(user))
            }
        transactionTemplate.executeWithoutResult { service.loadUser(googleUserRequest()) }
    }

    private fun googleUserInfoResponse(user: User) =
        object : RestTemplate() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> exchange(
                requestEntity: RequestEntity<*>,
                responseType: ParameterizedTypeReference<T>,
            ): ResponseEntity<T> =
                ResponseEntity.ok(
                    mapOf("sub" to user.providerId, "email" to user.email, "email_verified" to true),
                ) as ResponseEntity<T>
        }

    private fun googleUserRequest() =
        OAuth2UserRequest(
            ClientRegistration
                .withRegistrationId("google")
                .clientId("client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .build(),
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(60),
            ),
        )
}
