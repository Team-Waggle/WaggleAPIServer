package io.waggle.waggleapiserver.support

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * 테이블 목록을 손으로 나열하면 테이블이 추가될 때마다 낡아 테스트끼리 데이터가 샘.
 * 실제로 conversations·messages 누락 탓에 대화 검색 테스트가 간섭받은 전례가 있음
 */
@Component
class DatabaseCleaner(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val tables: List<String> by lazy {
        jdbcTemplate.queryForList(
            """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = DATABASE()
            AND table_type = 'BASE TABLE'
            AND table_name != 'flyway_schema_history'
            """,
            String::class.java,
        )
    }

    // TRUNCATE는 InnoDB에서 테이블스페이스를 재생성하는 DDL이라 훅마다 돌리면 스위트가 배로 느려짐
    fun clean() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        tables.forEach { jdbcTemplate.execute("DELETE FROM $it") }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }
}
