-- Drop old index
DROP INDEX idx_notifications_user_read_created ON notifications;

-- Remove old columns
ALTER TABLE notifications
    DROP COLUMN title,
    DROP COLUMN content,
    DROP COLUMN redirect_url;

-- Add new columns
ALTER TABLE notifications
    ADD COLUMN type VARCHAR(32) NOT NULL AFTER id,
    ADD COLUMN project_id BIGINT NULL AFTER type;

-- Create new index matching the entity specification
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, read_at, created_at DESC);
