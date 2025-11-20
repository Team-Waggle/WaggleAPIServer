package io.waggle.waggleapiserver.common.infrastructure.config

import io.waggle.waggleapiserver.domain.message.adapter.MessageSubscriber
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    @Bean
    fun channelTopic(): ChannelTopic = ChannelTopic("message")

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
        }

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        messageSubscriber: MessageSubscriber,
        channelTopic: ChannelTopic,
    ): RedisMessageListenerContainer =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            addMessageListener(messageSubscriber, channelTopic)
        }
}
