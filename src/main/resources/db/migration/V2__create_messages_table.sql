-- Messages table
CREATE TABLE messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BINARY(16) NOT NULL,
    receiver_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    read_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_messages_sender_id (sender_id),
    INDEX idx_messages_receiver_id (receiver_id),
    INDEX idx_messages_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
