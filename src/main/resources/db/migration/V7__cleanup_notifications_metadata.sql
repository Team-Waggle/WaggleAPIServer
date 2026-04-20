-- 1) APPLICATION_RECEIVED: applicationId로 applications 조회해 postId 백필
UPDATE notifications n
INNER JOIN applications a
    ON a.id = CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.applicationId')) AS UNSIGNED)
SET n.metadata = JSON_SET(n.metadata, '$.postId', a.post_id)
WHERE n.type = 'APPLICATION_RECEIVED'
  AND JSON_TYPE(JSON_EXTRACT(n.metadata, '$.applicationId')) = 'INTEGER';

-- 2) 새 스펙에서 triggeredBy를 저장하지 않는 타입들: 제거
--    REVIEW_RECEIVED/REVIEW_REQUESTED(익명), APPLICATION_REMIND(스케줄러 생성)
UPDATE notifications
SET metadata = JSON_REMOVE(metadata, '$.triggeredBy')
WHERE type IN ('REVIEW_RECEIVED', 'REVIEW_REQUESTED', 'APPLICATION_REMIND')
  AND JSON_CONTAINS_PATH(metadata, 'one', '$.triggeredBy');

-- 3) 모든 행에서 쓰지 않는 applicationId 제거
UPDATE notifications
SET metadata = JSON_REMOVE(metadata, '$.applicationId')
WHERE JSON_CONTAINS_PATH(metadata, 'one', '$.applicationId');
