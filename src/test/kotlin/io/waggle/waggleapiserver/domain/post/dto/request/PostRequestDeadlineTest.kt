package io.waggle.waggleapiserver.domain.post.dto.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDate

// @JsonTest는 앱 클래스의 @EnableJpaAuditing까지 끌어와 JPA 메타모델을 요구하므로 Jackson 자동설정만 올림
@SpringJUnitConfig(JacksonAutoConfiguration::class)
class PostRequestDeadlineTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `수정 요청에서 deadline 키를 빠뜨리면 거부한다`() {
        assertThatThrownBy {
            objectMapper.readValue(
                """{"title":"제목","content":"내용","recruitments":[]}""",
                PostUpdateRequest::class.java,
            )
        }.isInstanceOf(MismatchedInputException::class.java)
    }

    @Test
    fun `생성 요청에서 deadline 키를 빠뜨리면 거부한다`() {
        assertThatThrownBy {
            objectMapper.readValue(
                """{"teamId":1,"title":"제목","content":"내용","recruitments":[]}""",
                PostCreateRequest::class.java,
            )
        }.isInstanceOf(MismatchedInputException::class.java)
    }

    @Test
    fun `deadline이 null이면 무기한으로 받는다`() {
        val request =
            objectMapper.readValue(
                """{"title":"제목","content":"내용","recruitments":[],"deadline":null}""",
                PostUpdateRequest::class.java,
            )

        assertThat(request.deadline).isNull()
    }

    @Test
    fun `deadline은 시분초 없는 날짜로 받는다`() {
        val request =
            objectMapper.readValue(
                """{"title":"제목","content":"내용","recruitments":[],"deadline":"2026-09-30"}""",
                PostUpdateRequest::class.java,
            )

        assertThat(request.deadline).isEqualTo(LocalDate.of(2026, 9, 30))
    }
}
