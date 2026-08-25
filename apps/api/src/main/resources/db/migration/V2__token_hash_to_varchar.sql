ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
ALTER TABLE email_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
