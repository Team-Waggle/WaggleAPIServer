package io.waggle.waggleapiserver.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean
    @ServiceConnection
    fun mysql(): MySQLContainer<*> = TestContainers.mysql

    @Bean
    @ServiceConnection("redis")
    fun redis(): GenericContainer<*> = TestContainers.redis
}
