ALTER TABLE applications
    ADD COLUMN active_user_id BINARY(16) AS (IF(deleted_at IS NULL, user_id, NULL)) VIRTUAL;

CREATE UNIQUE INDEX uk_applications_post_active_user
    ON applications (post_id, active_user_id);
