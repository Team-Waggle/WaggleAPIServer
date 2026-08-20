-- notification cascade 삭제를 인덱스 가능하게: metadata(JSON)는 유지하고
-- 쿼리 키로만 쓰이는 teamId/postId만 generated STORED 컬럼으로 승격해 인덱싱
-- STORED라 기존 행도 ALTER 시점에 계산되어 백필 불필요
ALTER TABLE notifications
    ADD COLUMN team_id BIGINT
        GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.teamId')) AS UNSIGNED)) STORED,
    ADD COLUMN post_id BIGINT
        GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.postId')) AS UNSIGNED)) STORED;

CREATE INDEX idx_notifications_team
    ON notifications (team_id);

CREATE INDEX idx_notifications_post
    ON notifications (post_id);
