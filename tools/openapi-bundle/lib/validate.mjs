import {readFileSync} from 'node:fs';

import {HTTP_METHODS} from './bundle.mjs';

/**
 * Validation of Klabis-specific OpenAPI extensions.
 *
 * These checks exist because a mistake here surfaces late and confusingly: a typo in
 * `x-klabis-authority` becomes a compile error inside generated code, and an unknown
 * `x-klabis-*` key would otherwise be silently ignored by the generator.
 */

export const KNOWN_KLABIS_EXTENSIONS = new Set([
    'x-klabis-owner-id',
    'x-klabis-owner-visible',
    'x-klabis-authority',
    'x-klabis-halforms-access',
]);

export const HALFORMS_ACCESS_VALUES = new Set(['READ_ONLY', 'NONE', 'READ_WRITE', 'DEFAULT']);

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

/**
 * Extracts authority enum constant names from Authority.java.
 *
 * Authority is an enum whose constants look like:
 *     MEMBERS_MANAGE("MEMBERS:MANAGE", Scope.CONTEXT_SPECIFIC),
 * The trailing `public static final String *_SCOPE` constants are deliberately NOT authorities
 * and must not be accepted.
 */
export function parseAuthorities(authorityJavaSource) {
    const body = authorityJavaSource.slice(authorityJavaSource.indexOf('public enum Authority'));
    const enumBody = body.slice(0, body.indexOf(';'));
    return new Set([...enumBody.matchAll(/^\s*([A-Z][A-Z0-9_]*)\s*\(/gm)].map((m) => m[1]));
}

export function loadAuthorities(authorityJavaPath) {
    return parseAuthorities(readFileSync(authorityJavaPath, 'utf8'));
}

/** Collects every operationId present in the document. */
function collectOperationIds(document) {
    const ids = new Set();
    for (const pathItem of Object.values(document.paths ?? {})) {
        if (!isPlainObject(pathItem)) continue;
        for (const [method, operation] of Object.entries(pathItem)) {
            if (!HTTP_METHODS.includes(method) || !isPlainObject(operation)) continue;
            if (typeof operation.operationId === 'string') ids.add(operation.operationId);
        }
    }
    return ids;
}

function walk(node, path, visit) {
    visit(node, path);
    if (Array.isArray(node)) {
        node.forEach((item, i) => walk(item, `${path}[${i}]`, visit));
        return;
    }
    if (!isPlainObject(node)) return;
    for (const [key, value] of Object.entries(node)) {
        walk(value, `${path}/${key}`, visit);
    }
}

/**
 * @returns {Array<{path: string, message: string}>} empty when the document is valid
 */
export function validateSpec(document, {authorities}) {
    const errors = [];
    const operationIds = collectOperationIds(document);

    walk(document, '', (node, path) => {
        if (!isPlainObject(node)) return;

        for (const [key, value] of Object.entries(node)) {
            if (!key.startsWith('x-klabis-')) continue;

            if (!KNOWN_KLABIS_EXTENSIONS.has(key)) {
                errors.push({
                    path: `${path}/${key}`,
                    message: `Unknown extension "${key}". Known: ${[...KNOWN_KLABIS_EXTENSIONS].sort().join(', ')}`,
                });
                continue;
            }

            if (key === 'x-klabis-authority') {
                if (typeof value !== 'string') {
                    errors.push({path: `${path}/${key}`, message: 'must be a string'});
                } else if (!authorities.has(value)) {
                    errors.push({
                        path: `${path}/${key}`,
                        message: `"${value}" is not a constant of Authority.java`,
                    });
                }
            }

            if (key === 'x-klabis-halforms-access' && !HALFORMS_ACCESS_VALUES.has(value)) {
                errors.push({
                    path: `${path}/${key}`,
                    message: `"${value}" is not one of ${[...HALFORMS_ACCESS_VALUES].join(', ')}`,
                });
            }

            if ((key === 'x-klabis-owner-id' || key === 'x-klabis-owner-visible') && value !== true) {
                errors.push({path: `${path}/${key}`, message: 'must be true when present'});
            }
        }

        for (const halKey of ['x-hal-links', 'x-hal-templates']) {
            const rels = node[halKey];
            if (!isPlainObject(rels)) continue;
            for (const [rel, descriptor] of Object.entries(rels)) {
                if (!isPlainObject(descriptor)) continue;
                const op = descriptor.operation;
                if (op === undefined) continue;
                if (typeof op !== 'string') {
                    errors.push({path: `${path}/${halKey}/${rel}`, message: 'operation must be a string'});
                } else if (!operationIds.has(op)) {
                    errors.push({
                        path: `${path}/${halKey}/${rel}`,
                        message: `operation "${op}" does not match any operationId`,
                    });
                }
            }
        }
    });

    return errors;
}
