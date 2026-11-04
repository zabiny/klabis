import {describe, expect, it} from 'vitest';

import {collectHalResources, renderHalTypes} from '../lib/haltypes.mjs';

const memberDocument = (response) => ({
    paths: {
        '/api/members/{id}': {
            get: {
                operationId: 'getMember',
                responses: {
                    '200': {
                        description: 'Member found',
                        content: {
                            'application/prs.hal-forms+json': {
                                schema: {$ref: '#/components/schemas/MemberDetailsResponse'},
                            },
                        },
                        ...response,
                    },
                },
            },
        },
    },
});

describe('collectHalResources', () => {
    it('ignores an operation without x-hal-* declarations', () => {
        expect(collectHalResources(memberDocument())).toEqual([]);
    });

    it('collects an operation declaring only x-hal-links', () => {
        const resources = collectHalResources(memberDocument({
            'x-hal-links': {self: {operation: 'getMember'}},
        }));

        expect(resources).toHaveLength(1);
        expect(resources[0].links).toEqual(['self']);
        expect(resources[0].templates).toEqual([]);
    });

    it('collects an operation declaring only x-hal-templates', () => {
        const resources = collectHalResources(memberDocument({
            'x-hal-templates': {default: {operation: 'updateMember'}},
        }));

        expect(resources).toHaveLength(1);
        expect(resources[0].links).toEqual([]);
        expect(resources[0].templates).toEqual(['default']);
    });

    it('sorts resources by operationId so the generated file is stable', () => {
        const resources = collectHalResources({
            paths: {
                '/api/members': {
                    get: {
                        operationId: 'listMembers',
                        responses: {'200': {'x-hal-links': {self: {}}}},
                    },
                    post: {
                        operationId: 'createMember',
                        responses: {'201': {'x-hal-links': {self: {}}}},
                    },
                },
                '/api/events': {
                    get: {
                        operationId: 'getEvents',
                        responses: {'200': {'x-hal-links': {self: {}}}},
                    },
                },
            },
        });

        expect(resources.map((r) => r.operationId)).toEqual(['createMember', 'getEvents', 'listMembers']);
    });

    it('sorts rel names within an operation', () => {
        const resources = collectHalResources(memberDocument({
            'x-hal-links': {self: {}, 'ical-token': {}, edit: {}},
            'x-hal-templates': {suspend: {}, default: {}},
        }));

        expect(resources[0].links).toEqual(['edit', 'ical-token', 'self']);
        expect(resources[0].templates).toEqual(['default', 'suspend']);
    });

    it('picks the schema name out of the response content', () => {
        const resources = collectHalResources(memberDocument({'x-hal-links': {self: {}}}));

        expect(resources[0].schema).toBe('MemberDetailsResponse');
    });

    it('prefers the HAL content type schema over an application/json sibling', () => {
        // application/json is the backend-codegen sibling (klabis-api-spec skill); *Resource must
        // still type off the envelope schema (EntityModel*/PagedModel*/CollectionModel*), which
        // carries _links/_embedded/page — the bare payload schema does not. Bundled documents sort
        // content keys alphabetically, so "application/json" always precedes the HAL content type;
        // without an explicit preference this regressed silently across the members/event-types/
        // finance/events module migrations.
        const document = {
            paths: {
                '/api/members': {
                    get: {
                        operationId: 'listMembers',
                        responses: {
                            '200': {
                                content: {
                                    'application/json': {schema: {$ref: '#/components/schemas/MemberSummaryResponseList'}},
                                    'application/prs.hal-forms+json': {schema: {$ref: '#/components/schemas/PagedModelEntityModelMemberSummaryResponse'}},
                                },
                                'x-hal-links': {self: {}},
                            },
                        },
                    },
                },
            },
        };

        expect(collectHalResources(document)[0].schema).toBe('PagedModelEntityModelMemberSummaryResponse');
    });

    it('falls back to any content schema when no HAL content type carries one', () => {
        const document = {
            paths: {
                '/api/members': {
                    get: {
                        operationId: 'listMembers',
                        responses: {
                            '200': {
                                content: {
                                    'application/json': {schema: {$ref: '#/components/schemas/MemberSummaryResponseList'}},
                                    'application/prs.hal-forms+json': {},
                                },
                                'x-hal-links': {self: {}},
                            },
                        },
                    },
                },
            },
        };

        expect(collectHalResources(document)[0].schema).toBe('MemberSummaryResponseList');
    });

    it('leaves the schema undefined when the response has no content', () => {
        const document = memberDocument({'x-hal-links': {self: {}}});
        delete document.paths['/api/members/{id}'].get.responses['200'].content;

        expect(collectHalResources(document)[0].schema).toBeUndefined();
    });

    it('keeps only string descriptions', () => {
        const resources = collectHalResources(memberDocument({
            'x-hal-links': {
                self: {description: 'This member'},
                edit: {operation: 'updateMember'},
                other: {description: 42},
            },
        }));

        expect(resources[0].descriptions.links).toEqual({self: 'This member'});
    });

    it('records path, method and status of the response', () => {
        const resources = collectHalResources(memberDocument({'x-hal-links': {self: {}}}));

        expect(resources[0]).toMatchObject({
            path: '/api/members/{id}',
            method: 'get',
            status: '200',
        });
    });
});

