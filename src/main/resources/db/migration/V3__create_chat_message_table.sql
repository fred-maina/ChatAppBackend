CREATE TABLE IF NOT EXISTS chat_message (
                              message_id UUID PRIMARY KEY,
                              from_session_id VARCHAR(255),
                              content TEXT NOT NULL,
                              nickname VARCHAR(255),
                              timestamp TIMESTAMP,
                              to_user_id UUID NOT NULL,
                              CONSTRAINT fk_to_user FOREIGN KEY (to_user_id) REFERENCES users(id)
);
