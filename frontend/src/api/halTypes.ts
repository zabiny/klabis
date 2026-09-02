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

// --- GET /api/dashboard (200) ---

export interface DashboardHal {
  _links?: {
    /** This dashboard */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type DashboardResource =
  components['schemas']['EntityModelDashboardModel'] & DashboardHal;

export const DashboardRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type DashboardLinkRel = typeof DashboardRels.links[number];
export type DashboardTemplateRel = typeof DashboardRels.templates[number];

// --- POST /api/me/ical-token (200) ---

export interface GenerateTokenHal {
  _links?: {
    /** This token state */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type GenerateTokenResource =
  components['schemas']['EntityModelIcalTokenResponse'] & GenerateTokenHal;

export const GenerateTokenRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type GenerateTokenLinkRel = typeof GenerateTokenRels.links[number];
export type GenerateTokenTemplateRel = typeof GenerateTokenRels.templates[number];

// --- GET /api/events/{eventId}/accommodation-list (200) ---

export interface GetAccommodationListHal {
  _links?: {
    /** The event this accommodation list belongs to. Present only when the caller may also
read the event: reaching this list needs EVENTS:REGISTRATIONS or being the
coordinator, which does not imply EVENTS:READ.
 */
    'event'?: HalResourceLinks;
    /** This accommodation list */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type GetAccommodationListResource =
  components['schemas']['CollectionModelEntityModelAccommodationListItemDto'] & GetAccommodationListHal;

export const GetAccommodationListRels = {
  links: ['event', 'self'] as const,
  templates: [] as const,
} as const;

export type GetAccommodationListLinkRel = typeof GetAccommodationListRels.links[number];
export type GetAccommodationListTemplateRel = typeof GetAccommodationListRels.templates[number];

// --- GET /api/members/{memberId}/account (200) ---

export interface GetAccountHal {
  _links?: {
    /** The member this account belongs to */
    'accountOwner'?: HalResourceLinks;
    /** This account */
    'self'?: HalResourceLinks;
    /** Transaction history */
    'transactions'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with FINANCE:MANAGE */
    'charge'?: HalFormsTemplate;
    /** Present only for callers with FINANCE:MANAGE */
    'deposit'?: HalFormsTemplate;
  };
}

export type GetAccountResource =
  components['schemas']['EntityModelMemberAccountResource'] & GetAccountHal;

export const GetAccountRels = {
  links: ['accountOwner', 'self', 'transactions'] as const,
  templates: ['charge', 'deposit'] as const,
} as const;

export type GetAccountLinkRel = typeof GetAccountRels.links[number];
export type GetAccountTemplateRel = typeof GetAccountRels.templates[number];

// --- GET /api/calendar-items/{id} (200) ---

export interface GetCalendarItemHal {
  _links?: {
    /** Back to the current month's calendar item list */
    'collection'?: HalResourceLinks;
    /** Present only for event-linked items — the event this item is derived from */
    'event'?: HalResourceLinks;
    /** This calendar item. Event-linked items carry no update/delete affordances since
they are read-only.
 */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for manual items and callers with CALENDAR:MANAGE */
    'deleteCalendarItem'?: HalFormsTemplate;
    /** Present only for manual items and callers with CALENDAR:MANAGE */
    'updateCalendarItem'?: HalFormsTemplate;
  };
}

export type GetCalendarItemResource =
  components['schemas']['EntityModelCalendarItemDto'] & GetCalendarItemHal;

export const GetCalendarItemRels = {
  links: ['collection', 'event', 'self'] as const,
  templates: ['deleteCalendarItem', 'updateCalendarItem'] as const,
} as const;

export type GetCalendarItemLinkRel = typeof GetCalendarItemRels.links[number];
export type GetCalendarItemTemplateRel = typeof GetCalendarItemRels.templates[number];

// --- GET /api/events/{id} (200) ---

export interface GetEventHal {
  _links?: {
    /** Present only when the event offers shared accommodation and the caller is the
event coordinator or has EVENTS:REGISTRATIONS
 */
    'accommodation-list'?: HalResourceLinks;
    /** Back to the event list */
    'collection'?: HalResourceLinks;
    /** One link per event coordinator */
    'coordinator'?: HalResourceLinks;
    /** Present when the event has an event type assigned */
    'event-type'?: HalResourceLinks;
    /** Present for the caller's own not-yet-existing registration when registration is
being offered
 */
    'newRegistration'?: HalResourceLinks;
    /** Present for events not in DRAFT status */
    'registrations'?: HalResourceLinks;
    /** This event */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present for DRAFT/ACTIVE events and callers with EVENTS:MANAGE */
    'cancelEvent'?: HalFormsTemplate;
    /** Present when registrations are open and the caller is registered. The
wantsSharedTransport / wantsSharedAccommodation properties are included only for
the offers the event has enabled.
 */
    'editRegistration'?: HalFormsTemplate;
    /** Present for DRAFT events and callers with EVENTS:MANAGE */
    'publishEvent'?: HalFormsTemplate;
    /** Present when registrations are open and the caller is not yet registered. The
wantsSharedTransport / wantsSharedAccommodation properties are included only for
the offers the event has enabled.
 */
    'registerForEvent'?: HalFormsTemplate;
    /** Present when ORIS integration is active and the event has an orisId */
    'syncEventFromOris'?: HalFormsTemplate;
    /** Present when registrations are open and the caller is registered */
    'unregisterFromEvent'?: HalFormsTemplate;
    /** Present for the event coordinator or callers with EVENTS:MANAGE. Includes the
sharedTransportEnabled / sharedAccommodationEnabled properties.
 */
    'updateEvent'?: HalFormsTemplate;
  };
}

export type GetEventResource =
  components['schemas']['EntityModelEventDtoWithRegistrations'] & GetEventHal;

export const GetEventRels = {
  links: ['accommodation-list', 'collection', 'coordinator', 'event-type', 'newRegistration', 'registrations', 'self'] as const,
  templates: ['cancelEvent', 'editRegistration', 'publishEvent', 'registerForEvent', 'syncEventFromOris', 'unregisterFromEvent', 'updateEvent'] as const,
} as const;

export type GetEventLinkRel = typeof GetEventRels.links[number];
export type GetEventTemplateRel = typeof GetEventRels.templates[number];

// --- GET /api/event-types/{id} (200) ---

export interface GetEventTypeHal {
  _links?: {
    /** Back to the event type list */
    'collection'?: HalResourceLinks;
    /** This event type */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with EVENTS:MANAGE */
    'deleteEventType'?: HalFormsTemplate;
    /** Present only for callers with EVENTS:MANAGE. The orisDisciplineIds property is
populated at runtime with the available ORIS discipline options.
 */
    'updateEventType'?: HalFormsTemplate;
  };
}

export type GetEventTypeResource =
  components['schemas']['EntityModelEventTypeDto'] & GetEventTypeHal;

export const GetEventTypeRels = {
  links: ['collection', 'self'] as const,
  templates: ['deleteEventType', 'updateEventType'] as const,
} as const;

export type GetEventTypeLinkRel = typeof GetEventTypeRels.links[number];
export type GetEventTypeTemplateRel = typeof GetEventTypeRels.templates[number];

// --- GET /api/family-groups/{id} (200) ---

export interface GetFamilyGroupHal {
  _links?: {
    /** Present only for callers with MEMBERS:MANAGE */
    'collection'?: HalResourceLinks;
    /** This family group */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type GetFamilyGroupResource =
  components['schemas']['EntityModelFamilyGroupResponse'] & GetFamilyGroupHal;

export const GetFamilyGroupRels = {
  links: ['collection', 'self'] as const,
  templates: [] as const,
} as const;

export type GetFamilyGroupLinkRel = typeof GetFamilyGroupRels.links[number];
export type GetFamilyGroupTemplateRel = typeof GetFamilyGroupRels.templates[number];

// --- GET /api/membership-fee-groups/{id} (200) ---

export interface GetFeeGroupHal {
  _links?: {
    /** Payment rules snapshot for this group */
    'rules'?: HalResourceLinks;
    /** This fee group */
    'self'?: HalResourceLinks;
    /** The membership fee tier this group's snapshot was published from */
    'sourceLevel'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with MEMBERS:MANAGE */
    'assignMember'?: HalFormsTemplate;
    /** Present for callers with MEMBERS:MANAGE, only while the group is EDITABLE */
    'editSnapshot'?: HalFormsTemplate;
  };
}

export type GetFeeGroupResource =
  components['schemas']['EntityModelMembershipFeeGroupResponseWithMembers'] & GetFeeGroupHal;

export const GetFeeGroupRels = {
  links: ['rules', 'self', 'sourceLevel'] as const,
  templates: ['assignMember', 'editSnapshot'] as const,
} as const;

export type GetFeeGroupLinkRel = typeof GetFeeGroupRels.links[number];
export type GetFeeGroupTemplateRel = typeof GetFeeGroupRels.templates[number];

// --- GET /api/members/{memberId}/fee-history (200) ---

export interface GetFeeHistoryHal {
  _links?: {
    /** This fee history */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type GetFeeHistoryResource =
  components['schemas']['EntityModelMemberFeeHistoryResponse'] & GetFeeHistoryHal;

export const GetFeeHistoryRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type GetFeeHistoryLinkRel = typeof GetFeeHistoryRels.links[number];
export type GetFeeHistoryTemplateRel = typeof GetFeeHistoryRels.templates[number];

// --- GET /api/members/{memberId}/fee-summary/{year} (200) ---

export interface GetFeeSummaryHal {
  _links?: {
    /** Present when the member has a current fee group */
    'group'?: HalResourceLinks;
    /** This fee summary */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only while voting is open for this year */
    'chooseTier'?: HalFormsTemplate;
  };
}

export type GetFeeSummaryResource =
  components['schemas']['EntityModelMemberFeeSummaryResponse'] & GetFeeSummaryHal;

export const GetFeeSummaryRels = {
  links: ['group', 'self'] as const,
  templates: ['chooseTier'] as const,
} as const;

export type GetFeeSummaryLinkRel = typeof GetFeeSummaryRels.links[number];
export type GetFeeSummaryTemplateRel = typeof GetFeeSummaryRels.templates[number];

// --- GET /api/groups/{id} (200) ---

export interface GetGroupHal {
  _links?: {
    /** Back to the group list */
    'collection'?: HalResourceLinks;
    /** This group. Carries write affordances only for owners. */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for group owners */
    'addGroupOwner'?: HalFormsTemplate;
    /** Present only for group owners */
    'deleteGroup'?: HalFormsTemplate;
    /** Present only for group owners */
    'inviteMember'?: HalFormsTemplate;
    /** Present only for group owners */
    'updateGroup'?: HalFormsTemplate;
  };
}

export type GetGroupResource =
  components['schemas']['EntityModelGroupResponse'] & GetGroupHal;

export const GetGroupRels = {
  links: ['collection', 'self'] as const,
  templates: ['addGroupOwner', 'deleteGroup', 'inviteMember', 'updateGroup'] as const,
} as const;

export type GetGroupLinkRel = typeof GetGroupRels.links[number];
export type GetGroupTemplateRel = typeof GetGroupRels.templates[number];

// --- GET /api/members/{memberId}/fee-choice/{year} (200) ---

export interface GetChoiceHal {
  _links?: {
    /** Present when the member has a current fee group choice */
    'currentGroup'?: HalResourceLinks;
    /** Present when a recommended tier exists for this year */
    'recommendedLevel'?: HalResourceLinks;
    /** This fee choice */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Choose a fee level for this year */
    'chooseTier'?: HalFormsTemplate;
    /** Remove the fee level choice for this year */
    'removeChoice'?: HalFormsTemplate;
  };
}

export type GetChoiceResource =
  components['schemas']['EntityModelMemberFeeChoiceResponse'] & GetChoiceHal;

export const GetChoiceRels = {
  links: ['currentGroup', 'recommendedLevel', 'self'] as const,
  templates: ['chooseTier', 'removeChoice'] as const,
} as const;

export type GetChoiceLinkRel = typeof GetChoiceRels.links[number];
export type GetChoiceTemplateRel = typeof GetChoiceRels.templates[number];

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
    /** Present only while the member is suspended */
    'resume'?: HalFormsTemplate;
    /** Present only while the member is active */
    'suspend'?: HalFormsTemplate;
    'updateMember'?: HalFormsTemplate;
  };
}

export type GetMemberResource =
  components['schemas']['EntityModelMemberDetailsResponse'] & GetMemberHal;

export const GetMemberRels = {
  links: ['account', 'collection', 'familyGroup', 'feeSummary', 'ical-token', 'permissions', 'self', 'trainingGroup'] as const,
  templates: ['resume', 'suspend', 'updateMember'] as const,
} as const;

export type GetMemberLinkRel = typeof GetMemberRels.links[number];
export type GetMemberTemplateRel = typeof GetMemberRels.templates[number];

// --- GET /api/invitations/pending (200) ---

export interface GetPendingInvitationsHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type GetPendingInvitationsResource =
  components['schemas']['CollectionModelEntityModelPendingInvitationResponse'] & GetPendingInvitationsHal;

export const GetPendingInvitationsRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type GetPendingInvitationsLinkRel = typeof GetPendingInvitationsRels.links[number];
export type GetPendingInvitationsTemplateRel = typeof GetPendingInvitationsRels.templates[number];

// --- GET /api/category-presets/{id} (200) ---

export interface GetPresetHal {
  _links?: {
    /** Back to the category preset list */
    'collection'?: HalResourceLinks;
    /** This category preset */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Delete this category preset */
    'deleteCategoryPreset'?: HalFormsTemplate;
    /** Update this category preset */
    'updateCategoryPreset'?: HalFormsTemplate;
  };
}

export type GetPresetResource =
  components['schemas']['EntityModelCategoryPresetDto'] & GetPresetHal;

export const GetPresetRels = {
  links: ['collection', 'self'] as const,
  templates: ['deleteCategoryPreset', 'updateCategoryPreset'] as const,
} as const;

export type GetPresetLinkRel = typeof GetPresetRels.links[number];
export type GetPresetTemplateRel = typeof GetPresetRels.templates[number];

// --- GET /api/fee-selection-campaigns/{id} (200) ---

export interface GetPublicationHal {
  _links?: {
    /** Back to the publication list */
    'collection'?: HalResourceLinks;
    /** Fee groups published for this campaign's year */
    'levels'?: HalResourceLinks;
    /** This publication */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present for callers with MEMBERS:MANAGE, while the campaign is not closed */
    'changeDeadline'?: HalFormsTemplate;
    /** Present for callers with MEMBERS:MANAGE, while the campaign is not closed and not
yet processed
 */
    'closeCampaign'?: HalFormsTemplate;
  };
}

export type GetPublicationResource =
  components['schemas']['EntityModelFeeSelectionCampaignResponse'] & GetPublicationHal;

export const GetPublicationRels = {
  links: ['collection', 'levels', 'self'] as const,
  templates: ['changeDeadline', 'closeCampaign'] as const,
} as const;

export type GetPublicationLinkRel = typeof GetPublicationRels.links[number];
export type GetPublicationTemplateRel = typeof GetPublicationRels.templates[number];

// --- GET /api/events/{eventId}/registrations/{memberId} (200) ---

export interface GetRegistrationHal {
  _links?: {
    /** The event this registration belongs to */
    'event'?: HalResourceLinks;
    /** This registration */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only on the existing-registration (newRegistration=false) response, when
registrations are open. The wantsSharedTransport / wantsSharedAccommodation
properties are included only for the offers the event has enabled.
 */
    'editRegistration'?: HalFormsTemplate;
    /** Present only on the newRegistration=true prefill response, when registrations are
open and the member is not blocked from registering. The wantsSharedTransport /
wantsSharedAccommodation properties are included only for the offers the event
has enabled.
 */
    'registerForEvent'?: HalFormsTemplate;
    /** Present only on the existing-registration (newRegistration=false) response, when
registrations are open and the caller is the registered member
 */
    'unregisterFromEvent'?: HalFormsTemplate;
  };
}

export type GetRegistrationResource =
  components['schemas']['EntityModelRegistrationDto'] & GetRegistrationHal;

export const GetRegistrationRels = {
  links: ['event', 'self'] as const,
  templates: ['editRegistration', 'registerForEvent', 'unregisterFromEvent'] as const,
} as const;

export type GetRegistrationLinkRel = typeof GetRegistrationRels.links[number];
export type GetRegistrationTemplateRel = typeof GetRegistrationRels.templates[number];

// --- GET /api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking} (200) ---

export interface GetRuleHal {
  _links?: {
    /** The event type this rule applies to */
    'eventType'?: HalResourceLinks;
    /** This payment rule */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with MEMBERS:MANAGE */
    'editRule'?: HalFormsTemplate;
    /** Present only for callers with MEMBERS:MANAGE */
    'removeRule'?: HalFormsTemplate;
  };
}

export type GetRuleResource =
  components['schemas']['EntityModelPaymentRuleResponse'] & GetRuleHal;

export const GetRuleRels = {
  links: ['eventType', 'self'] as const,
  templates: ['editRule', 'removeRule'] as const,
} as const;

export type GetRuleLinkRel = typeof GetRuleRels.links[number];
export type GetRuleTemplateRel = typeof GetRuleRels.templates[number];

// --- GET /api/membership-fee-tiers/{id} (200) ---

export interface GetTierHal {
  _links?: {
    /** Back to the tier list */
    'collection'?: HalResourceLinks;
    /** Payment rules for this tier */
    'rules'?: HalResourceLinks;
    /** This tier */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with MEMBERS:MANAGE */
    'deleteTier'?: HalFormsTemplate;
    /** Present only for callers with MEMBERS:MANAGE */
    'editTier'?: HalFormsTemplate;
  };
}

export type GetTierResource =
  components['schemas']['EntityModelMembershipFeeTierResponse'] & GetTierHal;

export const GetTierRels = {
  links: ['collection', 'rules', 'self'] as const,
  templates: ['deleteTier', 'editTier'] as const,
} as const;

export type GetTierLinkRel = typeof GetTierRels.links[number];
export type GetTierTemplateRel = typeof GetTierRels.templates[number];

// --- GET /api/me/ical-token (200) ---

export interface GetTokenStateHal {
  _links?: {
    /** This token state */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Generates or rotates the token; the full subscribe URL is returned once */
    'generateToken'?: HalFormsTemplate;
  };
}

export type GetTokenStateResource =
  components['schemas']['EntityModelIcalTokenResponse'] & GetTokenStateHal;

export const GetTokenStateRels = {
  links: ['self'] as const,
  templates: ['generateToken'] as const,
} as const;

export type GetTokenStateLinkRel = typeof GetTokenStateRels.links[number];
export type GetTokenStateTemplateRel = typeof GetTokenStateRels.templates[number];

// --- GET /api/training-groups/{id} (200) ---

export interface GetTrainingGroupHal {
  _links?: {
    /** Present only for callers with GROUPS:TRAINING */
    'collection'?: HalResourceLinks;
    /** This training group */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type GetTrainingGroupResource =
  components['schemas']['EntityModelTrainingGroupResponse'] & GetTrainingGroupHal;

export const GetTrainingGroupRels = {
  links: ['collection', 'self'] as const,
  templates: [] as const,
} as const;

export type GetTrainingGroupLinkRel = typeof GetTrainingGroupRels.links[number];
export type GetTrainingGroupTemplateRel = typeof GetTrainingGroupRels.templates[number];

// --- GET /api/members/{memberId}/account/transactions/{txId} (200) ---

export interface GetTransactionHal {
  _links?: {
    /** The account this transaction belongs to */
    'account'?: HalResourceLinks;
    /** Member resource of the user who recorded this transaction */
    'recordedBy'?: HalResourceLinks;
    /** The reversal transaction, when this transaction has been reversed */
    'reversedBy'?: HalResourceLinks;
    /** The original transaction, when this transaction is itself a reversal */
    'reverses'?: HalResourceLinks;
    /** This transaction */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with FINANCE:MANAGE, on a non-reversal transaction that
has not already been reversed.
 */
    'reverse'?: HalFormsTemplate;
  };
}

export type GetTransactionResource =
  components['schemas']['EntityModelTransactionResource'] & GetTransactionHal;

export const GetTransactionRels = {
  links: ['account', 'recordedBy', 'reversedBy', 'reverses', 'self'] as const,
  templates: ['reverse'] as const,
} as const;

export type GetTransactionLinkRel = typeof GetTransactionRels.links[number];
export type GetTransactionTemplateRel = typeof GetTransactionRels.templates[number];

// --- GET /api/users/{id}/permissions (200) ---

export interface GetUserPermissionsHal {
  _links?: {
    /** This user's permissions */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    'updatePermissions'?: HalFormsTemplate;
  };
}

export type GetUserPermissionsResource =
  components['schemas']['EntityModelPermissionsResponse'] & GetUserPermissionsHal;

export const GetUserPermissionsRels = {
  links: ['self'] as const,
  templates: ['updatePermissions'] as const,
} as const;

export type GetUserPermissionsLinkRel = typeof GetUserPermissionsRels.links[number];
export type GetUserPermissionsTemplateRel = typeof GetUserPermissionsRels.templates[number];

// --- PATCH /api/fee-selection-campaigns/{id}/deadline (200) ---

export interface ChangeDeadlineHal {
  _links?: {
    /** This publication */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type ChangeDeadlineResource =
  components['schemas']['EntityModelFeeSelectionCampaignResponse'] & ChangeDeadlineHal;

export const ChangeDeadlineRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type ChangeDeadlineLinkRel = typeof ChangeDeadlineRels.links[number];
export type ChangeDeadlineTemplateRel = typeof ChangeDeadlineRels.templates[number];

// --- POST /api/events/import-batch (200) ---

export interface ImportEventsBatchHal {
  _links?: {
    /** This import result */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type ImportEventsBatchResource =
  components['schemas']['EntityModelBulkImportResult'] & ImportEventsBatchHal;

export const ImportEventsBatchRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type ImportEventsBatchLinkRel = typeof ImportEventsBatchRels.links[number];
export type ImportEventsBatchTemplateRel = typeof ImportEventsBatchRels.templates[number];

// --- GET /api/calendar-items (200) ---

export interface ListCalendarItemsHal {
  _links?: {
    /** Same range shifted forward by one month */
    'next'?: HalResourceLinks;
    /** Same range shifted back by one month */
    'prev'?: HalResourceLinks;
    /** This collection, with the current date-range, sort and mySchedule parameters */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with CALENDAR:MANAGE */
    'createCalendarItem'?: HalFormsTemplate;
  };
}

export type ListCalendarItemsResource =
  components['schemas']['CollectionModelEntityModelCalendarItemDto'] & ListCalendarItemsHal;

export const ListCalendarItemsRels = {
  links: ['next', 'prev', 'self'] as const,
  templates: ['createCalendarItem'] as const,
} as const;

export type ListCalendarItemsLinkRel = typeof ListCalendarItemsRels.links[number];
export type ListCalendarItemsTemplateRel = typeof ListCalendarItemsRels.templates[number];

// --- GET /api/events (200) ---

export interface ListEventsHal {
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
    /** Present only for callers with EVENTS:MANAGE */
    'createEvent'?: HalFormsTemplate;
    /** Present only when ORIS integration is active and caller has EVENTS:MANAGE */
    'importEvent'?: HalFormsTemplate;
    /** Present only when ORIS integration is active and caller has EVENTS:MANAGE */
    'importEventsBatch'?: HalFormsTemplate;
    /** Present only when ORIS integration is active and caller has EVENTS:MANAGE */
    'syncAllUpcomingFromOris'?: HalFormsTemplate;
  };
}

export type ListEventsResource =
  components['schemas']['PagedModelEntityModelEventSummaryDto'] & ListEventsHal;

export const ListEventsRels = {
  links: ['first', 'last', 'next', 'prev', 'self'] as const,
  templates: ['createEvent', 'importEvent', 'importEventsBatch', 'syncAllUpcomingFromOris'] as const,
} as const;

export type ListEventsLinkRel = typeof ListEventsRels.links[number];
export type ListEventsTemplateRel = typeof ListEventsRels.templates[number];

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

// --- GET /api/family-groups (200) ---

export interface ListFamilyGroupsHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    'createFamilyGroup'?: HalFormsTemplate;
  };
}

export type ListFamilyGroupsResource =
  components['schemas']['CollectionModelEntityModelFamilyGroupSummaryResponse'] & ListFamilyGroupsHal;

export const ListFamilyGroupsRels = {
  links: ['self'] as const,
  templates: ['createFamilyGroup'] as const,
} as const;

export type ListFamilyGroupsLinkRel = typeof ListFamilyGroupsRels.links[number];
export type ListFamilyGroupsTemplateRel = typeof ListFamilyGroupsRels.templates[number];

// --- GET /api/membership-fee-groups/{id}/rules (200) ---

export interface ListGroupRulesHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type ListGroupRulesResource =
  components['schemas']['CollectionModelEntityModelPaymentRuleResponse'] & ListGroupRulesHal;

export const ListGroupRulesRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type ListGroupRulesLinkRel = typeof ListGroupRulesRels.links[number];
export type ListGroupRulesTemplateRel = typeof ListGroupRulesRels.templates[number];

// --- GET /api/groups (200) ---

export interface ListGroupsHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    'createGroup'?: HalFormsTemplate;
  };
}

export type ListGroupsResource =
  components['schemas']['CollectionModelEntityModelGroupSummaryResponse'] & ListGroupsHal;

export const ListGroupsRels = {
  links: ['self'] as const,
  templates: ['createGroup'] as const,
} as const;

export type ListGroupsLinkRel = typeof ListGroupsRels.links[number];
export type ListGroupsTemplateRel = typeof ListGroupsRels.templates[number];

// --- GET /api/fee-selection-campaigns/{year}/levels (200) ---

export interface ListGroupsForYearHal {
  _links?: {
    /** This collection, for the given year */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type ListGroupsForYearResource =
  components['schemas']['CollectionModelEntityModelMembershipFeeGroupResponse'] & ListGroupsForYearHal;

export const ListGroupsForYearRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type ListGroupsForYearLinkRel = typeof ListGroupsForYearRels.links[number];
export type ListGroupsForYearTemplateRel = typeof ListGroupsForYearRels.templates[number];

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
    /** Present only for callers with MEMBERS:MANAGE */
    'registerMember'?: HalFormsTemplate;
    /** Edit template used by the table's inline row editor */
    'updateMember'?: HalFormsTemplate;
  };
}

export type ListMembersResource =
  components['schemas']['PagedModelEntityModelMemberSummaryResponse'] & ListMembersHal;

export const ListMembersRels = {
  links: ['first', 'last', 'next', 'prev', 'self'] as const,
  templates: ['registerMember', 'updateMember'] as const,
} as const;

export type ListMembersLinkRel = typeof ListMembersRels.links[number];
export type ListMembersTemplateRel = typeof ListMembersRels.templates[number];

// --- GET /api/category-presets (200) ---

export interface ListPresetsHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Create a new category preset */
    'createCategoryPreset'?: HalFormsTemplate;
  };
}

export type ListPresetsResource =
  components['schemas']['CollectionModelEntityModelCategoryPresetDto'] & ListPresetsHal;

export const ListPresetsRels = {
  links: ['self'] as const,
  templates: ['createCategoryPreset'] as const,
} as const;

export type ListPresetsLinkRel = typeof ListPresetsRels.links[number];
export type ListPresetsTemplateRel = typeof ListPresetsRels.templates[number];

// --- GET /api/fee-selection-campaigns (200) ---

export interface ListPublicationsHal {
  _links?: {
    /** This collection, with the current status filter */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with MEMBERS:MANAGE */
    'publishYear'?: HalFormsTemplate;
  };
}

export type ListPublicationsResource =
  components['schemas']['CollectionModelEntityModelFeeSelectionCampaignResponse'] & ListPublicationsHal;

export const ListPublicationsRels = {
  links: ['self'] as const,
  templates: ['publishYear'] as const,
} as const;

export type ListPublicationsLinkRel = typeof ListPublicationsRels.links[number];
export type ListPublicationsTemplateRel = typeof ListPublicationsRels.templates[number];

// --- GET /api/events/{eventId}/registrations (200) ---

export interface ListRegistrationsHal {
  _links?: {
    /** The event these registrations belong to */
    'event'?: HalResourceLinks;
    /** This collection, with the current sort parameter */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type ListRegistrationsResource =
  components['schemas']['CollectionModelEntityModelRegistrationSummaryDto'] & ListRegistrationsHal;

export const ListRegistrationsRels = {
  links: ['event', 'self'] as const,
  templates: [] as const,
} as const;

export type ListRegistrationsLinkRel = typeof ListRegistrationsRels.links[number];
export type ListRegistrationsTemplateRel = typeof ListRegistrationsRels.templates[number];

// --- GET /api/membership-fee-tiers/{id}/rules (200) ---

export interface ListRulesHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with MEMBERS:MANAGE */
    'addRule'?: HalFormsTemplate;
  };
}

export type ListRulesResource =
  components['schemas']['CollectionModelEntityModelPaymentRuleResponse'] & ListRulesHal;

export const ListRulesRels = {
  links: ['self'] as const,
  templates: ['addRule'] as const,
} as const;

export type ListRulesLinkRel = typeof ListRulesRels.links[number];
export type ListRulesTemplateRel = typeof ListRulesRels.templates[number];

// --- GET /api/membership-fee-tiers (200) ---

export interface ListTiersHal {
  _links?: {
    /** Present only for callers with MEMBERS:MANAGE, when a campaign is active */
    'activeCampaign'?: HalResourceLinks;
    /** Present only for callers with MEMBERS:MANAGE */
    'pastCampaigns'?: HalResourceLinks;
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    /** Present only for callers with MEMBERS:MANAGE */
    'createTier'?: HalFormsTemplate;
    /** Present only for callers with MEMBERS:MANAGE */
    'publishYear'?: HalFormsTemplate;
  };
}

export type ListTiersResource =
  components['schemas']['CollectionModelEntityModelMembershipFeeTierSummaryResponse'] & ListTiersHal;

export const ListTiersRels = {
  links: ['activeCampaign', 'pastCampaigns', 'self'] as const,
  templates: ['createTier', 'publishYear'] as const,
} as const;

export type ListTiersLinkRel = typeof ListTiersRels.links[number];
export type ListTiersTemplateRel = typeof ListTiersRels.templates[number];

// --- GET /api/training-groups (200) ---

export interface ListTrainingGroupsHal {
  _links?: {
    /** This collection */
    'self'?: HalResourceLinks;
  };
  _templates?: {
    'createTrainingGroup'?: HalFormsTemplate;
  };
}

export type ListTrainingGroupsResource =
  components['schemas']['CollectionModelEntityModelTrainingGroupSummaryResponse'] & ListTrainingGroupsHal;

export const ListTrainingGroupsRels = {
  links: ['self'] as const,
  templates: ['createTrainingGroup'] as const,
} as const;

export type ListTrainingGroupsLinkRel = typeof ListTrainingGroupsRels.links[number];
export type ListTrainingGroupsTemplateRel = typeof ListTrainingGroupsRels.templates[number];

// --- GET /api/members/{memberId}/account/transactions (200) ---

export interface ListTransactionsHal {
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
  _templates?: Record<never, never>;
}

export type ListTransactionsResource =
  components['schemas']['PagedModelEntityModelTransactionResource'] & ListTransactionsHal;

export const ListTransactionsRels = {
  links: ['first', 'last', 'next', 'prev', 'self'] as const,
  templates: [] as const,
} as const;

export type ListTransactionsLinkRel = typeof ListTransactionsRels.links[number];
export type ListTransactionsTemplateRel = typeof ListTransactionsRels.templates[number];

// --- GET /api (200) ---

export interface RootNavigationHal {
  _links?: {
    /** Present only when authenticated as the hardcoded "admin" username */
    'admin'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type RootNavigationResource =
  components['schemas']['EntityModelRootModel'] & RootNavigationHal;

export const RootNavigationRels = {
  links: ['admin'] as const,
  templates: [] as const,
} as const;

export type RootNavigationLinkRel = typeof RootNavigationRels.links[number];
export type RootNavigationTemplateRel = typeof RootNavigationRels.templates[number];

// --- POST /api/events/sync-from-oris/all-upcoming (200) ---

export interface SyncAllUpcomingFromOrisHal {
  _links?: {
    /** This sync result */
    'self'?: HalResourceLinks;
  };
  _templates?: Record<never, never>;
}

export type SyncAllUpcomingFromOrisResource =
  components['schemas']['EntityModelBulkSyncResult'] & SyncAllUpcomingFromOrisHal;

export const SyncAllUpcomingFromOrisRels = {
  links: ['self'] as const,
  templates: [] as const,
} as const;

export type SyncAllUpcomingFromOrisLinkRel = typeof SyncAllUpcomingFromOrisRels.links[number];
export type SyncAllUpcomingFromOrisTemplateRel = typeof SyncAllUpcomingFromOrisRels.templates[number];
