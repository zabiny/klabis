import {readFileSync} from 'node:fs';

import {HTTP_METHODS, sortKeysDeep} from './bundle.mjs';
import {forEachHalResponse, halResponsePayloadNames, isEnvelopeShaped} from './derive.mjs';

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
    'x-klabis-url',
    'x-klabis-class-constraint',
    'x-klabis-relation',
    'x-klabis-hal',
]);

export const HALFORMS_ACCESS_VALUES = new Set(['READ_ONLY', 'NONE', 'READ_WRITE', 'DEFAULT']);

/** Extensions that belong on the HTTP-method node rather than on a schema property. */
const OPERATION_LEVEL_EXTENSIONS = new Set([
    'x-klabis-authority',
    'x-klabis-owner-visible',
    'x-klabis-hal',
]);

/**
 * Boolean flags standing in for Bean Validation constraints OpenAPI cannot express. They are
 * emitted by the overridden pojo.mustache, which covers schema properties only.
 */
const PROPERTY_ONLY_CONSTRAINT_EXTENSIONS = new Set([
    'x-klabis-not-blank',
    'x-klabis-past',
    'x-klabis-url',
]);

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

/** Collects every operationId present in the document, with the operations declaring it. */
function collectOperationIds(document) {
    const byId = new Map();
    for (const [pathName, pathItem] of Object.entries(document.paths ?? {})) {
        if (!isPlainObject(pathItem)) continue;
        for (const [method, operation] of Object.entries(pathItem)) {
            if (!HTTP_METHODS.includes(method) || !isPlainObject(operation)) continue;
            if (typeof operation.operationId !== 'string') continue;
            const declaredAt = `${method.toUpperCase()} ${pathName}`;
            byId.set(operation.operationId, [...(byId.get(operation.operationId) ?? []), declaredAt]);
        }
    }
    return byId;
}

/**
 * OpenAPI requires operationId to be unique across the whole document, and two modules picking the
 * same name is easy to miss because each file reads fine on its own. It matters beyond conformance:
 * haltypes.mjs derives its exported type names from the operationId, so a collision emits duplicate
 * TypeScript declarations that fail the frontend build rather than anything diagnosable here.
 */
