-- Add member self-edit fields
-- Phase 2: Add editable profile fields for member self-service functionality
-- These fields support members updating their own profile information via the API

-- Add chip number field
ALTER TABLE members
    ADD COLUMN chip_number VARCHAR(50);
COMMENT ON COLUMN members.chip_number IS 'Member identification chip number (nullable, max 50 characters)';

-- Add identity card fields (flattened from IdentityCard value object)
ALTER TABLE members
    ADD COLUMN identity_card_number VARCHAR(50);
ALTER TABLE members
    ADD COLUMN identity_card_validity_date DATE;
COMMENT ON COLUMN members.identity_card_number IS 'Identity card number (nullable, max 50 characters)';
COMMENT ON COLUMN members.identity_card_validity_date IS 'Identity card validity expiration date (nullable)';

-- Add medical course fields (flattened from MedicalCourse value object)
ALTER TABLE members
    ADD COLUMN medical_course_completion_date DATE;
ALTER TABLE members
    ADD COLUMN medical_course_validity_date DATE;
COMMENT ON COLUMN members.medical_course_completion_date IS 'Medical course completion date (nullable)';
COMMENT ON COLUMN members.medical_course_validity_date IS 'Medical course validity expiration date (nullable)';

-- Add trainer license fields (flattened from TrainerLicense value object)
ALTER TABLE members
    ADD COLUMN trainer_license_number VARCHAR(50);
ALTER TABLE members
    ADD COLUMN trainer_license_validity_date DATE;
COMMENT ON COLUMN members.trainer_license_number IS 'Trainer license number (nullable, max 50 characters)';
COMMENT ON COLUMN members.trainer_license_validity_date IS 'Trainer license validity expiration date (nullable)';

-- Add driving license group field
ALTER TABLE members
    ADD COLUMN driving_license_group VARCHAR(10);
COMMENT ON COLUMN members.driving_license_group IS 'Driving license group/category (nullable, max 10 characters, e.g., BE, C1, C1E)';

-- Add dietary restrictions field
ALTER TABLE members
    ADD COLUMN dietary_restrictions VARCHAR(500);
COMMENT ON COLUMN members.dietary_restrictions IS 'Dietary restrictions or food allergies (nullable, max 500 characters)';
