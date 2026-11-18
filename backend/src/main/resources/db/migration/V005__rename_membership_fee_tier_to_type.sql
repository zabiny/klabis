-- Rename of the membership fee catalog item "tier" to "type" (data is preserved).
-- Auto-named constraints (primary key, foreign keys) are intentionally not renamed: their generated
-- names differ between H2 and PostgreSQL.

ALTER TABLE membershipfees.membership_fee_tier RENAME TO membership_fee_type;

ALTER TABLE membershipfees.membership_payment_rule
    RENAME COLUMN membership_fee_tier_id TO membership_fee_type_id;

ALTER TABLE membershipfees.membership_fee_group
    RENAME COLUMN source_level_id TO source_type_id;

ALTER INDEX membershipfees.idx_membership_payment_rule_tier RENAME TO idx_membership_payment_rule_type;
ALTER INDEX membershipfees.idx_membership_fee_group_source_level RENAME TO idx_membership_fee_group_source_type;

COMMENT ON TABLE membershipfees.membership_fee_type IS 'Catalog of membership fee type templates. Editable; published types snapshot this into membership_fee_group.';
COMMENT ON COLUMN membershipfees.membership_fee_type.yearly_fee_amount IS 'Annual membership fee for this type';
COMMENT ON TABLE membershipfees.membership_payment_rule IS 'Co-payment rules per (event_type, ranking) combination within a fee type template';
COMMENT ON COLUMN membershipfees.membership_fee_group.source_type_id IS 'Fee type template this snapshot was created from (for D8: previous-year default lookup)';
