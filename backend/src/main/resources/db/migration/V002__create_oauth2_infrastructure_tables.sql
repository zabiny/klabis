-- OAuth2 Infrastructure Tables
-- Created: 2026-02-09
--
-- These tables are part of Spring Authorization Server infrastructure
-- Used for OAuth2 client registration, authorization flows, and token management
-- Not part of the core domain model

-- ============================================================================
-- OAUTH2_REGISTERED_CLIENT TABLE
-- OAuth2 client registrations (Spring Authorization Server schema)
-- ============================================================================

CREATE TABLE oauth2_registered_client
(
    id                            VARCHAR(100)  NOT NULL PRIMARY KEY,
    client_id                     VARCHAR(100)  NOT NULL UNIQUE,
    client_id_issued_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_secret                 VARCHAR(200),
    client_secret_expires_at      TIMESTAMP,
    client_name                   VARCHAR(200)  NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000),
    post_logout_redirect_uris     VARCHAR(1000),
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,
    token_settings                VARCHAR(2000) NOT NULL
);

-- Comments for oauth2_registered_client
COMMENT ON TABLE oauth2_registered_client IS 'OAuth2 client registrations';
COMMENT ON COLUMN oauth2_registered_client.client_secret IS 'BCrypt-hashed client secret';

-- ============================================================================
-- OAUTH2_AUTHORIZATION TABLE
-- OAuth2 authorization records (Spring Authorization Server schema)
-- ============================================================================

CREATE TABLE oauth2_authorization
(
    id                            varchar(100) NOT NULL,
    registered_client_id          varchar(100) NOT NULL,
    principal_name                varchar(200) NOT NULL,
    authorization_grant_type      varchar(100) NOT NULL,
    authorized_scopes             varchar(1000) DEFAULT NULL,
    attributes                    text          DEFAULT NULL,
    state                         varchar(500)  DEFAULT NULL,
    authorization_code_value      text          DEFAULT NULL,
    authorization_code_issued_at  timestamp     DEFAULT NULL,
    authorization_code_expires_at timestamp     DEFAULT NULL,
    authorization_code_metadata   text          DEFAULT NULL,
    access_token_value            text          DEFAULT NULL,
    access_token_issued_at        timestamp     DEFAULT NULL,
    access_token_expires_at       timestamp     DEFAULT NULL,
    access_token_metadata         text          DEFAULT NULL,
    access_token_type             varchar(100)  DEFAULT NULL,
    access_token_scopes           varchar(1000) DEFAULT NULL,
    oidc_id_token_value           text          DEFAULT NULL,
    oidc_id_token_issued_at       timestamp     DEFAULT NULL,
    oidc_id_token_expires_at      timestamp     DEFAULT NULL,
    oidc_id_token_metadata        text          DEFAULT NULL,
    refresh_token_value           text          DEFAULT NULL,
    refresh_token_issued_at       timestamp     DEFAULT NULL,
    refresh_token_expires_at      timestamp     DEFAULT NULL,
    refresh_token_metadata        text          DEFAULT NULL,
    user_code_value               text          DEFAULT NULL,
    user_code_issued_at           timestamp     DEFAULT NULL,
    user_code_expires_at          timestamp     DEFAULT NULL,
    user_code_metadata            text          DEFAULT NULL,
    device_code_value             text          DEFAULT NULL,
    device_code_issued_at         timestamp     DEFAULT NULL,
    device_code_expires_at        timestamp     DEFAULT NULL,
    device_code_metadata          text          DEFAULT NULL,
    PRIMARY KEY (id)
);

-- Indexes for oauth2_authorization
CREATE INDEX idx_oauth2_authorization_registered_client_id ON oauth2_authorization (registered_client_id);
CREATE INDEX idx_oauth2_authorization_principal_name ON oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_authorization_access_token_expires_at ON oauth2_authorization (access_token_expires_at);
CREATE INDEX idx_oauth2_authorization_refresh_token_expires_at ON oauth2_authorization (refresh_token_expires_at);

-- Comments for oauth2_authorization
COMMENT ON TABLE oauth2_authorization IS 'OAuth2 authorization records';

-- ============================================================================
-- OAUTH2_AUTHORIZATION_CONSENT TABLE
-- OAuth2 authorization consent records (Spring Authorization Server schema)
-- ============================================================================

CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(500)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- Comments for oauth2_authorization_consent
COMMENT ON TABLE oauth2_authorization_consent IS 'OAuth2 authorization consent records';
