ALTER TABLE chat_message
    ADD COLUMN to_session_id VARCHAR(255),
    ADD COLUMN from_user_id UUID,
    ADD CONSTRAINT fk_from_user FOREIGN KEY (from_user_id) REFERENCES users(id);
