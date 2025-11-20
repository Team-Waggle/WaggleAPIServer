CREATE TABLE messages
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    sender_id   BINARY(16)            NOT NULL,
    receiver_id BINARY(16)            NOT NULL,
    content     TEXT                  NOT NULL,
    read_at     DATETIME(6)           NULL,
    created_at  DATETIME(6)           NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

CREATE INDEX idx_messages_receiver_read ON messages (receiver_id, read_at);

CREATE INDEX idx_messages_receiver_sender_created ON messages (receiver_id, sender_id, created_at);

CREATE INDEX idx_messages_sender_receiver_created ON messages (sender_id, receiver_id, created_at);

ALTER TABLE notifications
    DROP COLUMN is_read,
    ADD COLUMN read_at DATETIME(6) NULL;

-- Drop old indexes
DROP INDEX idx_applications_project_id ON applications;
DROP INDEX idx_applications_user_id ON applications;
DROP INDEX idx_applications_project_id_status ON applications;
DROP INDEX idx_follows_followee_id ON follows;
DROP INDEX idx_members_project_id ON members;
DROP INDEX idx_notifications_user_id_is_read_created_at ON notifications;
DROP INDEX idx_recruitments_project_id ON recruitments;

-- Create new indexes
CREATE INDEX idx_applications_project ON applications (project_id);
CREATE INDEX idx_applications_user ON applications (user_id);
CREATE INDEX idx_applications_project_status ON applications (project_id, status);
CREATE INDEX idx_follows_folowee ON follows (followee_id);
CREATE INDEX idx_members_project ON members (project_id);
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, read_at, created_at);
CREATE INDEX idx_recruitments_project ON recruitments (project_id);
