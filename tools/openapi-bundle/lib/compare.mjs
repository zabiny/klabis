/**
 * Migration-only comparison between the springdoc (code-first) output and the hand-written spec.
 *
 * Deliberately narrow: it only compares what springdoc is capable of expressing. Anything springdoc
 * cannot see (HAL links, _templates, x-* extensions) is out of scope, and prose fields are ignored
 * because they legitimately differ.
 *
 * Deleted together with the springdoc plugin once the migration completes.
 */

import {HTTP_METHODS} from './bundle.mjs';

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

/** `#/components/schemas/MemberDetailsResponse` -> `MemberDetailsResponse` */
const refName = (ref) => (typeof ref === 'string' ? ref.split('/').pop() : undefined);

/**
 * Follows a local `#/components/...` reference.
 *
 * Parameters and responses are compared structurally, so a `$ref` to a reusable component has to
 * be dereferenced first — otherwise a hand-written spec that factors shared parameters out looks
 * like it declares nothing at all. Schemas are deliberately NOT dereferenced: they are compared by
 * name (see schemaFingerprint), which is both cheaper and enough to spot a changed payload type.
 */
function deref(node, document, seen = new Set()) {
    if (!isPlainObject(node) || typeof node.$ref !== 'string') return node;
    if (!node.$ref.startsWith('#/components/') || seen.has(node.$ref)) return node;

    const segments = node.$ref.replace(/^#\//, '').split('/');
    let target = document;
    for (const segment of segments) {
        if (!isPlainObject(target)) return node;
        target = target[segment];
    }
    if (target === undefined) return node;

    return deref(target, document, new Set(seen).add(node.$ref));
}

function schemaFingerprint(schema) {
    if (!isPlainObject(schema)) return undefined;
    if (schema.$ref) return refName(schema.$ref);
    if (schema.type === 'array') return `array<${schemaFingerprint(schema.items) ?? '?'}>`;
    return schema.format ? `${schema.type}:${schema.format}` : schema.type;
}

function normalizeParameters(parameters, document) {
    if (!Array.isArray(parameters)) return [];
    return parameters
        .map((p) => deref(p, document))
        .filter(isPlainObject)
        .map((p) => ({
            name: p.name,
            in: p.in,
            required: p.required === true,
            type: schemaFingerprint(p.schema),
        }))
        .sort((a, b) => `${a.in}:${a.name}`.localeCompare(`${b.in}:${b.name}`));
}

function normalizeRequestBody(rawRequestBody, document) {
    const requestBody = deref(rawRequestBody, document);
    if (!isPlainObject(requestBody)) return undefined;
    const content = requestBody.content;
    if (!isPlainObject(content)) return undefined;
    return {
        required: requestBody.required === true,
        mediaTypes: Object.keys(content).sort(),
        schemas: Object.fromEntries(
            Object.entries(content)
                .map(([mediaType, def]) => [mediaType, schemaFingerprint(def?.schema)])
                .sort(([a], [b]) => a.localeCompare(b)),
        ),
    };
}

function normalizeResponses(responses, document) {
    if (!isPlainObject(responses)) return {};
    return Object.fromEntries(
        Object.entries(responses)
            .map(([status, rawDef]) => {
                const def = deref(rawDef, document);
                const content = isPlainObject(def) && isPlainObject(def.content) ? def.content : {};
                return [status, {
                    mediaTypes: Object.keys(content).sort(),
                    schemas: Object.fromEntries(
                        Object.entries(content)
                            .map(([mediaType, d]) => [mediaType, schemaFingerprint(d?.schema)])
                            .sort(([a], [b]) => a.localeCompare(b)),
                    ),
                }];
            })
            .sort(([a], [b]) => a.localeCompare(b)),
    );
}

/** Flattens a document into `"GET /api/members" -> normalized operation`. */
export function normalizeOperations(document) {
    const result = new Map();
    for (const [path, pathItem] of Object.entries(document?.paths ?? {})) {
        if (!isPlainObject(pathItem)) continue;
        const shared = normalizeParameters(pathItem.parameters, document);
        for (const method of HTTP_METHODS) {
            const operation = pathItem[method];
            if (!isPlainObject(operation)) continue;
            const own = normalizeParameters(operation.parameters, document);
            const merged = [...shared, ...own].sort((a, b) =>
                `${a.in}:${a.name}`.localeCompare(`${b.in}:${b.name}`));
            result.set(`${method.toUpperCase()} ${path}`, {
                operationId: operation.operationId,
                parameters: merged,
                requestBody: normalizeRequestBody(operation.requestBody, document),
                responses: normalizeResponses(operation.responses, document),
            });
        }
    }
    return result;
}

function diffFields(codeFirst, specFirst) {
    const differences = [];
    for (const field of ['parameters', 'requestBody', 'responses']) {
        const a = JSON.stringify(codeFirst[field] ?? null);
        const b = JSON.stringify(specFirst[field] ?? null);
        if (a !== b) differences.push({field, codeFirst: codeFirst[field], specFirst: specFirst[field]});
    }
    return differences;
}

/**
 * @returns {{missingInSpec: string[], extraInSpec: string[], mismatched: Array, matched: number}}
 */
export function compareDocuments(codeFirstDoc, specFirstDoc) {
    const codeFirst = normalizeOperations(codeFirstDoc);
    const specFirst = normalizeOperations(specFirstDoc);

    const missingInSpec = [];
    const mismatched = [];
    let matched = 0;

    for (const [key, codeOperation] of codeFirst) {
        const specOperation = specFirst.get(key);
        if (!specOperation) {
            missingInSpec.push(key);
            continue;
        }
        const differences = diffFields(codeOperation, specOperation);
        if (differences.length > 0) mismatched.push({operation: key, differences});
        else matched++;
    }

    const extraInSpec = [...specFirst.keys()].filter((key) => !codeFirst.has(key));

    return {
        missingInSpec: missingInSpec.sort(),
        extraInSpec: extraInSpec.sort(),
        mismatched: mismatched.sort((a, b) => a.operation.localeCompare(b.operation)),
        matched,
    };
}
