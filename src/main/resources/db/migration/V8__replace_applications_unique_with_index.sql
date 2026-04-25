ALTER TABLE applications
    DROP INDEX UK7uplan8w2mdqesciciufjepsx;

CREATE INDEX idx_applications_post_user_position
    ON applications (post_id, user_id, position);
