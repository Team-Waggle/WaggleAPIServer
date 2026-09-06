package io.waggle.waggleapiserver.support

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer

/**
 * 컨텍스트마다 컨테이너가 새로 뜨지 않도록 JVM 싱글턴으로 둠.
 * 스프링에 인스턴스 생성을 맡기면 목 구성이 다른 컨텍스트마다 MySQL이 따로 기동됨.
 * stop하지 않는 것은 의도 - 캐시된 컨텍스트가 죽은 포트를 물지 않게 하며 JVM 종료 시 Ryuk가 정리함
 */
object TestContainers {
    val mysql: MySQLContainer<*> =
        MySQLContainer("mysql:8.0").apply {
            withDatabaseName("waggle")
            withCommand("--ngram-token-size=2")
            start()
        }

    val redis: GenericContainer<*> =
        GenericContainer("redis:7-alpine").apply {
            withExposedPorts(6379)
            start()
        }
}
