import {describe, expect, it} from 'vitest';

import {parseAuthorities, validateSpec} from '../lib/validate.mjs';

const AUTHORITY_JAVA = `
package com.klabis.common.users;

public enum Authority {
    CALENDAR_MANAGE("CALENDAR:MANAGE", Scope.CONTEXT_SPECIFIC),
    MEMBERS_MANAGE("MEMBERS:MANAGE", Scope.CONTEXT_SPECIFIC),
    FINANCE_MANAGE("FINANCE:MANAGE", Scope.GLOBAL);

    public static final String MEMBERS_SCOPE = "MEMBERS";
    public static final String EVENTS_SCOPE = "EVENTS";
}
`;

describe('parseAuthorities', () => {
    it('extracts enum constants', () => {
        expect(parseAuthorities(AUTHORITY_JAVA))
            .toEqual(new Set(['CALENDAR_MANAGE', 'MEMBERS_MANAGE', 'FINANCE_MANAGE']));
    });

    it('does not mistake *_SCOPE string constants for authorities', () => {
        const authorities = parseAuthorities(AUTHORITY_JAVA);
        expect(authorities.has('MEMBERS_SCOPE')).toBe(false);
        expect(authorities.has('EVENTS_SCOPE')).toBe(false);
    });
});

describe('validateSpec', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    const docWithSchema = (properties) => ({
        paths: {},
        components: {schemas: {Thing: {type: 'object', properties}}},
    });

    it('accepts a known authority', () => {
        expect(validate(docWithSchema({
            dateOfBirth: {type: 'string', 'x-klabis-authority': 'MEMBERS_MANAGE'},
        }))).toEqual([]);
    });

    it('rejects an unknown authority', () => {
        const errors = validate(docWithSchema({
            dateOfBirth: {type: 'string', 'x-klabis-authority': 'MEMBERS_MANAG'},
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not a constant of Authority.java');
    });

    it('rejects a *_SCOPE constant used as an authority', () => {
        const errors = validate(docWithSchema({
            x: {type: 'string', 'x-klabis-authority': 'MEMBERS_SCOPE'},
        }));
        expect(errors).toHaveLength(1);
    });

    it('rejects a misspelled extension name', () => {
        const errors = validate(docWithSchema({
            id: {type: 'string', 'x-klabis-owner-idd': true},
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('Unknown extension');
    });

    it('rejects an invalid halforms access value', () => {
        const errors = validate(docWithSchema({
            status: {type: 'string', 'x-klabis-halforms-access': 'READONLY'},
        }));
        expect(errors).toHaveLength(1);
    });

    it('requires owner-id to be true rather than false', () => {
        expect(validate(docWithSchema({id: {'x-klabis-owner-id': false}}))).toHaveLength(1);
        expect(validate(docWithSchema({id: {'x-klabis-owner-id': true}}))).toEqual([]);
    });

    it('accepts x-hal-templates pointing at an existing operationId', () => {
        expect(validate({
            paths: {
                '/api/members/{id}': {
                    get: {
                        operationId: 'getMember',
                        responses: {'200': {'x-hal-templates': {default: {operation: 'updateMember'}}}},
                    },
                    patch: {operationId: 'updateMember', responses: {}},
                },
            },
        })).toEqual([]);
    });

    it('rejects x-hal-links pointing at a non-existent operationId', () => {
        const errors = validate({
            paths: {
                '/api/members': {
                    get: {
                        operationId: 'listMembers',
                        responses: {'200': {'x-hal-links': {self: {operation: 'nope'}}}},
                    },
                },
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('does not match any operationId');
    });

    it('allows x-hal-links entries without an operation reference', () => {
        expect(validate({
            paths: {
                '/api/members': {
                    get: {
                        operationId: 'listMembers',
                        responses: {'200': {'x-hal-links': {self: {description: 'This collection'}}}},
                    },
                },
            },
        })).toEqual([]);
    });
});

describe('validateSpec — x-klabis-authority on operations', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    const docWithOperation = (operationExtra) => ({
        paths: {
            '/api/members': {
                post: {
                    operationId: 'registerMember',
                    responses: {},
                    ...operationExtra,
                },
            },
        },
    });

    it('accepts a known authority directly on an operation', () => {
        expect(validate(docWithOperation({'x-klabis-authority': 'MEMBERS_MANAGE'}))).toEqual([]);
    });

    it('rejects an unknown authority on an operation', () => {
        const errors = validate(docWithOperation({'x-klabis-authority': 'NOPE'}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not a constant of Authority.java');
    });

    it('rejects x-klabis-owner-id on an operation', () => {
        const errors = validate(docWithOperation({'x-klabis-owner-id': true}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not valid on an operation');
    });

    it('rejects x-klabis-owner-visible on an operation', () => {
        const errors = validate(docWithOperation({'x-klabis-owner-visible': true}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not valid on an operation');
    });

    it('rejects x-klabis-halforms-access on an operation', () => {
        const errors = validate(docWithOperation({'x-klabis-halforms-access': 'READ_ONLY'}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not valid on an operation');
    });

    it('still accepts x-klabis-authority on a schema property alongside an operation-level one', () => {
        const doc = {
            paths: {
                '/api/members': {
                    post: {
                        operationId: 'registerMember',
                        responses: {},
                        'x-klabis-authority': 'MEMBERS_MANAGE',
                    },
                },
            },
            components: {
                schemas: {
                    Thing: {
                        type: 'object',
                        properties: {
                            dateOfBirth: {type: 'string', 'x-klabis-authority': 'MEMBERS_MANAGE'},
                        },
                    },
                },
            },
        };
        expect(validate(doc)).toEqual([]);
    });
});