describe('renderHalTypes', () => {
    it('produces a valid module for a document without x-hal-* declarations', () => {
        const output = renderHalTypes({paths: {}});

        expect(output).toContain('export {};');
        expect(output).not.toContain('export interface');
    });

    it('generates the interface, resource alias, rels constant and rel unions', () => {
        const output = renderHalTypes(memberDocument({
            'x-hal-links': {self: {}},
            'x-hal-templates': {default: {}},
        }));

        expect(output).toContain('export interface GetMemberHal {');
        expect(output).toContain("export type GetMemberResource =\n  components['schemas']['MemberDetailsResponse'] & GetMemberHal;");
        expect(output).toContain('export const GetMemberRels = {');
        expect(output).toContain('export type GetMemberLinkRel = typeof GetMemberRels.links[number];');
        expect(output).toContain('export type GetMemberTemplateRel = typeof GetMemberRels.templates[number];');
    });

    it('quotes rel names that are not valid identifiers', () => {
        const output = renderHalTypes(memberDocument({'x-hal-links': {'ical-token': {}}}));

        expect(output).toContain("'ical-token'?: HalResourceLinks;");
        expect(output).toContain("links: ['ical-token'] as const,");
    });

    it('omits the *Resource alias when the response declares no schema', () => {
        const document = memberDocument({'x-hal-links': {self: {}}});
        delete document.paths['/api/members/{id}'].get.responses['200'].content;

        const output = renderHalTypes(document);
        expect(output).toContain('export interface GetMemberHal {');
        expect(output).toContain('export const GetMemberRels = {');
        expect(output).not.toContain('GetMemberResource');
    });

    it('renders descriptions as JSDoc comments above the rel', () => {
        const output = renderHalTypes(memberDocument({
            'x-hal-links': {self: {description: 'This member'}},
            'x-hal-templates': {default: {description: 'Update this member'}},
        }));

        expect(output).toContain("/** This member */\n    'self'?: HalResourceLinks;");
        expect(output).toContain("/** Update this member */\n    'default'?: HalFormsTemplate;");
    });

    // Without `as const` the array widens to string[] and the *LinkRel union degrades to string,
    // which silently defeats the whole point of generating these types.
    it('marks the rels arrays as const', () => {
        const output = renderHalTypes(memberDocument({
            'x-hal-links': {self: {}, edit: {}},
            'x-hal-templates': {default: {}},
        }));

        expect(output).toContain("links: ['edit', 'self'] as const,");
        expect(output).toContain("templates: ['default'] as const,");
        expect(output).toContain('} as const;');
    });

    it('renders an empty rel block when only one of links or templates is declared', () => {
        const output = renderHalTypes(memberDocument({'x-hal-links': {self: {}}}));

        expect(output).toContain('_templates?: Record<never, never>;');
        expect(output).toContain('templates: [] as const,');
    });
});

const listDocument = (response, schemas = {}) => ({
    paths: {
        '/api/members': {
            get: {
                operationId: 'listMembers',
                responses: {
                    '200': {
                        content: {
                            'application/json': {
                                schema: {type: 'array', items: {$ref: '#/components/schemas/MemberSummaryResponse'}},
                            },
                            'application/prs.hal-forms+json': {
                                schema: {$ref: '#/components/schemas/CollectionModelEntityModelMemberSummaryResponse'},
                            },
                        },
                        ...response,
                    },
                },
            },
        },
    },
    components: {schemas},
});

