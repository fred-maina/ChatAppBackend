-- Add the is_read column to the chat_message table
ALTER TABLE chat_message
    ADD COLUMN is_read BOOLEAN DEFAULT FALSE;

