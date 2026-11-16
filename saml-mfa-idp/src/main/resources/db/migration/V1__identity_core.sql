CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    username_normalized VARCHAR(64) NOT NULL,
    email VARCHAR(256) NOT NULL,
    email_normalized VARCHAR(256) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    failed_logins INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ,
    last_failed_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    modified_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_username_normalized UNIQUE (username_normalized),
    CONSTRAINT uk_users_email_normalized UNIQUE (email_normalized)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE password_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_password_history_user ON password_history (user_id, created_at DESC);

CREATE TABLE auth_source_throttle (
    source VARCHAR(64) PRIMARY KEY,
    failures INTEGER NOT NULL DEFAULT 0,
    window_started_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    actor_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    sp_entity_id VARCHAR(256),
    outcome VARCHAR(32) NOT NULL,
    client_ip INET,
    user_agent VARCHAR(512),
    correlation_id VARCHAR(64),
    failure_category VARCHAR(64),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_created ON audit_events (created_at);
CREATE INDEX idx_audit_actor ON audit_events (actor_user_id);
CREATE INDEX idx_audit_type ON audit_events (event_type);

CREATE TABLE admin_bootstrap (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

INSERT INTO roles (id, name) VALUES
    ('11111111-1111-1111-1111-111111111001', 'USER'),
    ('11111111-1111-1111-1111-111111111002', 'OPERATOR'),
    ('11111111-1111-1111-1111-111111111003', 'ADMIN'),
    ('11111111-1111-1111-1111-111111111004', 'AUDITOR');
