import {describe, expect, it} from 'vitest';

import {moduleFileNames, parseAuthorities, validateModuleDocuments, validateSpec} from '../lib/validate.mjs';

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

    it('requires past to be true rather than false', () => {
        expect(validate(docWithSchema({dateOfBirth: {'x-klabis-past': false}}))).toHaveLength(1);
        expect(validate(docWithSchema({dateOfBirth: {'x-klabis-past': true}}))).toEqual([]);
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

    // Two modules picking the same operationId reads fine in each file on its own, and the
    // consequence lands in the frontend build: haltypes.mjs names its exported types after the
    // operationId, so a collision emits duplicate TypeScript declarations.
    it('rejects an operationId declared by more than one operation', () => {
        const errors = validate({
            paths: {
                '/api/groups/{id}': {get: {operationId: 'getGroup', responses: {}}},
                '/api/membership-fee-groups/{id}': {get: {operationId: 'getGroup', responses: {}}},
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be unique');
        expect(errors[0].message).toContain('GET /api/groups/{id}');
        expect(errors[0].message).toContain('GET /api/membership-fee-groups/{id}');
    });

    it('allows the same operationId to appear once per document', () => {
        expect(validate({
            paths: {
                '/api/groups/{id}': {
                    get: {operationId: 'getGroup', responses: {}},
                    patch: {operationId: 'updateGroup', responses: {}},
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

    // The pair is split across two nodes: x-klabis-owner-visible: true on the operation
    // (-> @OwnerVisible, api.mustache) and x-klabis-owner-id: true on one of its parameters
    // (-> @OwnerId, pathParams.mustache). Neither template can see the other, so validation is
    // the only thing keeping them together — @OwnerVisible without @OwnerId makes
    // checkOwnership() deny instead of resolving ownership, silently dropping the
    // owner-or-authority semantics the endpoint advertises.
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

    const ownerParam = (extra) =>
        ({name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}, ...extra});

    it('accepts an operation whose parameter is marked x-klabis-owner-id', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': true},
            [ownerParam({'x-klabis-owner-id': true})],
        ));
        expect(errors).toEqual([]);
    });

    it('rejects x-klabis-owner-visible when no parameter carries the owner id', () => {
        const errors = validate(docWithParams({'x-klabis-owner-visible': true}, [ownerParam()]));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('exactly one parameter marked x-klabis-owner-id');
    });

    it('rejects x-klabis-owner-visible on an operation with no parameters at all', () => {
        const errors = validate(docWithParams({'x-klabis-owner-visible': true}, undefined));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('exactly one parameter marked x-klabis-owner-id');
    });

    it('rejects two owner-id parameters — findAnnotatedParameterIndex would take the first', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': true},
            [ownerParam({'x-klabis-owner-id': true}),
             {name: 'other', in: 'path', required: true, schema: {type: 'string'}, 'x-klabis-owner-id': true}],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('2 parameters are marked');
    });

    it('rejects an owner-id parameter that is not a path parameter', () => {
        // Only pathParams.mustache has a branch for the key, so anywhere else it is silently
        // dropped and @OwnerVisible is left with nothing to resolve against. This also covers
        // page/size/sort, which x-spring-paginated folds into Pageable — they are query params.
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': true},
            [{name: 'memberId', in: 'query', schema: {type: 'string'}, 'x-klabis-owner-id': true}],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('only generated for path parameters');
    });

    it('rejects a non-boolean x-klabis-owner-visible on an operation', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': 'id'},
            [ownerParam({'x-klabis-owner-id': true})],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be true when present');
    });

    it('resolves a $ref parameter when looking for the owner id', () => {
        const doc = {
            paths: {
                '/api/members/{id}': {
                    patch: {
                        operationId: 'updateMember',
                        responses: {},
                        'x-klabis-owner-visible': true,
                        parameters: [{$ref: '#/components/parameters/MemberIdParam'}],
                    },
                },
            },
            components: {
                parameters: {
                    MemberIdParam: {...ownerParam({'x-klabis-owner-id': true})},
                },
            },
        };
        expect(validate(doc)).toEqual([]);
    });

    it('allows an owner-id parameter shared with operations that never opt into ownership', () => {
        // This is what lets the annotation sit on a shared $ref instead of being inlined per
        // operation: @OwnerId is inert unless the method is also @OwnerVisible.
        const doc = {
            paths: {
                '/api/members/{id}': {
                    get: {
                        operationId: 'getMember',
                        responses: {},
                        parameters: [{$ref: '#/components/parameters/MemberIdParam'}],
                    },
                    patch: {
                        operationId: 'updateMember',
                        responses: {},
                        'x-klabis-owner-visible': true,
                        parameters: [{$ref: '#/components/parameters/MemberIdParam'}],
                    },
                },
            },
            components: {
                parameters: {
                    MemberIdParam: {...ownerParam({'x-klabis-owner-id': true})},
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

describe('validateModuleDocuments', () => {
    const SCHEMES = {KlabisAuth: {type: 'oauth2', flows: {authorizationCode: {scopes: {MEMBERS: 'Members'}}}}};
    const root = {
        openapi: '3.1.0',
        info: {title: 'Klabis', version: '0.1.0'},
        components: {securitySchemes: SCHEMES},
    };
    const module = (overrides = {}) => ({
        openapi: '3.1.0',
        info: {title: 'Klabis API — Members module', version: '0.1.0'},
        components: {securitySchemes: SCHEMES},
        ...overrides,
    });

    it('accepts a module matching the root', () => {
        expect(validateModuleDocuments(root, [{name: 'members.yaml', document: module()}])).toEqual([]);
    });

    it('allows the title to differ — only version, openapi and securitySchemes are pinned', () => {
        const doc = module({info: {title: 'Something else entirely', version: '0.1.0'}});
        expect(validateModuleDocuments(root, [{name: 'members.yaml', document: doc}])).toEqual([]);
    });

    it('rejects a drifted info.version', () => {
        const doc = module({info: {title: 'x', version: '0.2.0'}});
        const errors = validateModuleDocuments(root, [{name: 'members.yaml', document: doc}]);
        expect(errors).toHaveLength(1);
        expect(errors[0].path).toBe('members.yaml/info/version');
    });

    it('rejects a drifted openapi version', () => {
        const errors = validateModuleDocuments(root, [
            {name: 'members.yaml', document: module({openapi: '3.0.3'})},
        ]);
        expect(errors).toHaveLength(1);
        expect(errors[0].path).toBe('members.yaml/openapi');
    });

    it('rejects drifted securitySchemes', () => {
        const doc = module({
            components: {securitySchemes: {KlabisAuth: {type: 'http', scheme: 'bearer'}}},
        });
        const errors = validateModuleDocuments(root, [{name: 'members.yaml', document: doc}]);
        expect(errors).toHaveLength(1);
        expect(errors[0].path).toBe('members.yaml/components/securitySchemes');
    });

    // A module without the header at all is the pre-migration state, not a valid document — it
    // must fail rather than be waved through as "nothing to compare".
    it('rejects a module missing the header entirely', () => {
        const errors = validateModuleDocuments(root, [{name: 'members.yaml', document: {paths: {}}}]);
        expect(errors).toHaveLength(3);
    });
});

describe('moduleFileNames', () => {
    it('derives the module list from the paths klabis.yaml routes', () => {
        expect(moduleFileNames({
            paths: {
                '/api/members': {$ref: './members.yaml#/paths/~1api~1members'},
                '/api/members/{id}': {$ref: './members.yaml#/paths/~1api~1members~1{id}'},
                '/api/events': {$ref: './events.yaml#/paths/~1api~1events'},
            },
        })).toEqual(['events.yaml', 'members.yaml']);
    });

    // _shared/*.yaml hold components only and are pulled in by the modules, never routed to.
    it('excludes refs into a subdirectory', () => {
        expect(moduleFileNames({
            paths: {'/api/x': {$ref: './_shared/hal.yaml#/components/schemas/Link'}},
        })).toEqual([]);
    });

    // The point of deriving rather than globbing: a scratch file in the spec directory is not a
    // module, so it is never forced through the header check.
    it('ignores a file nothing routes to', () => {
        expect(moduleFileNames({paths: {'/api/x': {$ref: './members.yaml#/paths/~1api~1x'}}}))
            .toEqual(['members.yaml']);
    });

    it('tolerates a document with no paths', () => {
        expect(moduleFileNames({})).toEqual([]);
    });
});
