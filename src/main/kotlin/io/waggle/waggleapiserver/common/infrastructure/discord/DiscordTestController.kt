package io.waggle.waggleapiserver.common.infrastructure.discord

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Profile("prod")
@RestController
class DiscordTestController {

    @GetMapping("/test/discord-error")
    fun triggerError(): Nothing {
        throw RuntimeException("Discord 알림 테스트용 에러")
    }
}
