/**
 * HAL and HATEOAS related constants
 * Used for link relationships, content types, and API patterns
 */

export const HAL_LINK_RELS = {
	SELF: 'self',
	CURIES: 'curies',
} as const;

export const HAL_CONTENT_TYPES = {
	HAL_FORMS: 'application/prs.hal-forms+json; charset=utf-8',
	HAL_JSON: 'application/hal+json',
	PROBLEM_JSON: 'application/problem+json',
	HAL_FORMS_AND_JSON: 'application/prs.hal-forms+json,application/hal+json',
} as const;

export const HAL_PROPERTY_FILTERS = {
	// Properties starting with underscore are HAL metadata
	EXCLUDE_PREFIX: '_',
} as const;

/**
 * Special property names in HAL responses
 */
export const HAL_PROPERTIES = {
	LINKS: '_links',
	EMBEDDED: '_embedded',
	TEMPLATES: '_templates',
} as const;
