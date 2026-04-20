-- 1) metadata JSON 컬럼 추가
ALTER TABLE notifications ADD COLUMN metadata JSON NULL;

-- 2) 기존 데이터를 metadata JSON으로 마이그레이션
UPDATE notifications
SET metadata = JSON_OBJECT(
    'teamId', team_id,
    'applicationId', application_id,
    'triggeredBy', IF(triggered_by IS NOT NULL, BIN_TO_UUID(triggered_by), NULL)
);

-- 3) APPLICATION_ACCEPTED -> TEAM_JOINED 타입 변경
UPDATE notifications
SET type = 'TEAM_JOINED'
WHERE type = 'APPLICATION_ACCEPTED';

-- 4) 기존 컬럼 드롭
ALTER TABLE notifications DROP COLUMN team_id;
ALTER TABLE notifications DROP COLUMN application_id;
ALTER TABLE notifications DROP COLUMN triggered_by;
