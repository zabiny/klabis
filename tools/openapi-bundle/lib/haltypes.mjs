/**
 * Generates TypeScript declarations from the x-hal-* extensions.
 *
 * openapi-typescript only understands standard OpenAPI, so link relations and HAL-FORMS template
 * names — the part of the contract the Klabis frontend actually navigates by — never reach the
 * generated types. This fills that gap.
 *
 * What is generated is the *union of rel names*, not a presence guarantee: klabisLinkTo returns
 * Optional and klabisAfford returns an empty list when the caller lacks authorization, so every
 * relation is optional at runtime. The value is catching typos and offering autocomplete.
 */

import {isEnvelopeShaped, resolveSchema, schemaName} from './derive.mjs';

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

const HTTP_METHODS = ['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace'];

/**
 * Content types that actually carry the HAL envelope (`_links`/`_embedded`/`page`) on the wire.
 * `*Resource` must be typed off one of these, never off a same-response `application/json` sibling
 * (added for backend codegen — see klabis-api-spec skill) or the bare payload/array shape wins, since
 * the bundler alphabetizes content keys and "application/json" always sorts first.
 */
const HAL_CONTENT_TYPES = ['application/prs.hal-forms+json', 'application/hal+json'];

/** getMember -> GetMember */
const pascalCase = (s) => s.charAt(0).toUpperCase() + s.slice(1);

const quote = (s) => `'${s.replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`;

const relNames = (rels) => (isPlainObject(rels) ? Object.keys(rels).sort() : []);

/** The row schema of a response's `_embedded`: the declared items payload, or EntityModel of the array's items. */
function responseRowSchema(response, content, schemas) {
    if (isPlainObject(response['x-hal-embedded']) && typeof response['x-hal-embedded'].items === 'string') {
        return response['x-hal-embedded'].items;
    }
    const jsonSchema = content['application/json']?.schema;
    const itemName = jsonSchema?.type === 'array' ? schemaName(jsonSchema.items) : undefined;
    if (itemName === undefined) return undefined;
    // The deriver leaves an already-enveloped items payload as it is.
    return isEnvelopeShaped(resolveSchema({components: {schemas}}, jsonSchema.items)) ? itemName : `EntityModel${itemName}`;
}

/**
 * Array properties carrying x-hal-item-* rels, per schema that declares them. The deriver has
 * already retargeted their `items` at the EntityModel wrapper, which is the row schema.
 */
function collectPropertyItems(document) {
    const bySchema = new Map();
    for (const [owner, schema] of Object.entries(document?.components?.schemas ?? {})) {
        const members = [schema, ...(Array.isArray(schema?.allOf) ? schema.allOf : [])];
        const found = [];
        for (const member of members) {
            if (!isPlainObject(member?.properties)) continue;
            for (const [property, definition] of Object.entries(member.properties)) {
                const links = relNames(definition?.['x-hal-item-links']);
                const templates = relNames(definition?.['x-hal-item-templates']);
                const schemaOfItems = schemaName(definition?.items);
                if ((links.length === 0 && templates.length === 0) || schemaOfItems === undefined) continue;
                found.push({
                    property,
                    required: members.some((m) => Array.isArray(m?.required) && m.required.includes(property)),
                    schema: schemaOfItems,
                    links,
                    templates,
                    descriptions: {
                        links: describe(definition['x-hal-item-links']),
                        templates: describe(definition['x-hal-item-templates']),
                    },
                });
            }
        }
        if (found.length > 0) bySchema.set(owner, found);
    }
    return bySchema;
}

/** Property items reachable from a resource schema through `allOf` refs. */
function propertyItemsOf(rootName, document, bySchema) {
    const found = new Map();
    const visited = new Set();
    const visit = (name) => {
        if (name === undefined || visited.has(name)) return;
        visited.add(name);
        for (const item of bySchema.get(name) ?? []) {
            if (!found.has(item.property)) found.set(item.property, item);
        }
        for (const member of document.components?.schemas?.[name]?.allOf ?? []) visit(schemaName(member));
    };
    visit(rootName);
    return [...found.values()];
}

