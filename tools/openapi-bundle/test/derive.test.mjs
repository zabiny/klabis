import {describe, expect, it} from 'vitest';

import {deriveHalEnvelopes, embeddedKey, isEnvelopeShaped} from '../lib/derive.mjs';

const LINKS = {$ref: '#/components/schemas/Links'};
const TEMPLATES = {$ref: '#/components/schemas/HalFormsTemplates'};

const ref = (name) => ({$ref: `#/components/schemas/${name}`});

/** Minimal document: one GET whose 200 returns `jsonSchema`, plus the given payload schemas. */
function docWith(jsonSchema, schemas = {}, {operation = {}, response = {}} = {}) {
    return {
        paths: {
            '/api/things': {
                get: {
                    operationId: 'listThings',
                    ...operation,
                    responses: {
                        '200': {...response, content: {'application/json': {schema: jsonSchema}}},
                    },
                },
            },
        },
        components: {schemas},
    };
}

const halForms = (document) => document.paths['/api/things'].get
    .responses['200'].content['application/prs.hal-forms+json'];

describe('embeddedKey', () => {
    it('prefers a declared x-klabis-relation.collectionRelation', () => {
        expect(embeddedKey('AccommodationListItemDto', {
            'x-klabis-relation': {collectionRelation: 'accommodationList'},
        })).toBe('accommodationList');
    });

    it('falls back to uncapitalize(name) + "List"', () => {
        expect(embeddedKey('MemberSummaryResponse', {})).toBe('memberSummaryResponseList');
    });

    it('applies the fallback purely lexically, keeping a Dto suffix', () => {
        expect(embeddedKey('EventSummaryDto', undefined)).toBe('eventSummaryDtoList');
    });
});

describe('isEnvelopeShaped', () => {
    it('recognises the allOf EntityModel form', () => {
        expect(isEnvelopeShaped({allOf: [ref('Thing'), {type: 'object', properties: {_links: LINKS}}]}))
            .toBe(true);
    });

    it('recognises the flat collection form', () => {
        expect(isEnvelopeShaped({type: 'object', properties: {_embedded: {}, _links: LINKS}}))
            .toBe(true);
    });

    it('recognises the hand-written marker types, which carry only _links', () => {
        expect(isEnvelopeShaped({type: 'object', properties: {_links: LINKS}})).toBe(true);
    });

    it('does not mistake a plain payload for an envelope', () => {
        expect(isEnvelopeShaped({type: 'object', properties: {id: {type: 'string'}}})).toBe(false);
    });
});

