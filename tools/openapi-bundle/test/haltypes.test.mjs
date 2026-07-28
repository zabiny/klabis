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
