/**
 * Derives HAL envelopes into the bundled document.
 *
 * A source spec states the payload once; this module reconstructs the `EntityModel` /
 * `CollectionModel` / `PagedModel` schemas and the `application/prs.hal-forms+json` content entry
 * that the frontend types and Swagger UI are generated from. It is the only place in the repo that
 * encodes HAL envelope structure — see openspec/changes/derive-hal-envelopes-in-bundler/design.md.
 *
 * Crucially it is a **no-op on already-enveloped input**: a response that already carries a
 * hal-forms entry, and a schema already shaped as an envelope, are left exactly as they are. That
 * property is what lets hand-written and derived modules coexist while the specs are migrated.
 */

import {HTTP_METHODS, sortKeysDeep} from './bundle.mjs';

const HAL_FORMS = 'application/prs.hal-forms+json';
const JSON_MEDIA = 'application/json';

const LINKS_REF = '#/components/schemas/Links';
const TEMPLATES_REF = '#/components/schemas/HalFormsTemplates';
const PAGE_METADATA_REF = '#/components/schemas/PageMetadata';

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

const uncapitalize = (name) => name.charAt(0).toLowerCase() + name.slice(1);

/** Local `#/components/schemas/X` ref -> `X`; anything else -> undefined. */
function schemaName(node) {
    if (!isPlainObject(node) || typeof node.$ref !== 'string') return undefined;
    const match = node.$ref.match(/^#\/components\/schemas\/(.+)$/);
    return match ? match[1] : undefined;
}

const schemaRef = (name) => ({$ref: `#/components/schemas/${name}`});

/**
 * Recognises a schema the deriver would itself have produced, so it is not wrapped a second time.
 *
 * Both real shapes count: the `allOf` form an EntityModel takes, and the flat `{type, properties}`
 * form of a collection envelope — the latter also covering the hand-written marker types
 * (`EntityModelRootModel`, `EntityModelDashboardModel`) which have `schemaMappings` in
 * build.gradle.kts and must survive untouched.
 *
 * It is a shape test, not a declaration, so a genuine payload owning a `_links`/`_embedded` property
 * of its own would be mistaken for an envelope and skipped in silence. `validate.mjs` catches that —
 * see enveloped-payload-schema errors there.
 */
export function isEnvelopeShaped(schema) {
    if (!isPlainObject(schema)) return false;

    if (Array.isArray(schema.allOf)) {
        return schema.allOf.some((member) => isPlainObject(member?.properties)
            && Object.hasOwn(member.properties, '_links'));
    }

    return isPlainObject(schema.properties)
        && (Object.hasOwn(schema.properties, '_links') || Object.hasOwn(schema.properties, '_embedded'));
}

/**
 * The `_embedded` key, per Decision 5: declared `x-klabis-relation.collectionRelation` wins,
 * otherwise Spring HATEOAS's default. The default is purely lexical — `EventSummaryDto` becomes
 * `eventSummaryDtoList`, the suffix carried through unchanged — and is verified against live
 * responses (tasks 1.1/1.2), not assumed.
 */
export function embeddedKey(payloadName, payloadSchema) {
    const declared = payloadSchema?.['x-klabis-relation']?.collectionRelation;
    if (typeof declared === 'string' && declared !== '') return declared;
    return `${uncapitalize(payloadName)}List`;
}

function entityModel(payloadName, {templates}) {
    const wrapper = {type: 'object', properties: {_links: {$ref: LINKS_REF}}};
    if (templates) wrapper.properties._templates = {$ref: TEMPLATES_REF};
    return {allOf: [schemaRef(payloadName), wrapper]};
}

function collectionModel(itemSchemaName, embedded, {paged, templates}) {
    const properties = {
        _embedded: {
            type: 'object',
            properties: {
                [embedded]: {type: 'array', items: schemaRef(itemSchemaName)},
            },
        },
        _links: {$ref: LINKS_REF},
    };
    if (templates) properties._templates = {$ref: TEMPLATES_REF};
    if (paged) properties.page = {$ref: PAGE_METADATA_REF};
    return {type: 'object', properties};
}

/**
 * Registers a derived schema, tolerating a name already taken by an identical definition (two
 * operations returning the same payload derive the same envelope). A name taken by something
 * *different* is a genuine collision the author has to resolve.
 */
function define(schemas, name, definition, collisions) {
    const existing = schemas[name];
    if (existing !== undefined) {
        // Key order carries no meaning in a schema, so compare the canonical shape: a hand-written
        // schema in a not-yet-migrated module must collide on substance, never on ordering.
        if (JSON.stringify(sortKeysDeep(existing)) !== JSON.stringify(sortKeysDeep(definition))) {
            collisions.push({
                name,
                message: `derived envelope "${name}" collides with an existing, differently-shaped schema`,
            });
        }
        return name;
    }
    schemas[name] = definition;
    return name;
}

/**
 * Derives the envelope for one `application/json` response schema.
 *
 * @returns the name of the schema the hal-forms entry should reference, or undefined when the
 *          schema is not something the deriver can wrap (an inline array of inline items, say).
 */
function deriveResponseEnvelope(jsonSchema, schemas, {paged, templates}, collisions) {
    if (jsonSchema.type === 'array') {
        const itemName = schemaName(jsonSchema.items);
        if (itemName === undefined) return undefined;

        const payload = schemas[itemName];
        if (isEnvelopeShaped(payload)) return undefined;

        const item = define(schemas, `EntityModel${itemName}`,
            entityModel(itemName, {templates}), collisions);
        const prefix = paged ? 'PagedModel' : 'CollectionModel';
        return define(schemas, `${prefix}${item}`,
            collectionModel(item, embeddedKey(itemName, payload), {paged, templates}), collisions);
    }

    const payloadName = schemaName(jsonSchema);
    if (payloadName === undefined) return undefined;
    if (isEnvelopeShaped(schemas[payloadName])) return undefined;

    return define(schemas, `EntityModel${payloadName}`,
        entityModel(payloadName, {templates}), collisions);
}

/**
 * `x-hal-entity-items: true` on an array property: its items are independently addressable
 * resources carrying their own `_links`, which is an API design choice rather than something
 * derivable. Emits `EntityModel<Item>` and retargets the items `$ref` at it.
 */
function deriveEntityItems(node, schemas, collisions) {
    if (Array.isArray(node)) {
        for (const item of node) deriveEntityItems(item, schemas, collisions);
        return;
    }
    if (!isPlainObject(node)) return;

    if (node['x-hal-entity-items'] === true && node.type === 'array') {
        const itemName = schemaName(node.items);
        if (itemName !== undefined && !isEnvelopeShaped(schemas[itemName])) {
            // The one exception to Decision 7: nested collection items carry links but no
            // `_templates` — affordances attach to the response, not to an embedded row. This is
            // what keeps the `groups` migration byte-identical.
            const wrapped = define(schemas, `EntityModel${itemName}`,
                entityModel(itemName, {templates: false}), collisions);
            node.items = schemaRef(wrapped);
        }
    }

    for (const value of Object.values(node)) deriveEntityItems(value, schemas, collisions);
}

/**
 * Adds the hal-forms content entry to every response of every HAL operation, and rewrites
 * `x-hal-entity-items` array properties.
 *
 * Mutates `document` in place and returns it. HAL is the default (105 of 111 operations);
 * `x-klabis-hal: false` on the operation is the only way out — see Decision 4.
 *
 * @returns {{document: object, collisions: Array<{name: string, message: string}>}}
 */
export function forEachHalResponse(document, visit) {
    for (const [pathName, pathItem] of Object.entries(document?.paths ?? {})) {
        if (!isPlainObject(pathItem)) continue;

        for (const [method, operation] of Object.entries(pathItem)) {
            if (!HTTP_METHODS.includes(method) || !isPlainObject(operation)) continue;
            if (operation['x-klabis-hal'] === false) continue;

            const paged = operation['x-spring-paginated'] === true;

            for (const [status, response] of Object.entries(operation.responses ?? {})) {
                // Only success payloads are hypermedia resources. Error bodies are declared as
                // problem+json, except `suspendMember`'s 409 warning which is plain
                // application/json — enveloping that would invent a schema nothing serves.
                if (!/^2\d\d$/.test(status)) continue;

                const content = response?.content;
                if (!isPlainObject(content)) continue;
                // Already hand-enveloped — the migration's coexistence guarantee.
                if (Object.hasOwn(content, HAL_FORMS)) continue;

                const jsonSchema = content[JSON_MEDIA]?.schema;
                if (!isPlainObject(jsonSchema)) continue;

                visit({
                    content,
                    jsonSchema,
                    paged,
                    templates: isPlainObject(response['x-hal-templates']),
                    where: `${method.toUpperCase()} ${pathName} ${status}`,
                });
            }
        }
    }
}

/**
 * The payload schema names a HAL response would be enveloped from: the item name for an array,
 * the schema name otherwise. Empty when the deriver could not name one (an inline items schema).
 */
export function halResponsePayloadNames(jsonSchema) {
    const name = jsonSchema.type === 'array'
        ? schemaName(jsonSchema.items)
        : schemaName(jsonSchema);
    return name === undefined ? [] : [name];
}

export function deriveHalEnvelopes(document) {
    const collisions = [];
    const schemas = document?.components?.schemas;
    if (!isPlainObject(schemas)) return {document, collisions};

    forEachHalResponse(document, ({content, jsonSchema, paged, templates}) => {
        const derived = deriveResponseEnvelope(jsonSchema, schemas, {paged, templates}, collisions);
        if (derived === undefined) return;
        content[HAL_FORMS] = {schema: schemaRef(derived)};
    });

    deriveEntityItems(schemas, schemas, collisions);

    return {document, collisions};
}
