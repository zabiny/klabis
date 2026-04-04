-- Database Cleanup Script for Testing
-- Deletes all data from application tables in correct order to prevent FK violations
-- Use this in tests to ensure clean state between test runs

-- Disable foreign key checks for faster cleanup (optional)
-- SET FOREIGN_KEY_CHECKS = 0;  -- MySQL
-- SET REFERENTIAL_INTEGRITY FALSE;  -- H2

-- OAuth2 authorization consent (must be before oauth2_registered_client)
DELETE FROM oauth2_authorization_consent;

-- OAuth2 authorization records (references oauth2_registered_client)
DELETE FROM oauth2_authorization;

-- OAuth2 registered clients
DELETE FROM oauth2_registered_client;

-- Users aggregate (password tokens and user permissions are deleted through cascade)
-- DELETE FROM password_setup_tokens;
-- DELETE FROM user_permissions;
DELETE FROM users;

-- Event publication outbox table
DELETE FROM event_publication;

-- Calendar items (references events)
DELETE FROM calendar_items;

-- Events table (registrations are deleted through cascade)
-- DELETE FROM event_registrations;
DELETE FROM events;

-- Training groups (members module aggregate root)
DELETE FROM training_group_members;
DELETE FROM training_group_trainers;
DELETE FROM training_groups;

-- Members table
DELETE FROM members;

-- Re-enable foreign key checks (if disabled above)
-- SET FOREIGN_KEY_CHECKS = 1;  -- MySQL
-- SET REFERENTIAL_INTEGRITY TRUE;  -- H2
