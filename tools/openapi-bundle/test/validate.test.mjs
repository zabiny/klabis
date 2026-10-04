import {describe, expect, it} from 'vitest';

import {moduleFileNames, parseAuthorities, validateModuleDocuments, validateSpec} from '../lib/validate.mjs';

const AUTHORITY_JAVA = `
package com.klabis.common.users;

public enum Authority {
    CALENDAR_MANAGE("CALENDAR:MANAGE", TargetType.NONE, GrantForm.ALL),
    MEMBERS_MANAGE("MEMBERS:MANAGE", TargetType.MEMBER, GrantForm.ALL),
    EVENTS_MANAGE("EVENTS:MANAGE", TargetType.EVENT, GrantForm.ALL),
    MEMBERS_EDIT_PROFILE("MEMBERS:EDIT_PROFILE", TargetType.MEMBER, GrantForm.ALL, GrantForm.SPECIFIC),
    FINANCE_MANAGE("FINANCE:MANAGE", TargetType.NONE, GrantForm.ALL);

    public static final String MEMBERS_SCOPE = "MEMBERS";
    public static final String EVENTS_SCOPE = "EVENTS";
}
`;

describe('parseAuthorities', () => {
    it('extracts enum constants', () => {
        expect([...parseAuthorities(AUTHORITY_JAVA).keys()])
            .toEqual(['CALENDAR_MANAGE', 'MEMBERS_MANAGE', 'EVENTS_MANAGE', 'MEMBERS_EDIT_PROFILE', 'FINANCE_MANAGE']);
    });

    it('reads the target type and grant forms of each constant', () => {
        const authorities = parseAuthorities(AUTHORITY_JAVA);
        expect(authorities.get('MEMBERS_MANAGE'))
            .toEqual({targetType: 'MEMBER', grantForms: new Set(['ALL'])});
        expect(authorities.get('MEMBERS_EDIT_PROFILE'))
            .toEqual({targetType: 'MEMBER', grantForms: new Set(['ALL', 'SPECIFIC'])});
        expect(authorities.get('CALENDAR_MANAGE').targetType).toBe('NONE');
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
            id: {type: 'string', 'x-klabis-target-idd': true},
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

    it('rejects the removed x-klabis-owner-id and points at its replacement', () => {
        const errors = validate(docWithSchema({id: {'x-klabis-owner-id': true}}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('x-klabis-target-id');
    });

    it('accepts a target type on a schema property', () => {
        expect(validate(docWithSchema({id: {'x-klabis-target-id': 'MEMBER'}}))).toEqual([]);
    });

    it('rejects an unknown target type, and NONE which names no target', () => {
        expect(validate(docWithSchema({id: {'x-klabis-target-id': 'GROUP'}}))).toHaveLength(1);
        expect(validate(docWithSchema({id: {'x-klabis-target-id': 'NONE'}}))).toHaveLength(1);
        expect(validate(docWithSchema({id: {'x-klabis-target-id': true}}))).toHaveLength(1);
    });

    it('accepts a list of known authorities', () => {
        expect(validate(docWithSchema({
            email: {type: 'string', 'x-klabis-authority': ['MEMBERS_MANAGE', 'MEMBERS_EDIT_PROFILE']},
        }))).toEqual([]);
    });

    it('reports each unknown authority in a list and rejects an empty or non-string list', () => {
        const errors = validate(docWithSchema({
            email: {type: 'string', 'x-klabis-authority': ['MEMBERS_MANAGE', 'NOPE', 'NADA']},
        }));
        expect(errors).toHaveLength(2);
        expect(validate(docWithSchema({email: {'x-klabis-authority': []}}))).toHaveLength(1);
        expect(validate(docWithSchema({email: {'x-klabis-authority': [1]}}))).toHaveLength(1);
        expect(validate(docWithSchema({email: {'x-klabis-authority': true}}))).toHaveLength(1);
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

    it('rejects x-klabis-target-id on an operation', () => {
        const errors = validate(docWithOperation({'x-klabis-target-id': 'MEMBER'}));
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
    // (-> @OwnerVisible, api.mustache) and x-klabis-target-id: MEMBER on one of its parameters
    // (-> @TargetId, pathParams.mustache). Neither template can see the other, so validation is
    // the only thing keeping them together — @OwnerVisible without @TargetId makes
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

    it('accepts an operation whose parameter is marked x-klabis-target-id', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': true},
            [ownerParam({'x-klabis-target-id': 'MEMBER'})],
        ));
        expect(errors).toEqual([]);
    });

    it('rejects x-klabis-owner-visible when no parameter carries the owner id', () => {
        const errors = validate(docWithParams({'x-klabis-owner-visible': true}, [ownerParam()]));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('exactly one parameter marked x-klabis-target-id');
    });

    it('rejects x-klabis-owner-visible on an operation with no parameters at all', () => {
        const errors = validate(docWithParams({'x-klabis-owner-visible': true}, undefined));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('exactly one parameter marked x-klabis-target-id');
    });

    it('rejects two target-id parameters — findAnnotatedParameterIndex would take the first', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': true},
            [ownerParam({'x-klabis-target-id': 'MEMBER'}),
             {name: 'other', in: 'path', required: true, schema: {type: 'string'}, 'x-klabis-target-id': 'MEMBER'}],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('2 parameters are marked');
    });

    it('rejects an target-id parameter that is not a path parameter', () => {
        // Only pathParams.mustache has a branch for the key, so anywhere else it is silently
        // dropped and @OwnerVisible is left with nothing to resolve against. This also covers
        // page/size/sort, which x-spring-paginated folds into Pageable — they are query params.
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': true},
            [{name: 'memberId', in: 'query', schema: {type: 'string'}, 'x-klabis-target-id': 'MEMBER'}],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('only generated for path parameters');
    });

    it('rejects a non-boolean x-klabis-owner-visible on an operation', () => {
        const errors = validate(docWithParams(
            {'x-klabis-owner-visible': 'id'},
            [ownerParam({'x-klabis-target-id': 'MEMBER'})],
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
                    MemberIdParam: {...ownerParam({'x-klabis-target-id': 'MEMBER'})},
                },
            },
        };
        expect(validate(doc)).toEqual([]);
    });

    it('allows an target-id parameter shared with operations that never opt into ownership', () => {
        // This is what lets the annotation sit on a shared $ref instead of being inlined per
        // operation: @TargetId is inert unless the method is also @OwnerVisible.
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
                    MemberIdParam: {...ownerParam({'x-klabis-target-id': 'MEMBER'})},
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

describe('validateSpec — x-klabis-hal', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    const docWithOperation = (operationExtra) => ({
        paths: {
            '/api/oris/events': {
                get: {operationId: 'listOrisEvents', responses: {}, ...operationExtra},
            },
        },
    });

    it('accepts x-klabis-hal: false on an operation', () => {
        expect(validate(docWithOperation({'x-klabis-hal': false}))).toEqual([]);
    });

    it('rejects x-klabis-hal: true — it is an opt-out, not an opt-in', () => {
        const errors = validate(docWithOperation({'x-klabis-hal': true}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be false when present');
    });

    it('rejects x-klabis-hal on a schema property, where the deriver never reads it', () => {
        const errors = validate({
            paths: {},
            components: {schemas: {Thing: {type: 'object', 'x-klabis-hal': false}}},
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('only valid on an operation');
    });
});

describe('validateSpec — x-hal-embedded', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const docWithEmbedded = (embedded) => ({
        paths: {
            '/api/groups/{id}': {
                get: {
                    operationId: 'getFeeGroup',
                    responses: {
                        '200': {
                            description: 'ok',
                            'x-hal-embedded': embedded,
                            content: {
                                'application/json': {
                                    schema: {$ref: '#/components/schemas/MembershipFeeGroupResponse'},
                                },
                            },
                        },
                    },
                },
            },
        },
        components: {
            schemas: {
                MembershipFeeGroupResponse: {type: 'object', properties: {}},
                MemberInGroupResponse: {type: 'object', properties: {}},
            },
        },
    });
    const validate = (doc) => validateSpec(doc, {authorities});
    const valid = {items: 'MemberInGroupResponse', suffix: 'WithMembers'};

    it('accepts a complete marker', () => {
        expect(validate(docWithEmbedded(valid))).toEqual([]);
    });

    it.each(['items', 'suffix'])('rejects a marker missing %s', (field) => {
        const {[field]: _omitted, ...incomplete} = valid;
        const errors = validate(docWithEmbedded(incomplete));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain(`${field} is required`);
    });

    it('rejects items naming a schema that does not exist', () => {
        const errors = validate(docWithEmbedded({...valid, items: 'NoSuchSchema'}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('does not name a schema');
    });

    it('rejects a non-object marker', () => {
        const errors = validate(docWithEmbedded('members'));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be an object');
    });

    it('rejects a suffix that is not PascalCase', () => {
        const errors = validate(docWithEmbedded({...valid, suffix: 'withMembers'}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('PascalCase');
    });

    // The deriver only visits 2xx responses carrying an application/json schema; a marker anywhere
    // else is silently ignored, which looks live but does nothing.
    it('rejects a marker on a response the deriver never visits', () => {
        const doc = docWithEmbedded(valid);
        delete doc.paths['/api/groups/{id}'].get.responses['200'].content['application/json'];
        const errors = validate(doc);
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('application/json schema');
    });
});

describe('validateSpec — x-hal-entity-items', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const docWithProperty = (property, extraSchemas = {}) => ({
        paths: {},
        components: {
            schemas: {
                TrainingGroupResponse: {type: 'object', properties: {trainers: property}},
                TrainerResponse: {type: 'object', properties: {}},
                ...extraSchemas,
            },
        },
    });
    const validate = (doc) => validateSpec(doc, {authorities});

    it('accepts the marker on an array whose items are a $ref', () => {
        expect(validate(docWithProperty({
            type: 'array',
            'x-hal-entity-items': true,
            items: {$ref: '#/components/schemas/TrainerResponse'},
        }))).toEqual([]);
    });

    it('rejects a value other than true', () => {
        const errors = validate(docWithProperty({
            type: 'array',
            'x-hal-entity-items': false,
            items: {$ref: '#/components/schemas/TrainerResponse'},
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be true when present');
    });

    it('rejects the marker on a non-array schema', () => {
        const errors = validate(docWithProperty({
            type: 'object',
            'x-hal-entity-items': true,
            properties: {id: {type: 'string'}},
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('type: array');
    });

    it('rejects an array whose items are inline rather than a $ref', () => {
        const errors = validate(docWithProperty({
            type: 'array',
            'x-hal-entity-items': true,
            items: {type: 'string'},
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('$ref to a payload schema');
    });

    it('rejects items already shaped as a HAL envelope', () => {
        const errors = validate(docWithProperty({
            type: 'array',
            'x-hal-entity-items': true,
            items: {$ref: '#/components/schemas/EntityModelTrainerResponse'},
        }, {
            EntityModelTrainerResponse: {
                allOf: [
                    {$ref: '#/components/schemas/TrainerResponse'},
                    {type: 'object', properties: {_links: {$ref: '#/components/schemas/Links'}}},
                ],
            },
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('already shaped as a HAL envelope');
    });
});

describe('validateSpec — x-klabis-nullable', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const docWithProperty = (property) => ({
        paths: {},
        components: {
            schemas: {
                UpdateThingRequest: {type: 'object', properties: {name: property}},
                ThingName: {type: 'object', properties: {}},
            },
        },
    });
    const validate = (doc) => validateSpec(doc, {authorities});

    it('accepts true on a $ref property — the deriver consumes it before validation', () => {
        expect(validate(docWithProperty({
            $ref: '#/components/schemas/ThingName',
            'x-klabis-nullable': true,
        }))).toEqual([]);
    });

    it('accepts false beside a nullable type array', () => {
        expect(validate(docWithProperty({
            type: ['string', 'null'],
            'x-klabis-nullable': false,
        }))).toEqual([]);
    });

    it('rejects a non-boolean value', () => {
        const errors = validate(docWithProperty({'x-klabis-nullable': 'true'}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be a boolean');
    });

    it('rejects the directive beside oneOf — composition would strip it or double-declare', () => {
        const errors = validate(docWithProperty({
            oneOf: [{$ref: '#/components/schemas/ThingName'}, {type: 'null'}],
            'x-klabis-nullable': true,
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('oneOf');
    });

    it('rejects the directive beside allOf for the same reason', () => {
        const errors = validate(docWithProperty({
            allOf: [{$ref: '#/components/schemas/ThingName'}],
            'x-klabis-nullable': false,
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('allOf');
    });

    it('rejects true where the type array already declares null — redundant dual declaration', () => {
        const errors = validate(docWithProperty({
            type: ['string', 'null'],
            'x-klabis-nullable': true,
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('already declares');
    });

    it('rejects true on a property that is not a $ref', () => {
        const errors = validate(docWithProperty({type: 'string', 'x-klabis-nullable': true}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('$ref');
    });

    it('rejects false when the type does not contain null — there is nothing to narrow', () => {
        const errors = validate(docWithProperty({type: 'string', 'x-klabis-nullable': false}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain("'null'");
    });

    it('rejects a survivor inside a composition subtree — the codegen would not match the bundle', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {
                    ComposedThing: {
                        allOf: [
                            {$ref: '#/components/schemas/ThingName', 'x-klabis-nullable': true},
                        ],
                    },
                    ThingName: {type: 'object', properties: {}},
                },
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('composition');
    });

    it('rejects the directive on a bare schema, where the codegen never reads it', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {Thing: {$ref: '#/components/schemas/ThingName', 'x-klabis-nullable': true}},
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('only honoured on a schema property');
    });

    it('rejects x-klabis-nullable: true on a required member — JsonNullable would miss its import', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {
                    CreateThingRequest: {
                        type: 'object',
                        required: ['name'],
                        properties: {
                            name: {$ref: '#/components/schemas/ThingName', 'x-klabis-nullable': true},
                        },
                    },
                    ThingName: {type: 'object', properties: {}},
                },
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('required property');
    });

    it('accepts x-klabis-nullable: false on a required member — it removes a wrapper, not adds one', () => {
        expect(validate({
            paths: {},
            components: {
                schemas: {
                    CreateThingRequest: {
                        type: 'object',
                        required: ['name'],
                        properties: {name: {type: ['string', 'null'], 'x-klabis-nullable': false}},
                    },
                },
            },
        })).toEqual([]);
    });

    it('rejects the directive on an operation, where the codegen never reads it', () => {
        const errors = validate({
            paths: {
                '/api/things': {
                    patch: {operationId: 'updateThing', responses: {}, 'x-klabis-nullable': true},
                },
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not valid on an operation');
    });
});

describe('validateSpec — x-hal-input-type', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const docWithProperty = (property) => ({
        paths: {},
        components: {
            schemas: {
                UpdateThingRequest: {type: 'object', properties: {name: property}},
                ThingName: {type: 'object', properties: {}},
            },
        },
    });
    const validate = (doc) => validateSpec(doc, {authorities});

    it('accepts a non-empty string input type', () => {
        expect(validate(docWithProperty({type: 'string', 'x-hal-input-type': 'textarea'})))
            .toEqual([]);
    });

    it('rejects an empty string', () => {
        const errors = validate(docWithProperty({type: 'string', 'x-hal-input-type': ''}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be a non-empty string');
    });

    it('rejects a non-string value', () => {
        const errors = validate(docWithProperty({type: 'string', 'x-hal-input-type': 42}));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('must be a non-empty string');
    });

    it('rejects the marker beside oneOf, where composition would silently lose it', () => {
        const errors = validate(docWithProperty({
            oneOf: [{$ref: '#/components/schemas/ThingName'}, {type: 'null'}],
            'x-hal-input-type': 'RankingRequest',
        }));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('oneOf');
    });

    it('rejects a survivor inside a composition subtree for the same reason', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {
                    ComposedThing: {
                        allOf: [
                            {type: 'object', properties: {name: {type: 'string', 'x-hal-input-type': 'textarea'}}},
                        ],
                    },
                },
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('oneOf/allOf');
    });

    it('rejects the marker on a bare schema, where the generator never assembles the annotation', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {Thing: {type: 'object', 'x-hal-input-type': 'textarea'}},
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('only honoured on a schema property');
    });
});

describe('validateSpec — unknown x-hal extensions', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    it('rejects a typo in the family the way the x-klabis-* loop rejects its own', () => {
        const errors = validate({
            paths: {},
            components: {
                schemas: {
                    Thing: {type: 'object', properties: {name: {type: 'string', 'x-hal-inputtype': 'textarea'}}},
                },
            },
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('Unknown extension "x-hal-inputtype"');
        expect(errors[0].message).toContain('x-hal-input-type');
    });

    it('accepts every known x-hal key in the shapes the other suites already pin', () => {
        expect(validate({
            paths: {
                '/api/things': {
                    get: {
                        operationId: 'listThings',
                        responses: {
                            '200': {
                                description: 'ok',
                                'x-hal-links': {self: {operation: 'listThings'}},
                                'x-hal-templates': {default: {operation: 'createThing'}},
                                'x-hal-embedded': {items: 'ThingItem', suffix: 'WithItems'},
                                content: {
                                    'application/json': {schema: {$ref: '#/components/schemas/ThingResponse'}},
                                },
                            },
                        },
                    },
                    post: {operationId: 'createThing', responses: {}},
                },
            },
            components: {
                schemas: {
                    ThingResponse: {
                        type: 'object',
                        properties: {
                            rows: {
                                type: 'array',
                                'x-hal-entity-items': true,
                                items: {$ref: '#/components/schemas/ThingItem'},
                            },
                            name: {type: 'string', 'x-hal-input-type': 'textarea'},
                        },
                    },
                    ThingItem: {type: 'object', properties: {}},
                },
            },
        })).toEqual([]);
    });
});

describe('validateSpec — x-field-extra-annotation', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const docWithProperty = (value) => ({
        paths: {},
        components: {
            schemas: {
                CancelThingRequest: {
                    type: 'object',
                    properties: {reason: {type: 'string', 'x-field-extra-annotation': value}},
                },
            },
        },
    });
    const validate = (doc) => validateSpec(doc, {authorities});

    it('rejects a hand-written HalForms annotation spelled the way the specs write it', () => {
        const errors = validate(docWithProperty('com.klabis.common.ui.HalForms(formInputType = "textarea")'));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('@HalForms');
        expect(errors[0].message).toContain('x-hal-input-type');
    });

    it('rejects the @-prefixed spelling too', () => {
        const errors = validate(docWithProperty('@HalForms(formInputType = "textarea")'));
        expect(errors).toHaveLength(1);
    });

    it('accepts extra annotations that are not HalForms', () => {
        expect(validate(docWithProperty(
            'com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)',
        ))).toEqual([]);
        expect(validate(docWithProperty('com.fasterxml.jackson.annotation.JsonIgnore'))).toEqual([]);
    });

    it('does not mistake an annotation whose name merely contains HalForms for the real one', () => {
        expect(validate(docWithProperty('com.example.UseHalFormsAround(msg = "x")'))).toEqual([]);
    });
});

describe('validateSpec — payload schema mistaken for an envelope', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const LINKS = {$ref: '#/components/schemas/Links'};
    const validate = (doc) => validateSpec(doc, {authorities});

    const docWith = (responseContent, schemas, operation = {}) => ({
        paths: {
            '/api/things': {
                get: {
                    operationId: 'listThings',
                    ...operation,
                    responses: {'200': {content: responseContent}},
                },
            },
        },
        components: {schemas},
    });

    const jsonOnly = (name) => ({'application/json': {schema: {$ref: `#/components/schemas/${name}`}}});

    it('flags a payload declaring _links, which the deriver silently skips', () => {
        const errors = validate(docWith(jsonOnly('ThingResponse'), {
            ThingResponse: {type: 'object', properties: {_links: LINKS, id: {type: 'string'}}},
        }));

        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('no hal-forms media type');
        expect(errors[0].path).toBe('/components/schemas/ThingResponse');
    });

    it('flags an array item payload declaring _embedded', () => {
        const errors = validate(docWith(
            {'application/json': {schema: {type: 'array', items: {$ref: '#/components/schemas/ThingItem'}}}},
            {ThingItem: {type: 'object', properties: {_embedded: {type: 'object'}}}},
        ));

        expect(errors).toHaveLength(1);
        expect(errors[0].path).toBe('/components/schemas/ThingItem');
    });

    it('accepts a plain payload', () => {
        expect(validate(docWith(jsonOnly('ThingResponse'), {
            ThingResponse: {type: 'object', properties: {id: {type: 'string'}}},
        }))).toEqual([]);
    });

    it('accepts a hand-written envelope served through its own hal-forms entry', () => {
        expect(validate(docWith({
            'application/json': {schema: {$ref: '#/components/schemas/RootModel'}},
            'application/prs.hal-forms+json': {schema: {$ref: '#/components/schemas/EntityModelRootModel'}},
        }, {
            RootModel: {type: 'object', properties: {_links: LINKS}},
            EntityModelRootModel: {type: 'object', properties: {_links: LINKS}},
        }))).toEqual([]);
    });

    it('accepts an envelope-shaped payload on an operation opted out of HAL', () => {
        expect(validate(docWith(jsonOnly('ThingResponse'), {
            ThingResponse: {type: 'object', properties: {_links: LINKS}},
        }, {'x-klabis-hal': false}))).toEqual([]);
    });
});

describe('validateSpec — HAL envelope base models', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    const ENTITY_MODEL = {
        type: 'object',
        properties: {_links: {type: 'object'}, _templates: {type: 'object'}},
    };
    const derivedDoc = (schemas) => ({paths: {}, components: {schemas}});

    it('flags a derived EntityModel envelope when the shared base was not hoisted', () => {
        const errors = validate(derivedDoc({
            CollectionModel: ENTITY_MODEL,
            PagedModel: {allOf: [{$ref: '#/components/schemas/CollectionModel'}]},
            EntityModelThingResponse: {
                allOf: [
                    {$ref: '#/components/schemas/ThingResponse'},
                    {$ref: '#/components/schemas/EntityModel'},
                ],
            },
        }));

        expect(errors).toHaveLength(1);
        expect(errors[0].path).toBe('/components/schemas/EntityModel');
        expect(errors[0].message).toContain('not hoisted from');
    });

    it('flags only the base models actually referenced by a derived envelope', () => {
        const errors = validate(derivedDoc({
            CollectionModelEntityModelThing: {
                allOf: [
                    {$ref: '#/components/schemas/CollectionModel'},
                    {type: 'object', properties: {_embedded: {type: 'object'}}},
                ],
            },
        }));

        expect(errors.map((e) => e.path)).toEqual(['/components/schemas/CollectionModel']);
    });

    it('accepts the same envelope once the base models are present', () => {
        expect(validate(derivedDoc({
            EntityModel: ENTITY_MODEL,
            CollectionModel: ENTITY_MODEL,
            PagedModel: {allOf: [{$ref: '#/components/schemas/CollectionModel'}]},
            EntityModelThingResponse: {
                allOf: [
                    {$ref: '#/components/schemas/ThingResponse'},
                    {$ref: '#/components/schemas/EntityModel'},
                ],
            },
        }))).toEqual([]);
    });

    it('says nothing when no derived envelope is present', () => {
        expect(validate(derivedDoc({
            ThingResponse: {type: 'object', properties: {id: {type: 'string'}}},
        }))).toEqual([]);
    });
});

describe('validateSpec — authorities held over specific targets', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    const docWithOperation = (operationExtra, parameters) => ({
        paths: {
            '/api/members/{id}': {
                patch: {operationId: 'updateMember', responses: {}, parameters, ...operationExtra},
            },
        },
    });
    const idParam = (extra) =>
        ({name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}, ...extra});

    it('accepts a SPECIFIC authority on an operation whose target parameter has the authority\'s type', () => {
        expect(validate(docWithOperation(
            {'x-klabis-authority': ['MEMBERS_MANAGE', 'MEMBERS_EDIT_PROFILE']},
            [idParam({'x-klabis-target-id': 'MEMBER'})],
        ))).toEqual([]);
    });

    it('rejects a SPECIFIC authority on an operation with no target parameter', () => {
        const errors = validate(docWithOperation({'x-klabis-authority': 'MEMBERS_EDIT_PROFILE'}, [idParam()]));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('exactly one parameter marked x-klabis-target-id: MEMBER');
    });

    it('rejects a SPECIFIC authority whose target type differs from the parameter\'s', () => {
        const errors = validate(docWithOperation(
            {'x-klabis-authority': 'MEMBERS_EDIT_PROFILE'},
            [idParam({'x-klabis-target-id': 'EVENT'})],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('is about MEMBER targets');
    });

    it('rejects a SPECIFIC authority when two parameters name a target', () => {
        const errors = validate(docWithOperation(
            {'x-klabis-authority': 'MEMBERS_EDIT_PROFILE'},
            [idParam({'x-klabis-target-id': 'MEMBER'}), idParam({name: 'other', 'x-klabis-target-id': 'MEMBER'})],
        ));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('found 2');
    });

    it('does not require a target parameter for an {ALL}-only authority', () => {
        expect(validate(docWithOperation({'x-klabis-authority': 'MEMBERS_MANAGE'}, [idParam()]))).toEqual([]);
    });

    it('does not require a mismatching type for an {ALL}-only authority beside a target parameter', () => {
        expect(validate(docWithOperation(
            {'x-klabis-authority': 'EVENTS_MANAGE'},
            [idParam({'x-klabis-target-id': 'MEMBER'})],
        ))).toEqual([]);
    });

    it('resolves a $ref target parameter', () => {
        const doc = docWithOperation(
            {'x-klabis-authority': 'MEMBERS_EDIT_PROFILE'},
            [{$ref: '#/components/parameters/MemberIdParam'}],
        );
        doc.components = {parameters: {MemberIdParam: idParam({'x-klabis-target-id': 'MEMBER'})}};
        expect(validate(doc)).toEqual([]);
    });

    it('rejects x-klabis-target-id on an operation', () => {
        const errors = validate(docWithOperation({'x-klabis-target-id': 'MEMBER'}, undefined));
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not valid on an operation');
    });
});

describe('validateSpec — x-klabis-read-authority', () => {
    const authorities = parseAuthorities(AUTHORITY_JAVA);
    const validate = (doc) => validateSpec(doc, {authorities});

    const docWithRequestSchema = (properties, {nested = false} = {}) => ({
        paths: {
            '/api/members/{id}': {
                patch: {
                    operationId: 'updateMember',
                    responses: {},
                    requestBody: {
                        content: {'application/json': {schema: {$ref: '#/components/schemas/UpdateMemberRequest'}}},
                    },
                },
                get: {
                    operationId: 'getMember',
                    responses: {'200': {content: {'application/json': {schema: {$ref: '#/components/schemas/MemberDetails'}}}}},
                },
            },
        },
        components: {
            schemas: {
                UpdateMemberRequest: nested
                    ? {type: 'object', properties: {inner: {$ref: '#/components/schemas/Inner'}}}
                    : {type: 'object', properties},
                Inner: {type: 'object', properties},
                MemberDetails: {type: 'object', properties},
            },
        },
    });

    it('accepts it on a request schema property', () => {
        const doc = docWithRequestSchema({});
        doc.components.schemas.UpdateMemberRequest.properties = {
            email: {type: 'string', 'x-klabis-authority': 'MEMBERS_MANAGE', 'x-klabis-read-authority': ['MEMBERS_MANAGE']},
        };
        doc.components.schemas.MemberDetails.properties = {};
        doc.components.schemas.Inner.properties = {};
        expect(validate(doc)).toEqual([]);
    });

    it('accepts it on a schema reached from a request body through $ref', () => {
        const doc = docWithRequestSchema({email: {type: 'string', 'x-klabis-read-authority': ['MEMBERS_MANAGE']}}, {nested: true});
        doc.components.schemas.MemberDetails.properties = {};
        expect(validate(doc)).toEqual([]);
    });

    it('rejects it on a schema only responses use', () => {
        const doc = docWithRequestSchema({});
        doc.components.schemas.UpdateMemberRequest.properties = {};
        doc.components.schemas.MemberDetails.properties = {
            email: {type: 'string', 'x-klabis-read-authority': ['MEMBERS_MANAGE']},
        };
        const errors = validate(doc);
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('only valid on a request schema property');
    });

    it('accepts it on an inline request body schema', () => {
        expect(validate({
            paths: {
                '/api/x': {
                    post: {
                        operationId: 'x',
                        responses: {},
                        requestBody: {
                            content: {'application/json': {schema: {type: 'object', properties: {
                                email: {type: 'string', 'x-klabis-read-authority': ['MEMBERS_MANAGE']},
                            }}}},
                        },
                    },
                },
            },
        })).toEqual([]);
    });

    it('rejects an unknown authority in the list', () => {
        const doc = docWithRequestSchema({});
        doc.components.schemas.UpdateMemberRequest.properties = {
            email: {type: 'string', 'x-klabis-read-authority': ['NOPE']},
        };
        doc.components.schemas.MemberDetails.properties = {};
        doc.components.schemas.Inner.properties = {};
        const errors = validate(doc);
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not a constant of Authority.java');
    });

    it('rejects it on an operation', () => {
        const errors = validate({
            paths: {'/api/x': {get: {operationId: 'x', responses: {}, 'x-klabis-read-authority': ['MEMBERS_MANAGE']}}},
        });
        expect(errors).toHaveLength(1);
        expect(errors[0].message).toContain('not valid on an operation');
    });
});
