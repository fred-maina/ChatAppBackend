CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE reported_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID REFERENCES chat_message(message_id) ON DELETE SET NULL,
    reporter_id UUID NOT NULL REFERENCES users(id),
    anonymous_session_id VARCHAR(255) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reported_content TEXT NOT NULL,
    reported_nickname VARCHAR(255),
    reported_message_timestamp TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE blocked_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_session_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, blocked_session_id)
);

ALTER TABLE users ADD COLUMN eula_accepted_at TIMESTAMP WITH TIME ZONE;
