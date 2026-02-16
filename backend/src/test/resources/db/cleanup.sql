-- Database Cleanup Script for Testing
-- Deletes all data from application tables in correct order to prevent FK violations
-- Use this in tests to ensure clean state between test runs

-- Disable foreign key checks for faster cleanup (optional)
-- SET FOREIGN_KEY_CHECKS = 0;  -- MySQL
-- SET REFERENTIAL_INTEGRITY FALSE;  -- H2

-- OAuth2 authorization consent (must be before oauth2_registered_client)
DELETE
FROM oauth2_authorization_consent;

-- OAuth2 authorization records (references oauth2_registered_client)
DELETE
FROM oauth2_authorization;

-- OAuth2 registered clients
DELETE
FROM oauth2_registered_client;

-- Password setup tokens (references users with ON DELETE CASCADE)
DELETE
FROM password_setup_tokens;

-- User permissions (current table, references users)
DELETE
FROM user_permissions;

-- Users table
DELETE
FROM users;

-- Event publication outbox table
DELETE
FROM event_publication;

-- Calendar items (references events)
DELETE
FROM calendar_items;

-- Event registrations (references events and members)
DELETE
FROM event_registrations;

-- Events table
DELETE
FROM events;

-- Members table
DELETE
FROM members;

-- Re-enable foreign key checks (if disabled above)
-- SET FOREIGN_KEY_CHECKS = 1;  -- MySQL
-- SET REFERENTIAL_INTEGRITY TRUE;  -- H2
