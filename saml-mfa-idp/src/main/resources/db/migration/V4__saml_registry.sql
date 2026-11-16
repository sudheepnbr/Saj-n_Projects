CREATE TABLE service_providers (
    id UUID PRIMARY KEY,
    entity_id VARCHAR(256) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    want_authn_requests_signed BOOLEAN NOT NULL DEFAULT TRUE,
    encrypt_assertions BOOLEAN NOT NULL DEFAULT FALSE,
    nameid_format VARCHAR(128) NOT NULL DEFAULT 'urn:oasis:names:tc:SAML:2.0:nameid-format:persistent',
    mfa_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    modified_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_sp_entity UNIQUE (entity_id)
);

CREATE TABLE sp_acs (
    id UUID PRIMARY KEY,
    sp_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    binding VARCHAR(128) NOT NULL,
    url VARCHAR(512) NOT NULL,
    default_acs BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_sp_acs UNIQUE (sp_id, binding, url)
);

CREATE TABLE sp_slo (
    id UUID PRIMARY KEY,
    sp_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    binding VARCHAR(128) NOT NULL,
    url VARCHAR(512) NOT NULL,
    CONSTRAINT uk_sp_slo UNIQUE (sp_id, binding, url)
);

CREATE TABLE sp_certificates (
    id UUID PRIMARY KEY,
    sp_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    usage VARCHAR(16) NOT NULL,
    certificate_der BYTEA NOT NULL,
    fingerprint_sha256 VARCHAR(64) NOT NULL,
    not_after TIMESTAMPTZ NOT NULL
);

CREATE TABLE attribute_mappings (
    id UUID PRIMARY KEY,
    sp_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    saml_name VARCHAR(128) NOT NULL,
    source_field VARCHAR(64) NOT NULL,
    CONSTRAINT uk_attr_map UNIQUE (sp_id, saml_name)
);

CREATE TABLE persistent_nameids (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sp_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    name_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_persistent_nameid UNIQUE (user_id, sp_id)
);

CREATE TABLE replay_cache (
    id_hash VARCHAR(64) PRIMARY KEY,
    message_type VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_replay_expires ON replay_cache (expires_at);

CREATE TABLE sp_sessions (
    id UUID PRIMARY KEY,
    idp_session_id UUID NOT NULL REFERENCES idp_sessions (id) ON DELETE CASCADE,
    sp_id UUID NOT NULL REFERENCES service_providers (id) ON DELETE CASCADE,
    session_index VARCHAR(64) NOT NULL,
    name_id VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    slo_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE logout_correlation (
    id UUID PRIMARY KEY,
    idp_session_id UUID REFERENCES idp_sessions (id) ON DELETE SET NULL,
    originating_sp_id UUID REFERENCES service_providers (id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);
