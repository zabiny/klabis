INSERT INTO events (id, name, event_date, location, organizer, status, registration_deadline,
                    created_at, created_by, modified_at, modified_by, version)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Deadline Passed Event',
        CURRENT_DATE + 30,
        'Test Location',
        'TEST',
        'ACTIVE',
        CURRENT_DATE - 1,
        CURRENT_TIMESTAMP, 'test-setup',
        CURRENT_TIMESTAMP, 'test-setup',
        0);

INSERT INTO event_registrations (id, event_id, member_id, si_card_number, registered_at)
VALUES (gen_random_uuid(),
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        '11111111-1111-1111-1111-111111111111',
        '123456',
        CURRENT_TIMESTAMP);
