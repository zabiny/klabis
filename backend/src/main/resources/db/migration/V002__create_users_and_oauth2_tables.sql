-- Create users table for authentication
CREATE TABLE users
(
    id                      UUID PRIMARY KEY,
    registration_number     VARCHAR(7)   NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    account_status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    account_non_expired     BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE user_authorities
(
    user_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    authority VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, authority)
);

-- OAuth2 tables (Spring Authorization Server schema)
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

CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(500)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- Create indexes
CREATE INDEX idx_users_registration_number ON users (registration_number);
CREATE INDEX idx_users_account_status ON users (account_status);
CREATE INDEX idx_user_authorities_user_id ON user_authorities (user_id);
CREATE INDEX idx_oauth2_authorization_registered_client_id ON oauth2_authorization (registered_client_id);
CREATE INDEX idx_oauth2_authorization_principal_name ON oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_authorization_access_token_expires_at ON oauth2_authorization (access_token_expires_at);
CREATE INDEX idx_oauth2_authorization_refresh_token_expires_at ON oauth2_authorization (refresh_token_expires_at);

-- NOTE: Bootstrap data (admin user and OAuth2 client) is now managed by
-- BootstrapDataLoader component which reads credentials from environment variables.
-- This prevents credentials from being exposed in version control.
-- See: com.klabis.config.BootstrapDataLoader

-- Comments
COMMENT ON TABLE users IS 'User accounts for authentication';
COMMENT ON TABLE user_authorities IS 'User authorities assignments';
COMMENT ON TABLE oauth2_registered_client IS 'OAuth2 client registrations';
COMMENT ON TABLE oauth2_authorization IS 'OAuth2 authorization records';
COMMENT ON COLUMN users.registration_number IS 'Unique registration number, username for login';
COMMENT ON COLUMN users.password_hash IS 'BCrypt-hashed password';
COMMENT ON COLUMN oauth2_registered_client.client_secret IS 'BCrypt-hashed client secret';
