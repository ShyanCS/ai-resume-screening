ALTER TABLE email_tokens ADD COLUMN org_id BIGINT REFERENCES organizations (id);
ALTER TABLE email_tokens ADD COLUMN invited_role VARCHAR(20);
ALTER TABLE email_tokens
    ADD CONSTRAINT ck_email_tokens_invited_role CHECK (invited_role IN ('ORG_ADMIN', 'RECRUITER'));
