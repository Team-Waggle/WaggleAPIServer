-- APPLICATION_RECEIVED 알림 metadata에 position 백필
-- 매칭: notifications.metadata{teamId, postId, triggeredBy} → applications{team_id, post_id, user_id}
-- 동일 (team_id, post_id, user_id)에 여러 application이 있는 경우(서로 다른 직무 지원),
-- 알림 created_at과 가장 가까운 application의 position을 선택
UPDATE notifications n
SET metadata = JSON_SET(
    metadata,
    '$.position',
    (
        SELECT a.position
        FROM applications a
        WHERE a.team_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.teamId')) AS UNSIGNED)
          AND a.post_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.postId')) AS UNSIGNED)
          AND a.user_id = UUID_TO_BIN(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.triggeredBy')))
        ORDER BY ABS(TIMESTAMPDIFF(MICROSECOND, a.created_at, n.created_at))
        LIMIT 1
    )
)
WHERE n.type = 'APPLICATION_RECEIVED'
  AND NOT JSON_CONTAINS_PATH(n.metadata, 'one', '$.position')
  AND EXISTS (
      SELECT 1
      FROM applications a
      WHERE a.team_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.teamId')) AS UNSIGNED)
        AND a.post_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.postId')) AS UNSIGNED)
        AND a.user_id = UUID_TO_BIN(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.triggeredBy')))
  );