/**
 * Collects one descriptor per operation response that declares x-hal-links, x-hal-templates or
 * row relations (x-hal-item-links / x-hal-item-templates).
 *
 * @returns {Array<{operationId, status, schema, links: string[], templates: string[], descriptions,
 *   itemLinks: string[], itemTemplates: string[], itemDescriptions, itemSchema, propertyItems}>}
 */
export function collectHalResources(document) {
    const resources = [];
    const propertyItemsBySchema = collectPropertyItems(document);

    for (const [path, pathItem] of Object.entries(document?.paths ?? {})) {
        if (!isPlainObject(pathItem)) continue;

        for (const method of HTTP_METHODS) {
            const operation = pathItem[method];
            if (!isPlainObject(operation) || !isPlainObject(operation.responses)) continue;

            for (const [status, response] of Object.entries(operation.responses)) {
                if (!isPlainObject(response)) continue;

                const linkRels = relNames(response['x-hal-links']);
                const templateRels = relNames(response['x-hal-templates']);
                const itemLinkRels = relNames(response['x-hal-item-links']);
                const itemTemplateRels = relNames(response['x-hal-item-templates']);

                const content = isPlainObject(response.content) ? response.content : {};
                const halSchemas = HAL_CONTENT_TYPES
                    .map((type) => schemaName(content[type]?.schema))
                    .filter(Boolean);
                // Fallback for a response with no HAL content type carrying a schema (shouldn't
                // happen for anything declaring x-hal-links/x-hal-templates, but stay safe).
                const anySchemas = Object.values(content).map((c) => schemaName(c?.schema)).filter(Boolean);

                const schema = halSchemas[0] ?? anySchemas[0];
                const propertyItems = propertyItemsOf(schema, document, propertyItemsBySchema);

                if (linkRels.length === 0 && templateRels.length === 0
                    && itemLinkRels.length === 0 && itemTemplateRels.length === 0
                    && propertyItems.length === 0) continue;

                resources.push({
                    operationId: operation.operationId ?? `${method}${path}`,
                    path,
                    method,
                    status,
                    schema,
                    links: linkRels,
                    templates: templateRels,
                    descriptions: {
                        links: describe(response['x-hal-links']),
                        templates: describe(response['x-hal-templates']),
                    },
                    itemLinks: itemLinkRels,
                    itemTemplates: itemTemplateRels,
                    itemDescriptions: {
                        links: describe(response['x-hal-item-links']),
                        templates: describe(response['x-hal-item-templates']),
                    },
                    itemSchema: responseRowSchema(response, content, document.components?.schemas),
                    propertyItems,
                });
            }
        }
    }

    return resources.sort((a, b) => a.operationId.localeCompare(b.operationId));
}

function describe(rels) {
    if (!isPlainObject(rels)) return {};
    return Object.fromEntries(
        Object.entries(rels)
            .map(([rel, d]) => [rel, isPlainObject(d) ? d.description : undefined])
            .filter(([, description]) => typeof description === 'string'),
    );
}

function renderRelBlock(kind, rels, descriptions, indent) {
    if (rels.length === 0) return `${indent}${kind}?: Record<never, never>;`;

    const lines = [];
    for (const rel of rels) {
        const description = descriptions[rel];
        if (description) lines.push(`${indent}  /** ${description} */`);
        lines.push(`${indent}  ${quote(rel)}?: ${kind === '_links' ? 'HalResourceLinks' : 'HalFormsTemplate'};`);
    }
    return [`${indent}${kind}?: {`, ...lines, `${indent}};`].join('\n');
}

/**
 * @param document bundled OpenAPI document
 * @returns {string} contents of halTypes.d.ts
 */
