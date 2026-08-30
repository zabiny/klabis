import {describe, expect, it} from 'vitest';

import {compareDocuments, normalizeOperations} from '../lib/compare.mjs';

const getMember = (overrides = {}) => ({
    paths: {
        '/api/members/{id}': {
            get: {
                operationId: 'getMember',
                parameters: [{name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}}],
                responses: {
                    '200': {
                        description: 'Member found',
                        content: {
                            'application/prs.hal-forms+json': {
                                schema: {$ref: '#/components/schemas/MemberDetailsResponse'},
                            },
                        },
                    },
                },
                ...overrides,
            },
        },
    },
});

describe('compareDocuments', () => {
    it('matches identical operations', () => {
        const result = compareDocuments(getMember(), getMember());
        expect(result.matched).toBe(1);
        expect(result.missingInSpec).toEqual([]);
        expect(result.mismatched).toEqual([]);
    });

    it('ignores description, summary and tags', () => {
        const codeFirst = getMember();
        const specFirst = getMember({summary: 'Get a member', tags: ['Members']});
        specFirst.paths['/api/members/{id}'].get.responses['200'].description = 'Different prose';

        expect(compareDocuments(codeFirst, specFirst).matched).toBe(1);
    });

    it('ignores x-* extensions, which springdoc cannot express', () => {
        const specFirst = getMember();
        specFirst.paths['/api/members/{id}'].get.responses['200']['x-hal-links'] = {
            self: {description: 'This member'},
        };

        expect(compareDocuments(getMember(), specFirst).matched).toBe(1);
    });

    it('reports an operation that is not migrated yet', () => {
        const result = compareDocuments(getMember(), {paths: {}});
        expect(result.missingInSpec).toEqual(['GET /api/members/{id}']);
        expect(result.matched).toBe(0);
    });

    it('reports an operation present only in the spec', () => {
        const result = compareDocuments({paths: {}}, getMember());
        expect(result.extraInSpec).toEqual(['GET /api/members/{id}']);
    });

    it('detects a changed parameter type', () => {
        const specFirst = getMember();
        specFirst.paths['/api/members/{id}'].get.parameters[0].schema = {type: 'integer'};

        const result = compareDocuments(getMember(), specFirst);
        expect(result.mismatched).toHaveLength(1);
        expect(result.mismatched[0].differences[0].field).toBe('parameters');
    });

    it('detects a changed response schema', () => {
        const specFirst = getMember();
        specFirst.paths['/api/members/{id}'].get.responses['200']
            .content['application/prs.hal-forms+json'].schema = {$ref: '#/components/schemas/Other'};

        const result = compareDocuments(getMember(), specFirst);
        expect(result.mismatched).toHaveLength(1);
        expect(result.mismatched[0].differences[0].field).toBe('responses');
    });

    it('treats an id typed as an object differently from a uuid string', () => {
        // This is the known MemberId discrepancy: springdoc reports an object because it does not
        // know about @JsonValue on the mixin, while the spec correctly says it is a uuid string.
        const codeFirst = getMember();
        codeFirst.paths['/api/members/{id}'].get.responses['200']
            .content['application/prs.hal-forms+json'].schema = {$ref: '#/components/schemas/MemberId'};

        expect(compareDocuments(codeFirst, getMember()).mismatched).toHaveLength(1);
    });
});

describe('component references', () => {
    // A hand-written spec factors shared parameters and error responses into components; the
    // springdoc output inlines everything. Without dereferencing, every migrated operation would
    // look mismatched.
    const inlined = {
        paths: {
            '/api/members/{id}': {
                get: {
                    operationId: 'getMember',
                    parameters: [{name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}}],
                    responses: {
                        '404': {
                            description: 'Resource not found',
                            content: {'application/problem+json': {schema: {$ref: '#/components/schemas/ProblemDetail'}}},
                        },
                    },
                },
            },
        },
    };

    const referenced = {
        paths: {
            '/api/members/{id}': {
                get: {
                    operationId: 'getMember',
                    parameters: [{$ref: '#/components/parameters/MemberIdParam'}],
                    responses: {'404': {$ref: '#/components/responses/NotFound'}},
                },
            },
        },
        components: {
            parameters: {
                MemberIdParam: {name: 'id', in: 'path', required: true, schema: {type: 'string', format: 'uuid'}},
            },
            responses: {
                NotFound: {
                    description: 'Resource not found',
                    content: {'application/problem+json': {schema: {$ref: '#/components/schemas/ProblemDetail'}}},
                },
            },
        },
    };

    it('resolves referenced parameters and responses before comparing', () => {
        const result = compareDocuments(inlined, referenced);
        expect(result.mismatched).toEqual([]);
        expect(result.matched).toBe(1);
    });

    it('still detects a genuine difference behind a reference', () => {
        const changed = structuredClone(referenced);
        changed.components.parameters.MemberIdParam.schema = {type: 'integer'};

        expect(compareDocuments(inlined, changed).mismatched).toHaveLength(1);
    });

    it('does not hang on a self-referencing component', () => {
        const cyclic = structuredClone(referenced);
        cyclic.components.parameters.MemberIdParam = {$ref: '#/components/parameters/MemberIdParam'};

        expect(() => compareDocuments(inlined, cyclic)).not.toThrow();
    });
});

describe('normalizeOperations', () => {
    it('merges path-level parameters into each operation', () => {
        const operations = normalizeOperations({
            paths: {
                '/api/members/{id}': {
                    parameters: [{name: 'id', in: 'path', required: true, schema: {type: 'string'}}],
                    get: {operationId: 'getMember', responses: {}},
                },
            },
        });

        expect(operations.get('GET /api/members/{id}').parameters)
            .toEqual([{name: 'id', in: 'path', required: true, type: 'string'}]);
    });

    it('sorts parameters so declaration order does not matter', () => {
        const build = (params) => normalizeOperations({
            paths: {'/x': {get: {operationId: 'x', parameters: params, responses: {}}}},
        }).get('GET /x').parameters;

        const a = build([
            {name: 'q', in: 'query', schema: {type: 'string'}},
            {name: 'status', in: 'query', schema: {type: 'string'}},
        ]);
        const b = build([
            {name: 'status', in: 'query', schema: {type: 'string'}},
            {name: 'q', in: 'query', schema: {type: 'string'}},
        ]);

        expect(a).toEqual(b);
    });
});
