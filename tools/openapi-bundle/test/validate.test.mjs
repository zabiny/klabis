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

    it('requires not-blank to be true rather than false', () => {
        expect(validate(docWithSchema({name: {'x-klabis-not-blank': false}}))).toHaveLength(1);
        expect(validate(docWithSchema({name: {'x-klabis-not-blank': true}}))).toEqual([]);
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

    it('rejects x-klabis-not-blank on an operation', () => {
        expect(validate(docWithOperation({'x-klabis-not-blank': true}))).toHaveLength(1);
    });

    it('rejects x-klabis-not-blank on a parameter schema, where the generator would drop it', () => {
        const errors = validate(docWithOperation({
            parameters: [{name: 'token', in: 'query', schema: {type: 'string', 'x-klabis-not-blank': true}}],
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not honoured on a parameter');
    });

    it('rejects x-klabis-owner-id on an operation', () => {
        const errors = validate(docWithOperation({'x-klabis-owner-id': true}));
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

describe('validateSpec — x-klabis-owner-visible on operations', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    // x-klabis-owner-visible on an operation names the parameter that carries the owner ID.
    // This is the only shape that guarantees @OwnerVisible can never be generated without a
    // matching @OwnerId — the bundler resolves the name against the operation's own parameters,
    // so a typo or missing parameter is a validation/bundle error rather than a silently
    // incomplete pair.
    const docWithParams = (operationExtra, parameters) => ({
        paths: {
            '/api/members/{id}': {
                patch: {
                    operationId: 'updateMember',
                    responses: {},
                    parameters,
                    ...operationExtra,
                },
            },
        },
    });

    it('accepts x-klabis-owner-visible naming an existing path parameter', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': 'id'},
            [{name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}}],
        ));
        expect(errors).toEqual([]);
    });

    it('rejects x-klabis-owner-visible naming a parameter the operation does not declare', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': 'memberId'},
            [{name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}}],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('"memberId"');
        expect(errors[0].message).toContain('does not match any parameter');
    });

    it('rejects x-klabis-owner-visible on an operation with no parameters at all', () => {
        const errors = validate(docWithParams({'x-klabis-owner-visible': 'id'}, undefined));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('does not match any parameter');
    });

    it('rejects a non-string x-klabis-owner-visible value on an operation', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': true},
            [{name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}}],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be the name of a parameter');
    });

    it('resolves a $ref parameter to check the name', () => {
        const doc = {
            paths: {
                '/api/members/{id}': {
                    patch: {
                        operationId: 'updateMember',
                        responses: {},
                        'x-klabis-owner-visible': 'id',
                        parameters: [{$ref: '#/components/parameters/MemberIdParam'}],
                    },
                },
            },
            components: {
                parameters: {
                    MemberIdParam: {name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}},
                },
            },
        };
        expect(validate(doc)).toEqual([]);
    });

    it('still requires x-klabis-owner-visible to be true on a schema property (field-level case unchanged)', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {
                    Thing: {type: 'object', properties: {email: {type: 'string', 'x-klabis-owner-visible': true}}},
                },
            },
        });
        expect(errors).toEqual([]);
    });

    it('rejects x-klabis-owner-visible=false on a schema property', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {
                    Thing: {type: 'object', properties: {email: {type: 'string', 'x-klabis-owner-visible': false}}},
                },
            },
        });
        expect(errors).toHaveLength(1);
    });
});
