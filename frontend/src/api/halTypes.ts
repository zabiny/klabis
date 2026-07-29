/**
 * Generated from x-hal-links / x-hal-templates in docs/openapi/spec/.
 * Do not edit — run `npm run openapi` instead.
 *
 * Every relation is optional: links and templates the caller is not authorized for are
 * absent from the response, so these types describe the maximal variant. Use the *Rels
 * constants instead of string literals so a renamed relation fails the build.
 */

import type {HalFormsTemplate, HalResourceLinks} from './types';
import type {components} from './klabisApi';

// --- GET /api/event-types/{id} (200) ---

export interface GetEventTypeHal {
  _links?: {
    /** Back to the event type list */
    'collection'?: HalResourceLinks;
    /** This event type */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with EVENTS:MANAGE. The orisDisciplineIds property is
populated at runtime with the available ORIS discipline options.
 */
    'default'?: HalFormsTemplate;
    /** Present only for callers with EVENTS:MANAGE */
    'deleteEventType'?: HalFormsTemplate;
  };
}

export type GetEventTypeResource =
  components['schemas']['EntityModelEventTypeDto'] & GetEventTypeHal;

export const GetEventTypeRels = {
  links: ['collection', 'self'] as const,
  templates: ['default', 'deleteEventType'] as const,
} as const;

export type GetEventTypeLinkRel = typeof GetEventTypeRels.links[number];
export type GetEventTypeTemplateRel = typeof GetEventTypeRels.templates[number];

// --- GET /api/members/{id} (200) ---

export interface GetMemberHal {
  _links?: {
    /** Member's finance account (requires FINANCE:MANAGE or ownership) */
    'account'?: HalResourceLinks;
    /** Back to the member list */
    'collection'?: HalResourceLinks;
    /** Family group the member belongs to, when there is one */
    'familyGroup'?: HalResourceLinks;
    /** Membership fee summary for the current year */
    'feeSummary'?: HalResourceLinks;
    /** Calendar subscription token — only on the caller's own detail */
    'ical-token'?: HalResourceLinks;
    /** Member's permissions (requires MEMBERS:PERMISSIONS) */
    'permissions'?: HalResourceLinks;
    /** This member */
    'self'?: HalResourceLinks;
    /** Training group the member belongs to, when there is one */
    'trainingGroup'?: HalResourceLinks;
  };
  _templates?: {
    'default'?: HalFormsTemplate;
    /** Present only while the member is suspended */
    'resume'?: HalFormsTemplate;
    /** Present only while the member is active */
    'suspend'?: HalFormsTemplate;
  };
}

export type GetMemberResource =
  components['schemas']['EntityModelMemberDetailsResponse'] & GetMemberHal;

export const GetMemberRels = {
  links: ['account', 'collection', 'familyGroup', 'feeSummary', 'ical-token', 'permissions', 'self', 'trainingGroup'] as const,
  templates: ['default', 'resume', 'suspend'] as const,
} as const;

export type GetMemberLinkRel = typeof GetMemberRels.links[number];
export type GetMemberTemplateRel = typeof GetMemberRels.templates[number];

// --- GET /api/event-types (200) ---

export interface ListEventTypesHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with EVENTS:MANAGE. The orisDisciplineIds property is
populated at runtime with the available ORIS discipline options.
 */
    'createEventType'?: HalFormsTemplate;
  };
}

export type ListEventTypesResource =
  components['schemas']['CollectionModelEntityModelEventTypeDto'] & ListEventTypesHal;

export const ListEventTypesRels = {
  links: ['self'] as const,
  templates: ['createEventType'] as const,
} as const;

export type ListEventTypesLinkRel = typeof ListEventTypesRels.links[number];
export type ListEventTypesTemplateRel = typeof ListEventTypesRels.templates[number];

// --- GET /api/members (200) ---

export interface ListMembersHal {
  _links?: {
    /** First page (present when the result is paged) */
    'first'?: HalResourceLinks;
    /** Last page (present when the result is paged) */
    'last'?: HalResourceLinks;
    /** Next page (present when one exists) */
    'next'?: HalResourceLinks;
    /** Previous page (present when one exists) */
    'prev'?: HalResourceLinks;
    /** This collection, with the current paging and filter parameters */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Edit template used by the table's inline row editor */
    'default'?: HalFormsTemplate;
    /** Present only for callers with MEMBERS:MANAGE */
    'registerMember'?: HalFormsTemplate;
  };
}

export type ListMembersResource =
  components['schemas']['PagedModelEntityModelMemberSummaryResponse'] & ListMembersHal;

export const ListMembersRels = {
  links: ['first', 'last', 'next', 'prev', 'self'] as const,
  templates: ['default', 'registerMember'] as const,
} as const;

export type ListMembersLinkRel = typeof ListMembersRels.links[number];
export type ListMembersTemplateRel = typeof ListMembersRels.templates[number];
