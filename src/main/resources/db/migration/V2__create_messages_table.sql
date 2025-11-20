CREATE TABLE messages
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    sender_id   BINARY(16)            NOT NULL,
    receiver_id BINARY(16)            NOT NULL,
    content     TEXT                  NOT NULL,
    read_at     datetime              NULL,
    created_at  datetime              NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

CREATE INDEX idx_messages_receiver_read ON messages (receiver_id, read_at);

CREATE INDEX idx_messages_receiver_sender_created ON messages (receiver_id, sender_id, created_at);

CREATE INDEX idx_messages_sender_receiver_created ON messages (sender_id, receiver_id, created_at);