function duplicateOperationIdErrors(operationIds) {
    return [...operationIds]
        .filter(([, declaredAt]) => declaredAt.length > 1)
        .map(([id, declaredAt]) => ({
            path: `/paths (operationId "${id}")`,
            message: `operationId must be unique across the document, but is declared by `
                + `${declaredAt.length} operations: ${declaredAt.join(', ')}`,
        }));
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
 * Catches the deriver's one silent failure mode.
 *
 * `isEnvelopeShaped` is a shape test: any schema owning a `_links`/`_embedded` property counts as
 * already-enveloped and is skipped. That is deliberate — it protects the hand-written envelopes and
 * the `EntityModelRootModel`/`EntityModelDashboardModel` marker types. But a genuine payload that
 * declared such a property would be skipped just as quietly, and the only symptom would be a hal-forms
 * media type missing from the bundle much later.
 *
 * Run on the bundled document, *after* derivation, this is exact rather than heuristic: every HAL
 * response the deriver handled now carries a hal-forms entry, so a HAL response still pointing
 * `application/json` at an envelope-shaped schema is precisely one the deriver walked over. The
 * legitimate hand-written envelopes never reach here — `forEachHalResponse` skips any response that
 * already has a hal-forms entry, and the marker types are served through one.
 */
function envelopedPayloadErrors(document) {
    const errors = [];
    const schemas = document?.components?.schemas ?? {};

    forEachHalResponse(document, ({jsonSchema, where}) => {
        for (const name of halResponsePayloadNames(jsonSchema)) {
            if (!isEnvelopeShaped(schemas[name])) continue;
            errors.push({
                path: `/components/schemas/${name}`,
                message: `payload schema "${name}" declares _links or _embedded, so the deriver `
                    + `treats it as an already-written envelope and skips it — ${where} gets no `
                    + 'hal-forms media type. Remove the property, or write the envelope explicitly.',
            });
        }
    });

    return errors;
}

/**
 * @returns {Array<{path: string, message: string}>} empty when the document is valid
 */
export function validateSpec(document, {authorities}) {
    const operationIds = collectOperationIds(document);
    const errors = duplicateOperationIdErrors(operationIds);
    errors.push(...envelopedPayloadErrors(document));

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

            // x-klabis-authority (-> method-level @HasAuthority), x-klabis-owner-visible
            // (-> paired @OwnerVisible/@OwnerId, see below) and x-klabis-hal (-> the deriver's
            // opt-out) make sense directly on an operation. The others describe field-level access
            // on a schema property and have no meaning without a property to attach to.
            if (context === 'operation' && !OPERATION_LEVEL_EXTENSIONS.has(key)) {
                errors.push({
                    path: `${path}/${key}`,
                    message: `"${key}" is not valid on an operation — it is a schema-property extension`,
                });
                continue;
            }

            // The mirror image: the deriver reads x-klabis-hal off the operation node only, so
            // anywhere else it would be silently ignored rather than opting anything out.
            if (key === 'x-klabis-hal' && context !== 'operation') {
                errors.push({
                    path: `${path}/${key}`,
                    message: '"x-klabis-hal" is only valid on an operation',
                });
                continue;
            }

            // Opt-out only: HAL is the default for all but 6 operations, and `true` would read as
            // an opt-in that the deriver does not implement.
            if (key === 'x-klabis-hal' && value !== false) {
                errors.push({path: `${path}/${key}`, message: 'must be false when present'});
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

            // Rendered verbatim after a '@' at class level by pojo.mustache, so a value that is not
            // a fully-qualified annotation name becomes a compile error in generated code rather
            // than anything diagnosable here.
            if (key === 'x-klabis-class-constraint'
                && (typeof value !== 'string' || !/^[a-z]\w*(\.\w+)+$/.test(value))) {
                errors.push({
                    path: `${path}/${key}`,
                    message: 'must be a fully-qualified annotation class name, without the leading "@"',
                });
            }

            // Rendered by pojo.mustache into @Relation(collectionRelation=..., itemRelation=...) on
            // the schema — collectionRelation is mandatory (the annotation's own required member),
            // itemRelation optional.
            if (key === 'x-klabis-relation') {
                if (!isPlainObject(value) || typeof value.collectionRelation !== 'string' || value.collectionRelation === '') {
                    errors.push({
                        path: `${path}/${key}`,
                        message: 'must be an object with a non-empty string "collectionRelation"',
                    });
                } else if (value.itemRelation !== undefined && typeof value.itemRelation !== 'string') {
                    errors.push({path: `${path}/${key}/itemRelation`, message: 'must be a string when present'});
                }
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

            if (key === 'x-klabis-owner-visible' && value !== true) {
                errors.push({path: `${path}/${key}`, message: 'must be true when present'});
            }

            // @OwnerVisible without an @OwnerId parameter is the dangerous half of the pair:
            // checkOwnership() finds no owner to compare against and denies, so the endpoint
            // silently loses the owner-or-authority semantics it claims to have. The templates
            // emit each annotation from its own node and cannot see the other, so this is the
            // only thing keeping the two together.
            if (key === 'x-klabis-owner-visible' && context === 'operation' && value === true) {
                const params = Array.isArray(node.parameters) ? node.parameters : [];
                const ownerIdParams = params
                    .map((p) => resolveParameter(document, p))
                    .filter((p) => p?.['x-klabis-owner-id'] === true);

                if (ownerIdParams.length === 0) {
                    errors.push({
                        path: `${path}/${key}`,
                        message: 'requires exactly one parameter marked x-klabis-owner-id: true — '
                            + '@OwnerVisible without @OwnerId denies instead of resolving ownership',
                    });
                } else if (ownerIdParams.length > 1) {
                    // findAnnotatedParameterIndex returns the first match, so a second one would
                    // be silently ignored — and which parameter wins would depend on spec order.
                    errors.push({
                        path: `${path}/${key}`,
                        message: `${ownerIdParams.length} parameters are marked x-klabis-owner-id; `
                            + 'ownership resolves against exactly one',
                    });
                } else if (ownerIdParams[0].in !== 'path') {
                    // Only pathParams.mustache has a branch for x-klabis-owner-id. On a query or
                    // header parameter the key would be silently dropped, leaving @OwnerVisible
                    // with nothing to resolve against — the exact failure this check exists to
                    // prevent, just one step later. This also covers the page/size/sort case:
                    // x-spring-paginated folds those into a single Pageable argument, dropping the
                    // parameter the annotation was meant for, and all three are query parameters.
                    errors.push({
                        path: `${path}/${key}`,
                        message: `owner-id parameter "${ownerIdParams[0].name}" is in `
                            + `"${ownerIdParams[0].in}"; @OwnerId is only generated for path parameters`,
                    });
                }
            }
        }

        // Restricted to `type: array` with a `$ref` items schema (Decision 3). A singular
        // HAL-wrapped property would need the marker beside the `$ref`, where OpenAPI 3.0 ignores
        // siblings outright and 3.1 tooling honours them inconsistently — refused until a real
        // case appears. Pointing at an already-enveloped schema would double-wrap it.
        //
        // Coupled to `deriveEntityItems` in derive.mjs, which DELETES `x-hal-entity-items` from
        // every `type: array` node carrying it (see the comment there), and validation runs after
        // derivation — see the ordering comment above `validateSpec` in the entry-point bundle.mjs.
        // So a correctly-placed marker never reaches the checks below at all; what does reach them
        // is a marker on a non-array or with a non-`true` value, which is exactly the authoring
        // mistake they exist to catch. In particular the already-enveloped check cannot fire on the
        // deriver's own retargeted `items.$ref`. If that delete is ever removed, this check starts
        // flagging every migrated module.
        if (Object.hasOwn(node, 'x-hal-entity-items')) {
            const value = node['x-hal-entity-items'];
            if (value !== true) {
                errors.push({path: `${path}/x-hal-entity-items`, message: 'must be true when present'});
            } else if (node.type !== 'array') {
                errors.push({
                    path: `${path}/x-hal-entity-items`,
                    message: 'is only valid on a schema with "type: array"',
                });
            } else if (typeof node.items?.$ref !== 'string') {
                errors.push({
                    path: `${path}/x-hal-entity-items`,
                    message: 'requires the array\'s "items" to be a $ref to a payload schema',
                });
            } else {
                const match = node.items.$ref.match(/^#\/components\/schemas\/(.+)$/);
                const target = match ? document.components?.schemas?.[match[1]] : undefined;
                if (isEnvelopeShaped(target)) {
                    errors.push({
                        path: `${path}/x-hal-entity-items`,
                        message: `"${match[1]}" is already shaped as a HAL envelope — `
                            + 'the marker would wrap it a second time',
                    });
                }
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

/**
 * The module files, derived from the ones klabis.yaml routes a path to.
 *
 * Deliberately not a directory glob: that would treat any stray .yaml left in the spec directory as
 * a module and fail the build on its missing header. Deriving it from `paths` means a file nothing
 * routes to is simply not a module, and a newly routed one is picked up with no second list to
 * update. Refs into a subdirectory (`./_shared/hal.yaml`) are fragments, not modules.
 */
export function moduleFileNames(root) {
    return [...new Set(
        Object.values(root?.paths ?? {})
            .map((pathItem) => pathItem?.$ref)
            .filter((ref) => typeof ref === 'string')
            .map((ref) => ref.split('#')[0].replace(/^\.\//, ''))
            .filter((file) => !file.includes('/')),
    )].sort();
}

/**
 * Checks the invariants that hold between the spec FILES, which validateSpec cannot see.
 *
 * Every module file is a standalone OpenAPI document so it renders on its own in Swagger UI, which
 * means it repeats `openapi`, `info.version` and `components.securitySchemes` from klabis.yaml.
 * The bundler drops all three (modules are pulled in by a `#/paths/...` pointer, never whole-file),
 * so a module that drifted from the root would keep bundling cleanly and only mislead whoever
 * opened that one file. These checks are the only thing making the duplication safe.
 *
 * @param root      parsed klabis.yaml
 * @param modules   [{name, document}] parsed module files
 */
export function validateModuleDocuments(root, modules) {
    const errors = [];
    // Key order carries no meaning in a security scheme, so compare the sorted shape — otherwise
    // reordering `type`/`description`/`flows` in one file would report drift that isn't there.
    const canonical = (schemes) => JSON.stringify(sortKeysDeep(schemes ?? null));
    const rootScheme = canonical(root?.components?.securitySchemes);

    for (const {name, document} of modules) {
        if (document?.openapi !== root?.openapi) {
            errors.push({
                path: `${name}/openapi`,
                message: `"${document?.openapi}" does not match klabis.yaml's "${root?.openapi}"`,
            });
        }

        if (document?.info?.version !== root?.info?.version) {
            errors.push({
                path: `${name}/info/version`,
                message: `"${document?.info?.version}" does not match klabis.yaml's `
                    + `"${root?.info?.version}"`,
            });
        }

        if (canonical(document?.components?.securitySchemes) !== rootScheme) {
            errors.push({
                path: `${name}/components/securitySchemes`,
                message: 'does not match klabis.yaml — an operation\'s `security` would name a '
                    + 'scheme that differs from the one the bundle publishes',
            });
        }
    }

    return errors;
}