describe('deriveHalEnvelopes', () => {
    it('wraps a non-array payload in EntityModel', () => {
        const {document} = deriveHalEnvelopes(docWith(ref('MemberDetailsResponse'), {
            MemberDetailsResponse: {type: 'object', properties: {id: {type: 'string'}}},
        }));

        expect(halForms(document).schema).toEqual(ref('EntityModelMemberDetailsResponse'));
        expect(document.components.schemas.EntityModelMemberDetailsResponse).toEqual({
            allOf: [
                ref('MemberDetailsResponse'),
                {type: 'object', properties: {_links: LINKS}},
            ],
        });
    });

    it('wraps an unpaginated array in CollectionModel<EntityModel<Item>>', () => {
        const {document} = deriveHalEnvelopes(docWith(
            {type: 'array', items: ref('CalendarItemDto')},
            {CalendarItemDto: {type: 'object', properties: {id: {type: 'string'}}}},
        ));

        expect(halForms(document).schema).toEqual(ref('CollectionModelEntityModelCalendarItemDto'));
        expect(document.components.schemas.CollectionModelEntityModelCalendarItemDto).toEqual({
            type: 'object',
            properties: {
                _embedded: {
                    type: 'object',
                    properties: {
                        calendarItemDtoList: {
                            type: 'array',
                            items: ref('EntityModelCalendarItemDto'),
                        },
                    },
                },
                _links: LINKS,
            },
        });
        expect(document.components.schemas.EntityModelCalendarItemDto).toBeDefined();
    });

    it('wraps a paginated array in PagedModel and adds the page property', () => {
        const {document} = deriveHalEnvelopes(docWith(
            {type: 'array', items: ref('MemberSummaryResponse')},
            {MemberSummaryResponse: {type: 'object', properties: {id: {type: 'string'}}}},
            {operation: {'x-spring-paginated': true}},
        ));

        const paged = document.components.schemas.PagedModelEntityModelMemberSummaryResponse;
        expect(halForms(document).schema).toEqual(ref('PagedModelEntityModelMemberSummaryResponse'));
        expect(paged.properties.page).toEqual({$ref: '#/components/schemas/PageMetadata'});
        expect(paged.properties._embedded.properties.memberSummaryResponseList.items)
            .toEqual(ref('EntityModelMemberSummaryResponse'));
        expect(document.components.schemas.CollectionModelEntityModelMemberSummaryResponse)
            .toBeUndefined();
    });

    it('uses the declared collectionRelation as the _embedded key', () => {
        const {document} = deriveHalEnvelopes(docWith(
            {type: 'array', items: ref('AccommodationListItemDto')},
            {
                AccommodationListItemDto: {
                    type: 'object',
                    'x-klabis-relation': {collectionRelation: 'accommodationList'},
                    properties: {id: {type: 'string'}},
                },
            },
        ));

        const embedded = document.components.schemas
            .CollectionModelEntityModelAccommodationListItemDto.properties._embedded;
        expect(Object.keys(embedded.properties)).toEqual(['accommodationList']);
    });

    it('adds _templates when the response declares x-hal-templates', () => {
        const {document} = deriveHalEnvelopes(docWith(
            ref('CalendarItemDto'),
            {CalendarItemDto: {type: 'object', properties: {id: {type: 'string'}}}},
            {response: {'x-hal-templates': {update: {operation: 'updateCalendarItem'}}}},
        ));

        expect(document.components.schemas.EntityModelCalendarItemDto.allOf[1].properties)
            .toEqual({_links: LINKS, _templates: TEMPLATES});
    });

    it('leaves an operation carrying x-klabis-hal: false untouched', () => {
        const {document} = deriveHalEnvelopes(docWith(
            ref('OrisEventSummary'),
            {OrisEventSummary: {type: 'object', properties: {id: {type: 'string'}}}},
            {operation: {'x-klabis-hal': false}},
        ));

        expect(halForms(document)).toBeUndefined();
        expect(document.components.schemas.EntityModelOrisEventSummary).toBeUndefined();
    });

    it('does not envelope a non-2xx response body', () => {
        const document = {
            paths: {
                '/api/members/{id}/suspend': {
                    post: {
                        operationId: 'suspendMember',
                        responses: {
                            '409': {content: {'application/json': {schema: ref('SuspensionBlockedWarning')}}},
                        },
                    },
                },
            },
            components: {schemas: {SuspensionBlockedWarning: {type: 'object', properties: {}}}},
        };

        deriveHalEnvelopes(document);

        expect(document.components.schemas.EntityModelSuspensionBlockedWarning).toBeUndefined();
    });

    it('reuses one derived schema across two operations returning the same payload', () => {
        const document = docWith(ref('IcalTokenResponse'), {
            IcalTokenResponse: {type: 'object', properties: {token: {type: 'string'}}},
        });
        document.paths['/api/things'].post = {
            operationId: 'createThing',
            responses: {'200': {content: {'application/json': {schema: ref('IcalTokenResponse')}}}},
        };

        const {collisions} = deriveHalEnvelopes(document);

        expect(collisions).toEqual([]);
        expect(document.paths['/api/things'].post.responses['200']
            .content['application/prs.hal-forms+json'].schema)
            .toEqual(ref('EntityModelIcalTokenResponse'));
    });

    describe('x-hal-entity-items', () => {
        it('emits EntityModel<Item> and retargets the items $ref', () => {
            const document = docWith(ref('TrainingGroupResponse'), {
                TrainingGroupResponse: {
                    type: 'object',
                    properties: {
                        trainers: {
                            type: 'array',
                            'x-hal-entity-items': true,
                            items: ref('TrainerResponse'),
                        },
                    },
                },
                TrainerResponse: {type: 'object', properties: {id: {type: 'string'}}},
            });

            deriveHalEnvelopes(document);

            expect(document.components.schemas.TrainingGroupResponse.properties.trainers.items)
                .toEqual(ref('EntityModelTrainerResponse'));
            expect(document.components.schemas.EntityModelTrainerResponse).toEqual({
                allOf: [ref('TrainerResponse'), {type: 'object', properties: {_links: LINKS}}],
            });
        });

        it('leaves an unmarked array property alone', () => {
            const document = docWith(ref('GroupResponse'), {
                GroupResponse: {
                    type: 'object',
                    properties: {tags: {type: 'array', items: ref('Tag')}},
                },
                Tag: {type: 'object', properties: {}},
            });

            deriveHalEnvelopes(document);

            expect(document.components.schemas.GroupResponse.properties.tags.items)
                .toEqual(ref('Tag'));
            expect(document.components.schemas.EntityModelTag).toBeUndefined();
        });
    });

    describe('no-op on already-enveloped input', () => {
        it('skips a response that already has a hal-forms entry', () => {
            const document = docWith(ref('MemberSummaryResponseList'), {
                MemberSummaryResponseList: {type: 'array', items: ref('MemberSummaryResponse')},
                MemberSummaryResponse: {type: 'object', properties: {}},
                PagedModelEntityModelMemberSummaryResponse: {type: 'object', properties: {}},
            });
            document.paths['/api/things'].get.responses['200']
                .content['application/prs.hal-forms+json'] =
                {schema: ref('PagedModelEntityModelMemberSummaryResponse')};
            const before = JSON.stringify(document);

            deriveHalEnvelopes(document);

            expect(JSON.stringify(document)).toBe(before);
        });

        it('does not wrap a payload that is itself already envelope-shaped', () => {
            const document = docWith(ref('EntityModelRootModel'), {
                EntityModelRootModel: {type: 'object', properties: {_links: LINKS}},
            });

            deriveHalEnvelopes(document);

            expect(halForms(document)).toBeUndefined();
            expect(document.components.schemas.EntityModelEntityModelRootModel).toBeUndefined();
        });

        it('does not double-wrap items already pointing at an EntityModel', () => {
            const document = docWith(ref('GroupResponse'), {
                GroupResponse: {
                    type: 'object',
                    properties: {
                        owners: {
                            type: 'array',
                            'x-hal-entity-items': true,
                            items: ref('EntityModelOwnerResponse'),
                        },
                    },
                },
                EntityModelOwnerResponse: {
                    allOf: [ref('OwnerResponse'), {type: 'object', properties: {_links: LINKS}}],
                },
                OwnerResponse: {type: 'object', properties: {}},
            });

            deriveHalEnvelopes(document);

            expect(document.components.schemas.GroupResponse.properties.owners.items)
                .toEqual(ref('EntityModelOwnerResponse'));
            expect(document.components.schemas.EntityModelEntityModelOwnerResponse).toBeUndefined();
        });

        it('leaves a response whose content has no application/json entry alone', () => {
            const document = docWith(undefined, {AccommodationListItemDto: {type: 'object'}});
            document.paths['/api/things'].get.responses['200'].content =
                {'text/csv': {schema: {type: 'string', format: 'binary'}}};

            deriveHalEnvelopes(document);

            expect(halForms(document)).toBeUndefined();
            expect(document.paths['/api/things'].get.responses['200'].content)
                .toEqual({'text/csv': {schema: {type: 'string', format: 'binary'}}});
        });

        it('leaves a response with no application/json schema alone', () => {
            const document = docWith(undefined, {});
            document.paths['/api/things'].get.responses['200'].content =
                {'application/json': {}};

            deriveHalEnvelopes(document);

            expect(halForms(document)).toBeUndefined();
        });
    });
});
