package io.waggle.waggleapiserver.domain.message.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.stereotype.Component

@Component
class MessagePublisher(
    private val objectMapper: ObjectMapper,
    private val redisTemplate: RedisTemplate<String, String>,
    private val channelTopic: ChannelTopic,
) {
    fun publish(event: MessageEvent) {
        val payload = objectMapper.writeValueAsString(event)
        redisTemplate.convertAndSend(channelTopic.topic, payload)
    }
}
