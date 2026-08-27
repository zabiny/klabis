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

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

const HTTP_METHODS = ['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace'];

/** `#/components/schemas/EntityModelMemberDetailsResponse` -> `EntityModelMemberDetailsResponse` */
const refName = (ref) => (typeof ref === 'string' ? ref.split('/').pop() : undefined);

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

/**
 * Collects one descriptor per operation that declares x-hal-links or x-hal-templates.
 *
 * @returns {Array<{operationId, status, schema, links: string[], templates: string[], descriptions}>}
 */
export function collectHalResources(document) {
    const resources = [];

    for (const [path, pathItem] of Object.entries(document?.paths ?? {})) {
        if (!isPlainObject(pathItem)) continue;

        for (const method of HTTP_METHODS) {
            const operation = pathItem[method];
            if (!isPlainObject(operation) || !isPlainObject(operation.responses)) continue;

            for (const [status, response] of Object.entries(operation.responses)) {
                if (!isPlainObject(response)) continue;

                const linkRels = isPlainObject(response['x-hal-links'])
                    ? Object.keys(response['x-hal-links']) : [];
                const templateRels = isPlainObject(response['x-hal-templates'])
                    ? Object.keys(response['x-hal-templates']) : [];

                if (linkRels.length === 0 && templateRels.length === 0) continue;

                const content = isPlainObject(response.content) ? response.content : {};
                const halSchemas = HAL_CONTENT_TYPES
                    .map((type) => refName(content[type]?.schema?.$ref))
                    .filter(Boolean);
                // Fallback for a response with no HAL content type carrying a schema (shouldn't
                // happen for anything declaring x-hal-links/x-hal-templates, but stay safe).
                const anySchemas = Object.values(content).map((c) => refName(c?.schema?.$ref)).filter(Boolean);

                resources.push({
                    operationId: operation.operationId ?? `${method}${path}`,
                    path,
                    method,
                    status,
                    schema: halSchemas[0] ?? anySchemas[0],
                    links: linkRels.sort(),
                    templates: templateRels.sort(),
                    descriptions: {
                        links: describe(response['x-hal-links']),
                        templates: describe(response['x-hal-templates']),
                    },
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

    const blocks = resources.map((resource) => {
        const name = pascalCase(resource.operationId);
        const lines = [
            `// --- ${resource.method.toUpperCase()} ${resource.path} (${resource.status}) ---`,
            '',
            `export interface ${name}Hal {`,
            renderRelBlock('_links', resource.links, resource.descriptions.links, '  '),
            renderRelBlock('_templates', resource.templates, resource.descriptions.templates, '  '),
            '}',
            '',
        ];

        if (resource.schema) {
            lines.push(
                `export type ${name}Resource =`,
                `  components['schemas'][${quote(resource.schema)}] & ${name}Hal;`,
                '',
            );
        }

        lines.push(
            `export const ${name}Rels = {`,
            `  links: [${resource.links.map(quote).join(', ')}] as const,`,
            `  templates: [${resource.templates.map(quote).join(', ')}] as const,`,
            '} as const;',
            '',
            `export type ${name}LinkRel = typeof ${name}Rels.links[number];`,
            `export type ${name}TemplateRel = typeof ${name}Rels.templates[number];`,
            '',
        );

        return lines.join('\n');
    });

    return [...header, ...blocks].join('\n');
}
