import {describe, expect, it} from 'vitest';

import {stripHalForCodegen} from '../lib/stripHal.mjs';

const operationWithResponse = (response) => ({
    paths: {
        '/api/members/{id}': {
            get: {
                operationId: 'getMember',
                responses: {
                    '200': response,
                },
            },
        },
    },
});

describe('stripHalForCodegen', () => {
    it('empties the HAL schema but keeps the content-type key, when application/json is present', () => {
        const document = operationWithResponse({
            description: 'Member found',
            content: {
                'application/prs.hal-forms+json': {
                    schema: {$ref: '#/components/schemas/EntityModelMemberDetailsResponse'},
                },
                'application/json': {
                    schema: {$ref: '#/components/schemas/MemberDetailsResponse'},
                },
            },
            'x-hal-links': {self: {operation: 'getMember'}},
            'x-hal-templates': {default: {operation: 'updateMember'}},
        });

        const response = stripHalForCodegen(document).paths['/api/members/{id}'].get.responses['200'];

        expect(response.content).toEqual({
            'application/prs.hal-forms+json': {},
            'application/json': {schema: {$ref: '#/components/schemas/MemberDetailsResponse'}},
        });
        expect(response['x-hal-links']).toBeUndefined();
        expect(response['x-hal-templates']).toBeUndefined();
        expect(response.description).toBe('Member found');
    });

    it('strips application/hal+json the same way as application/prs.hal-forms+json', () => {
        const document = operationWithResponse({
            content: {
                'application/hal+json': {schema: {$ref: '#/components/schemas/Envelope'}},
                'application/json': {schema: {$ref: '#/components/schemas/Payload'}},
            },
        });

        const response = stripHalForCodegen(document).paths['/api/members/{id}'].get.responses['200'];

        expect(response.content['application/hal+json']).toEqual({});
        expect(response.content['application/json']).toEqual({schema: {$ref: '#/components/schemas/Payload'}});
    });

    it('leaves a response with no application/json sibling untouched', () => {
        const document = operationWithResponse({
            content: {
                'application/prs.hal-forms+json': {
                    schema: {$ref: '#/components/schemas/EntityModelMemberDetailsResponse'},
                },
            },
            'x-hal-links': {self: {operation: 'getMember'}},
        });

        const response = stripHalForCodegen(document).paths['/api/members/{id}'].get.responses['200'];

        expect(response.content).toEqual({
            'application/prs.hal-forms+json': {
                schema: {$ref: '#/components/schemas/EntityModelMemberDetailsResponse'},
            },
        });
        expect(response['x-hal-links']).toEqual({self: {operation: 'getMember'}});
    });

    it('leaves a bodyless response (no content at all) untouched', () => {
        const document = operationWithResponse({description: 'No content'});

        const response = stripHalForCodegen(document).paths['/api/members/{id}'].get.responses['200'];

        expect(response).toEqual({description: 'No content'});
    });

    it('leaves an already-empty HAL content entry (e.g. a 204) untouched', () => {
        const document = operationWithResponse({
            content: {'application/prs.hal-forms+json': {}},
        });

        const response = stripHalForCodegen(document).paths['/api/members/{id}'].get.responses['200'];

        expect(response.content).toEqual({'application/prs.hal-forms+json': {}});
    });

    it('does not mutate responses on operations/paths without any responses', () => {
        const document = {paths: {'/api/health': {get: {operationId: 'health'}}}};

        expect(stripHalForCodegen(document)).toEqual(document);
    });

    it('handles a document with no paths', () => {
        expect(stripHalForCodegen({})).toEqual({});
    });
});
