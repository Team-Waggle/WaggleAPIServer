-- NULL은 무기한 모집을 뜻하는 정식 값이라 백필하지 않음
-- 마감 여부는 저장하지 않고 조회 시점에 expires_at <= now로 파생함
ALTER TABLE posts
    ADD COLUMN expires_at DATETIME(6) NULL;
