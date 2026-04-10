--  V1 : Schéma initial — URL Shortener

-- Extension UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--  TABLE: shortened_urls

CREATE TABLE shortened_urls (
    id UUID PRIMARY KEY,
    url TEXT NOT NULL,
    short_code VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,

    CONSTRAINT uk_short_code UNIQUE (short_code),
    CONSTRAINT uk_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_url_length CHECK (LENGTH(url) <= 2048),
    CONSTRAINT chk_short_code_length CHECK (LENGTH(short_code) BETWEEN 4 AND 20)
);

CREATE INDEX idx_expires_at
    ON shortened_urls (expires_at)
    WHERE expires_at IS NOT NULL;

--  TABLE : access_logs

CREATE TABLE access_logs (
    id BIGSERIAL PRIMARY KEY,
    url_id UUID NOT NULL,
    accessed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_access_log_shortened_url
     FOREIGN KEY (url_id) REFERENCES shortened_urls(id)
         ON DELETE CASCADE
);

CREATE INDEX idx_access_logs_url_id
    ON access_logs (url_id);

CREATE INDEX idx_access_logs_accessed_at
    ON access_logs (accessed_at DESC);