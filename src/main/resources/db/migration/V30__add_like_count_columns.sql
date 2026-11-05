ALTER TABLE posts
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE comments
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;

UPDATE posts p
SET p.like_count = (SELECT COUNT(*) FROM likes l WHERE l.type = 'POST' AND l.target_id = p.id);

UPDATE comments c
SET c.like_count = (SELECT COUNT(*) FROM likes l WHERE l.type = 'COMMENT' AND l.target_id = c.id);

CREATE INDEX idx_posts_like_count
    ON posts (like_count);