export function renderHalTypes(document) {
    const resources = collectHalResources(document);

    const header = [
        '/**',
        ' * Generated from x-hal-links / x-hal-templates in docs/openapi/spec/.',
        ' * Do not edit — run `npm run openapi` instead.',
        ' *',
        ' * Every relation is optional: links and templates the caller is not authorized for are',
        ' * absent from the response, so these types describe the maximal variant. Use the *Rels',
        ' * constants instead of string literals so a renamed relation fails the build.',
        ' */',
        '',
        "import type {HalFormsTemplate, HalResourceLinks} from './types';",
        "import type {components} from './klabisApi';",
        '',
    ];

    if (resources.length === 0) {
        return `${[...header, '// No x-hal-* declarations found in the spec yet.', 'export {};', ''].join('\n')}`;
    }

    const blocks = resources.flatMap((resource) => {
        const name = pascalCase(resource.operationId);
        const result = [];

        const pageRels = {links: resource.links, templates: resource.templates, descriptions: resource.descriptions};
        const emitsPageTypes = resource.links.length > 0 || resource.templates.length > 0
            || resource.propertyItems.length > 0;

        if (emitsPageTypes) {
            result.push([
                `// --- ${resource.method.toUpperCase()} ${resource.path} (${resource.status}) ---`,
                '',
                renderResourceBlock(name, resource.schema, pageRels, {
                    propertyItems: resource.propertyItems,
                    withRelAliases: true,
                }),
            ].join('\n'));
        }

        if (resource.itemLinks.length > 0 || resource.itemTemplates.length > 0) {
            result.push(renderResourceBlock(`${name}Item`, resource.itemSchema, {
                links: resource.itemLinks,
                templates: resource.itemTemplates,
                descriptions: resource.itemDescriptions,
            }));
        }

        if (emitsPageTypes) {
            for (const item of resource.propertyItems) {
                result.push(renderResourceBlock(`${name}${pascalCase(item.property)}Item`, item.schema, item));
            }
        }

        return result;
    });

    return [...header, ...blocks].join('\n');
}

/**
 * The `<name>Hal` interface, the `<name>Resource` alias and the `<name>Rels` constant for one
 * resource, page or row. `propertyItems` swap array properties of the base schema for row types.
 */
function renderResourceBlock(name, schema, {links, templates, descriptions}, {propertyItems = [], withRelAliases = false} = {}) {
    const lines = [
        `export interface ${name}Hal {`,
        renderRelBlock('_links', links, descriptions.links, '  '),
        renderRelBlock('_templates', templates, descriptions.templates, '  '),
        '}',
        '',
    ];

    if (schema) {
        const base = `components['schemas'][${quote(schema)}]`;
        if (propertyItems.length === 0) {
            lines.push(`export type ${name}Resource =`, `  ${base} & ${name}Hal;`, '');
        } else {
            const omitted = propertyItems.map((item) => quote(item.property)).join(' | ');
            const overrides = propertyItems
                .map((item) => `${item.property}${item.required ? '' : '?'}: ${name}${pascalCase(item.property)}ItemResource[]`)
                .join('; ');
            lines.push(
                `export type ${name}Resource =`,
                `  Omit<${base}, ${omitted}>`,
                `  & {${overrides}}`,
                `  & ${name}Hal;`,
                '',
            );
        }
    }

    if (links.length > 0 || templates.length > 0) {
        lines.push(
            `export const ${name}Rels = {`,
            `  links: [${links.map(quote).join(', ')}] as const,`,
            `  templates: [${templates.map(quote).join(', ')}] as const,`,
            '} as const;',
            '',
        );
        if (withRelAliases) {
            lines.push(
                `export type ${name}LinkRel = typeof ${name}Rels.links[number];`,
                `export type ${name}TemplateRel = typeof ${name}Rels.templates[number];`,
                '',
            );
        }
    }

    return lines.join('\n');
}
