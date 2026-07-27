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

function schemaFingerprint(schema) {
    if (!isPlainObject(schema)) return undefined;
    if (schema.$ref) return refName(schema.$ref);
    if (schema.type === 'array') return `array<${schemaFingerprint(schema.items) ?? '?'}>`;
    return schema.format ? `${schema.type}:${schema.format}` : schema.type;
}

function normalizeParameters(parameters) {
    if (!Array.isArray(parameters)) return [];
    return parameters
        .filter(isPlainObject)
        .map((p) => ({
            name: p.name,
            in: p.in,
            required: p.required === true,
            type: schemaFingerprint(p.schema),
        }))
        .sort((a, b) => `${a.in}:${a.name}`.localeCompare(`${b.in}:${b.name}`));
}

function normalizeRequestBody(requestBody) {
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

function normalizeResponses(responses) {
    if (!isPlainObject(responses)) return {};
    return Object.fromEntries(
        Object.entries(responses)
            .map(([status, def]) => {
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
        const shared = normalizeParameters(pathItem.parameters);
        for (const method of HTTP_METHODS) {
            const operation = pathItem[method];
            if (!isPlainObject(operation)) continue;
            const own = normalizeParameters(operation.parameters);
            const merged = [...shared, ...own].sort((a, b) =>
                `${a.in}:${a.name}`.localeCompare(`${b.in}:${b.name}`));
            result.set(`${method.toUpperCase()} ${path}`, {
                operationId: operation.operationId,
                parameters: merged,
                requestBody: normalizeRequestBody(operation.requestBody),
                responses: normalizeResponses(operation.responses),
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
