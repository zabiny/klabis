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
    'x-klabis-not-blank',
    'x-klabis-past',
]);

export const HALFORMS_ACCESS_VALUES = new Set(['READ_ONLY', 'NONE', 'READ_WRITE', 'DEFAULT']);

/**
 * Boolean flags standing in for Bean Validation constraints OpenAPI cannot express. They are
 * emitted by the overridden pojo.mustache, which covers schema properties only.
 */
const PROPERTY_ONLY_CONSTRAINT_EXTENSIONS = new Set(['x-klabis-not-blank', 'x-klabis-past']);

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

/**
 * Resolves a parameter object, following a local `#/components/parameters/X` $ref one level
 * (operation parameter lists never nest $refs further than that in this codebase).
 */
function resolveParameter(document, param) {
    if (!isPlainObject(param)) return param;
    if (typeof param.$ref !== 'string') return param;
    const match = param.$ref.match(/^#\/components\/parameters\/(.+)$/);
    if (!match) return param;
    return document.components?.parameters?.[match[1]];
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

/**
 * Walks the document, tagging each object node with the context it appears in — 'operation' for
 * the HTTP-method object directly under a path item (`paths/{path}/{method}`), undefined
 * everywhere else. Only that exact node is tagged; nested objects (parameters, responses, request
 * bodies) revert to no context, since `x-klabis-*` on those describes a schema property, not the
 * operation itself.
 */
function walk(node, path, visit, context, parentKind) {
    visit(node, path, context);
    if (Array.isArray(node)) {
        node.forEach((item, i) => walk(item, `${path}[${i}]`, visit, undefined, undefined));
        return;
    }
    if (!isPlainObject(node)) return;

    for (const [key, value] of Object.entries(node)) {
        const childContext = (parentKind === 'pathItem' && HTTP_METHODS.includes(key)) ? 'operation' : undefined;
        const childKind = parentKind === 'paths' ? 'pathItem' : (path === '' && key === 'paths' ? 'paths' : undefined);
        walk(value, `${path}/${key}`, visit, childContext, childKind);
    }
}

/**
 * @returns {Array<{path: string, message: string}>} empty when the document is valid
 */
export function validateSpec(document, {authorities}) {
    const errors = [];
    const operationIds = collectOperationIds(document);

    walk(document, '', (node, path, context) => {
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

            // x-klabis-authority (-> method-level @HasAuthority) and x-klabis-owner-visible
            // (-> paired @OwnerVisible/@OwnerId, see below) make sense directly on an operation.
            // The others describe field-level access on a schema property and have no meaning
            // without a property to attach to.
            if (context === 'operation' && key !== 'x-klabis-authority' && key !== 'x-klabis-owner-visible') {
                errors.push({
                    path: `${path}/${key}`,
                    message: `"${key}" is not valid on an operation — it is a schema-property extension`,
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

            if (key === 'x-klabis-owner-id' && value !== true) {
                errors.push({path: `${path}/${key}`, message: 'must be true when present'});
            }

            if (PROPERTY_ONLY_CONSTRAINT_EXTENSIONS.has(key)) {
                if (value !== true) {
                    errors.push({path: `${path}/${key}`, message: 'must be true when present'});
                }
                // Only pojo.mustache is overridden, so on a parameter's schema the generator would
                // silently drop these and ship no validation at all. Flag it here rather than
                // letting it pass as a no-op; use a standard keyword (e.g. `pattern`) instead.
                if (path.includes('/parameters')) {
                    errors.push({
                        path: `${path}/${key}`,
                        message: `"${key}" is not honoured on a parameter — use a standard OpenAPI keyword instead`,
                    });
                }
            }

            if (key === 'x-klabis-owner-visible' && context === 'operation') {
                // On an operation the value names the parameter that carries the owner ID — this
                // is what lets the bundler generate @OwnerVisible and @OwnerId as an inseparable
                // pair (see applyOperationOwnerVisibleAnnotations in bundle.mjs). On a schema
                // property (context undefined) it stays the boolean field-level flag, handled below.
                //
                // The parameter-exists check below is defence in depth: validateSpec runs on an
                // already-bundled document, and applyOperationOwnerVisibleAnnotations throws on the
                // same mistake first, so the CLI reports that throw rather than this error. It stays
                // for callers that validate a document without bundling it.
                if (typeof value !== 'string' || value.length === 0) {
                    errors.push({
                        path: `${path}/${key}`,
                        message: 'must be the name of a parameter declared on this operation',
                    });
                } else {
                    const params = Array.isArray(node.parameters) ? node.parameters : [];
                    const names = params
                        .map((p) => resolveParameter(document, p)?.name)
                        .filter((name) => name !== undefined);
                    if (!names.includes(value)) {
                        errors.push({
                            path: `${path}/${key}`,
                            message: `"${value}" does not match any parameter declared on this operation`,
                        });
                    }
                }
            } else if (key === 'x-klabis-owner-visible' && value !== true) {
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
