-- Test setup for shouldFailUnregistrationOnOrAfterEventDate:
-- inserts a past ACTIVE event with a registration for member 11111111-1111-1111-1111-111111111111

INSERT INTO events (id, name, event_date, location, organizer, status,
                    created_at, created_by, modified_at, modified_by, version)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Past Event',
        CURRENT_DATE - 10,
        'Test Location',
        'TEST',
        'ACTIVE',
        CURRENT_TIMESTAMP, 'test-setup',
        CURRENT_TIMESTAMP, 'test-setup',
        0);

INSERT INTO event_registrations (id, event_id, member_id, si_card_number, registered_at)
VALUES (gen_random_uuid(),
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        '123456',
        CURRENT_TIMESTAMP);