describe('row types — placement on a response', () => {
    const rows = {
        'x-hal-links': {self: {}},
        'x-hal-item-links': {self: {description: 'This member'}},
        'x-hal-item-templates': {updateMember: {description: 'Edit member'}},
    };

    it('collects the item rels and the EntityModel row schema of an array payload', () => {
        const [resource] = collectHalResources(listDocument(rows));

        expect(resource.itemLinks).toEqual(['self']);
        expect(resource.itemTemplates).toEqual(['updateMember']);
        expect(resource.itemSchema).toBe('EntityModelMemberSummaryResponse');
    });

    it('types rows of an x-hal-embedded response by the declared items payload', () => {
        const document = memberDocument({
            'x-hal-links': {self: {}},
            'x-hal-embedded': {items: 'RegistrationResponse', suffix: 'WithRegistrations'},
            'x-hal-item-links': {self: {}},
        });
        document.paths['/api/members/{id}'].get.responses['200'].content['application/json'] = {
            schema: {$ref: '#/components/schemas/MemberDetailsResponse'},
        };

        const [resource] = collectHalResources(document);
        expect(resource.itemSchema).toBe('RegistrationResponse');
    });

    it('emits ItemHal, ItemResource and ItemRels next to the page types', () => {
        const output = renderHalTypes(listDocument(rows));

        expect(output).toContain('export interface ListMembersItemHal {');
        expect(output).toContain("'self'?: HalResourceLinks;");
        expect(output).toContain("'updateMember'?: HalFormsTemplate;");
        expect(output).toContain("/** Edit member */");
        expect(output).toContain("export type ListMembersItemResource =\n  components['schemas']['EntityModelMemberSummaryResponse'] & ListMembersItemHal;");
        expect(output).toContain("export const ListMembersItemRels = {\n  links: ['self'] as const,\n  templates: ['updateMember'] as const,\n} as const;");
        expect(output).toContain('export interface ListMembersHal {');
    });

    it('emits the row types even when the page itself declares no rels', () => {
        const output = renderHalTypes(listDocument({
            'x-hal-item-links': {self: {}},
        }));

        expect(output).toContain('export interface ListMembersItemHal {');
        expect(output).not.toContain('ListMembersHal');
    });

    it('leaves the output unchanged when no item rels are declared', () => {
        const without = renderHalTypes(listDocument({'x-hal-links': {self: {}}}));

        expect(without).not.toContain('Item');
    });
});

describe('row types — placement on an array property', () => {
    const groupDocument = (extraPaths = {}) => ({
        paths: {
            '/api/training-groups/{id}': {
                get: {
                    operationId: 'getTrainingGroup',
                    responses: {
                        '200': {
                            content: {
                                'application/prs.hal-forms+json': {
                                    schema: {$ref: '#/components/schemas/EntityModelTrainingGroupResponse'},
                                },
                            },
                            'x-hal-links': {self: {}},
                        },
                    },
                },
            },
            ...extraPaths,
        },
        components: {
            schemas: {
                EntityModelTrainingGroupResponse: {
                    allOf: [
                        {$ref: '#/components/schemas/TrainingGroupResponse'},
                        {$ref: '#/components/schemas/EntityModel'},
                    ],
                },
                TrainingGroupResponse: {
                    type: 'object',
                    properties: {
                        name: {type: 'string'},
                        members: {
                            type: 'array',
                            items: {$ref: '#/components/schemas/EntityModelTrainingGroupMemberResponse'},
                            'x-hal-item-links': {self: {description: 'This member'}},
                            'x-hal-item-templates': {removeMember: {}},
                        },
                        trainers: {
                            type: 'array',
                            items: {$ref: '#/components/schemas/EntityModelTrainerResponse'},
                            'x-hal-item-links': {self: {}},
                        },
                    },
                },
            },
        },
    });

    it('overrides the array properties on the parent resource via Omit', () => {
        const output = renderHalTypes(groupDocument());

        expect(output).toContain(
            "export type GetTrainingGroupResource =\n"
            + "  Omit<components['schemas']['EntityModelTrainingGroupResponse'], 'members' | 'trainers'>\n"
            + "  & {members?: GetTrainingGroupMembersItemResource[]; trainers?: GetTrainingGroupTrainersItemResource[]}\n"
            + "  & GetTrainingGroupHal;");
    });

    it('emits Hal, Resource and Rels per property, named after operation and property', () => {
        const output = renderHalTypes(groupDocument());

        expect(output).toContain('export interface GetTrainingGroupMembersItemHal {');
        expect(output).toContain("export type GetTrainingGroupMembersItemResource =\n  components['schemas']['EntityModelTrainingGroupMemberResponse'] & GetTrainingGroupMembersItemHal;");
        expect(output).toContain("export const GetTrainingGroupMembersItemRels = {\n  links: ['self'] as const,\n  templates: ['removeMember'] as const,\n} as const;");
        expect(output).toContain("export type GetTrainingGroupTrainersItemResource =\n  components['schemas']['EntityModelTrainerResponse'] & GetTrainingGroupTrainersItemHal;");
    });

    it('emits the override on every resource whose schema uses the property schema', () => {
        const output = renderHalTypes(groupDocument({
            '/api/training-groups': {
                post: {
                    operationId: 'createTrainingGroup',
                    responses: {
                        '201': {
                            content: {
                                'application/prs.hal-forms+json': {
                                    schema: {$ref: '#/components/schemas/EntityModelTrainingGroupResponse'},
                                },
                            },
                            'x-hal-links': {self: {}},
                        },
                    },
                },
            },
        }));

        expect(output).toContain('export type CreateTrainingGroupMembersItemResource =');
        expect(output).toContain('export type GetTrainingGroupMembersItemResource =');
        expect(output).toContain("Omit<components['schemas']['EntityModelTrainingGroupResponse'], 'members' | 'trainers'>\n  & {members?: CreateTrainingGroupMembersItemResource[]");
    });

    it('does not override a resource whose schema lacks the property schema', () => {
        const output = renderHalTypes(memberDocument({'x-hal-links': {self: {}}}));

        expect(output).not.toContain('Omit<');
    });
});

