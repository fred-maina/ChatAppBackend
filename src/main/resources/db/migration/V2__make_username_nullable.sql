-- V2__make_username_nullable.sql
ALTER TABLE users
    ALTER COLUMN username DROP NOT NULL;
