-- 조회순 정렬용
-- InnoDB가 PK(id)를 덧붙여 (view_count, id) 역방향 스캔이 되므로 id를 따로 넣지 않음
CREATE INDEX idx_posts_view_count
    ON posts (view_count);

-- 마감 임박순 첫 묶음(모집 중 + 기한 있음)의 range 스캔용
-- 마감일이 전부 KST 자정이라 expires_at 동률이 흔해 id를 내림차순으로 함께 넣어야 정렬이 사라짐
CREATE INDEX idx_posts_expires_at_id
    ON posts (expires_at, id DESC);