describe('row types — review fixes', () => {
    it('references the items payload itself when it is already envelope-shaped', () => {
        const document = listDocument({'x-hal-item-links': {self: {}}}, {
            MemberSummaryResponse: {
                type: 'object',
                properties: {_links: {type: 'object'}},
            },
        });

        const [resource] = collectHalResources(document);
        expect(resource.itemSchema).toBe('MemberSummaryResponse');
    });

    it('wraps a plain items payload in EntityModel', () => {
        const document = listDocument({'x-hal-item-links': {self: {}}}, {
            MemberSummaryResponse: {type: 'object', properties: {id: {type: 'string'}}},
        });

        expect(collectHalResources(document)[0].itemSchema).toBe('EntityModelMemberSummaryResponse');
    });

    const propertyOnlyDocument = (schemaPatch = {}) => ({
        paths: {
            '/api/groups/{id}': {
                get: {
                    operationId: 'getGroup',
                    responses: {
                        '200': {
                            content: {
                                'application/prs.hal-forms+json': {
                                    schema: {$ref: '#/components/schemas/EntityModelGroupResponse'},
                                },
                            },
                        },
                    },
                },
            },
        },
        components: {
            schemas: {
                EntityModelGroupResponse: {
                    allOf: [{$ref: '#/components/schemas/GroupResponse'}, {$ref: '#/components/schemas/EntityModel'}],
                },
                GroupResponse: {
                    type: 'object',
                    properties: {
                        owners: {
                            type: 'array',
                            items: {$ref: '#/components/schemas/EntityModelOwnerResponse'},
                            'x-hal-item-links': {self: {}},
                        },
                    },
                    ...schemaPatch,
                },
            },
        },
    });

    it('emits the parent resource with the override when the page declares no rels', () => {
        const output = renderHalTypes(propertyOnlyDocument());

        expect(output).toContain('export interface GetGroupHal {');
        expect(output).toContain(
            "export type GetGroupResource =\n"
            + "  Omit<components['schemas']['EntityModelGroupResponse'], 'owners'>\n"
            + "  & {owners?: GetGroupOwnersItemResource[]}\n"
            + "  & GetGroupHal;");
        expect(output).toContain('export const GetGroupOwnersItemRels = {');
    });

    it('does not emit empty page Rels for a parent that declares no rels', () => {
        const output = renderHalTypes(propertyOnlyDocument());

        expect(output).not.toContain('GetGroupRels');
        expect(output).not.toContain('GetGroupLinkRel');
    });

    it('keeps a required array property required in the override', () => {
        const output = renderHalTypes(propertyOnlyDocument({required: ['owners']}));

        expect(output).toContain('& {owners: GetGroupOwnersItemResource[]}');
    });

    it('reads required from an allOf sibling of the declaring schema', () => {
        const document = propertyOnlyDocument();
        const group = document.components.schemas.GroupResponse;
        document.components.schemas.GroupResponse = {
            allOf: [group, {required: ['owners']}],
        };

        expect(renderHalTypes(document)).toContain('& {owners: GetGroupOwnersItemResource[]}');
    });
});
