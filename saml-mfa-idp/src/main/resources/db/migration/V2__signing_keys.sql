CREATE TABLE signing_keys (
    id UUID PRIMARY KEY,
    cka_id VARCHAR(64) NOT NULL,
    label VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    certificate_der BYTEA NOT NULL,
    fingerprint_sha256 VARCHAR(64) NOT NULL,
    not_before TIMESTAMPTZ NOT NULL,
    not_after TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_signing_keys_cka UNIQUE (cka_id)
);

CREATE UNIQUE INDEX uk_signing_keys_active ON signing_keys (status) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_signing_keys_next ON signing_keys (status) WHERE status = 'NEXT';

CREATE TABLE key_events (
    id UUID PRIMARY KEY,
    signing_key_id UUID NOT NULL REFERENCES signing_keys (id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    details VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL
);
