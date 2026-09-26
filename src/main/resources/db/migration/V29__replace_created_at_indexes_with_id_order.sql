ALTER TABLE posts
    DROP INDEX idx_posts_title;

CREATE INDEX idx_notifications_user
    ON notifications (user_id);

ALTER TABLE notifications
    DROP INDEX idx_notifications_user_read_created;

CREATE INDEX idx_messages_sender_receiver
    ON messages (sender_id, receiver_id);

CREATE INDEX idx_messages_receiver_sender
    ON messages (receiver_id, sender_id);

ALTER TABLE messages
    DROP INDEX idx_messages_sender_receiver_created,
    DROP INDEX idx_messages_receiver_sender_created;
