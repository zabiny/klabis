import {readFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {parse} from 'yaml';

/**
 * Bundles a multi-file OpenAPI spec into a single document.
 *
 * Cross-file `$ref`s (`./members.yaml#/paths/...`, `./_shared/hal.yaml#/components/schemas/Link`)
 * are resolved by pulling the referenced file's components into the root document and rewriting
 * the ref to a local one. Same-file refs (`#/components/schemas/X`) are left untouched.
 */

const readYaml = (file) => parse(readFileSync(file, 'utf8'));

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

/** OpenAPI path-item keys that denote an operation. */
export const HTTP_METHODS = ['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace'];

/**
 * Fully-qualified name of the type-safe method-security annotation that x-klabis-authority
 * generates on an operation. See backend-patterns skill: "@HasAuthority Method/Class-Level
 * Authorization".
 */
const HAS_AUTHORITY_FQN = 'com.klabis.common.users.HasAuthority';
const AUTHORITY_FQN = 'com.klabis.common.users.Authority';

/**
 * Translates `x-klabis-authority` on an operation into `x-operation-extra-annotation`, which the
 * (stock) api.mustache template emits verbatim above the generated interface method. This is how
 * the spec drives method-level @HasAuthority instead of it being hand-written on the controller —
 * see klabis-api-spec skill and ADR for x-klabis-authority-on-operations.
 *
 * Only operations carry this rewrite; x-klabis-authority on a schema property (field-level
 * security) is untouched — that one is consumed directly by FieldSecurityBeanSerializerModifier,
 * not by codegen.
 */
export function applyOperationAuthorityAnnotations(document) {
    const paths = document.paths;
    if (!isPlainObject(paths)) return document;

    for (const pathItem of Object.values(paths)) {
        if (!isPlainObject(pathItem)) continue;
        for (const [method, operation] of Object.entries(pathItem)) {
            if (!HTTP_METHODS.includes(method) || !isPlainObject(operation)) continue;

            const authority = operation['x-klabis-authority'];
            if (typeof authority !== 'string') continue;

            const annotation = `@${HAS_AUTHORITY_FQN}(${AUTHORITY_FQN}.${authority})`;
            const existing = operation['x-operation-extra-annotation'];
            operation['x-operation-extra-annotation'] = existing
                ? `${existing}\n${annotation}`
                : annotation;
        }
    }

    return document;
}

const OWNER_VISIBLE_FQN = 'com.klabis.common.security.fieldsecurity.OwnerVisible';
const OWNER_ID_FQN = 'com.klabis.common.security.fieldsecurity.OwnerId';

/** Resolves a local `#/components/parameters/X` $ref; returns the object unchanged otherwise. */
function resolveParameterRef(document, param) {
    if (!isPlainObject(param) || typeof param.$ref !== 'string') return param;
    const match = param.$ref.match(/^#\/components\/parameters\/(.+)$/);
    if (!match) return param;
    const resolved = document.components?.parameters?.[match[1]];
    if (resolved === undefined) {
        throw new Error(`x-klabis-owner-visible: unresolvable parameter ref "${param.$ref}"`);
    }
    return resolved;
}

/**
 * Translates `x-klabis-owner-visible` on an operation into the paired
 * @OwnerVisible (operation) / @OwnerId (parameter) annotations.
 *
 * The value names the parameter that carries the owner ID. This is deliberately the *only* way
 * to declare either annotation: with one spec key driving both halves, it is structurally
 * impossible to emit @OwnerVisible without @OwnerId (or vice versa) — a name that does not match
 * any parameter on the operation is a hard bundle failure, not a silently incomplete pair. See
 * klabis-api-spec skill and HasAuthorityMethodInterceptor.checkOwnership(), which requires both
 * annotations to actually enforce ownership.
 *
 * The named parameter is inlined (its $ref, if any, is broken) so the @OwnerId annotation lands
 * only on this operation's copy — parameters are commonly shared via $ref across operations
 * (e.g. MemberIdParam used by getMember/updateMember/suspendMember/resumeMember), and only the
 * operation that opted in may carry @OwnerId.
 */
/** Query parameters x-spring-paginated replaces with a single Pageable argument. */
const PAGEABLE_PARAMS = ['page', 'size', 'sort'];

export function applyOperationOwnerVisibleAnnotations(document) {
    const paths = document.paths;
    if (!isPlainObject(paths)) return document;

    for (const pathItem of Object.values(paths)) {
        if (!isPlainObject(pathItem)) continue;
        for (const [method, operation] of Object.entries(pathItem)) {
            if (!HTTP_METHODS.includes(method) || !isPlainObject(operation)) continue;

            const ownerParamName = operation['x-klabis-owner-visible'];
            if (typeof ownerParamName !== 'string') continue;

            // x-spring-paginated folds these into a single Pageable argument, so a parameter named
            // here would carry @OwnerId on a parameter the generator then drops — leaving
            // @OwnerVisible with nothing to resolve ownership against. checkOwnership() denies in
            // that case rather than granting, but the annotation would still claim a protection
            // the method does not have.
            if (operation['x-spring-paginated'] === true && PAGEABLE_PARAMS.includes(ownerParamName)) {
                throw new Error(
                    `x-klabis-owner-visible on operation "${operation.operationId ?? '?'}" names ` +
                    `parameter "${ownerParamName}", which x-spring-paginated folds into Pageable — ` +
                    `@OwnerId cannot be placed on it`,
                );
            }

            const params = Array.isArray(operation.parameters) ? operation.parameters : [];
            const index = params.findIndex(
                (p) => resolveParameterRef(document, p)?.name === ownerParamName,
            );
            if (index === -1) {
                throw new Error(
                    `x-klabis-owner-visible on operation "${operation.operationId ?? '?'}" names ` +
                    `parameter "${ownerParamName}", which is not declared on that operation`,
                );
            }

            const resolvedParam = resolveParameterRef(document, params[index]);
            operation.parameters = [
                ...params.slice(0, index),
                {...resolvedParam, 'x-field-extra-annotation': `@${OWNER_ID_FQN}`},
                ...params.slice(index + 1),
            ];

            const annotation = `@${OWNER_VISIBLE_FQN}`;
            const existing = operation['x-operation-extra-annotation'];
            operation['x-operation-extra-annotation'] = existing
                ? `${existing}\n${annotation}`
                : annotation;
        }
    }

    return document;
}

/** Counts operations across all paths — used for reporting. */
export function countOperations(document) {
    return Object.values(document?.paths ?? {})
        .filter(isPlainObject)
        .flatMap((pathItem) => Object.keys(pathItem))
        .filter((key) => HTTP_METHODS.includes(key))
        .length;
}

/** Walks a `#/a/b/c` JSON pointer. Returns undefined when any segment is missing. */
function resolvePointer(doc, pointer) {
    const segments = pointer.replace(/^#\//, '').split('/');
    let node = doc;
    for (const raw of segments) {
        const segment = raw.replace(/~1/g, '/').replace(/~0/g, '~');
        if (!isPlainObject(node) && !Array.isArray(node)) return undefined;
        node = node[segment];
        if (node === undefined) return undefined;
    }
    return node;
}

/**
 * Merges `components.schemas` (and other component buckets) from a source file into the accumulator.
 *
 * Definitions are recursively processed too: a hoisted schema may itself reference another file
 * (e.g. MemberDetailsResponse -> ./_shared/address.yaml#/.../Address), and leaving that ref
 * unresolved would produce a bundle pointing at files that no longer exist alongside it.
 *
 * A name defined differently in two files is a genuine conflict — the caller decides what to do.
 */
function collectComponents(sourceDoc, into, sourceLabel, conflicts, resolveDefinition) {
    const components = sourceDoc?.components;
    if (!isPlainObject(components)) return;

    for (const [bucket, entries] of Object.entries(components)) {
        if (!isPlainObject(entries)) continue;
        into[bucket] ??= {};
        for (const [name, rawDefinition] of Object.entries(entries)) {
            // Claim the name before recursing, so a cyclic reference back to this schema
            // terminates instead of recursing forever.
            if (Object.hasOwn(into[bucket], name)) {
                const definition = resolveDefinition(rawDefinition);
                if (JSON.stringify(into[bucket][name]) !== JSON.stringify(definition)) {
                    conflicts.push({bucket, name, source: sourceLabel});
                }
                continue;
            }
            into[bucket][name] = rawDefinition;
            into[bucket][name] = resolveDefinition(rawDefinition);
        }
    }
}

/**
 * Recursively rewrites cross-file `$ref`s to local ones, loading referenced files on demand.
 */
function inlineRefs(node, baseDir, loadFile, components, conflicts) {
    if (Array.isArray(node)) {
        return node.map((item) => inlineRefs(item, baseDir, loadFile, components, conflicts));
    }
    if (!isPlainObject(node)) return node;

    if (typeof node.$ref === 'string' && !node.$ref.startsWith('#')) {
        const [filePart, pointerPart] = node.$ref.split('#');
        const targetPath = resolve(baseDir, filePart);
        const {doc, label} = loadFile(targetPath);
        const targetDir = dirname(targetPath);

        collectComponents(doc, components, label, conflicts,
            (definition) => inlineRefs(definition, targetDir, loadFile, components, conflicts));

        if (!pointerPart) {
            // Whole-file ref — inline its content (used for path fragments).
            return inlineRefs(doc, targetDir, loadFile, components, conflicts);
        }

        const pointer = `#${pointerPart}`;
        if (pointer.startsWith('#/components/')) {
            // Already hoisted by collectComponents; just make the ref local.
            return {...node, $ref: pointer};
        }

        const target = resolvePointer(doc, pointer);
        if (target === undefined) {
            throw new Error(`Unresolvable $ref "${node.$ref}" (from ${baseDir})`);
        }
        return inlineRefs(target, targetDir, loadFile, components, conflicts);
    }

    const result = {};
    for (const [key, value] of Object.entries(node)) {
        result[key] = inlineRefs(value, baseDir, loadFile, components, conflicts);
    }
    return result;
}

/** Recursively sorts object keys so the emitted JSON is stable across runs. */
export function sortKeysDeep(value) {
    if (Array.isArray(value)) return value.map(sortKeysDeep);
    if (!isPlainObject(value)) return value;
    return Object.fromEntries(
        Object.keys(value).sort().map((key) => [key, sortKeysDeep(value[key])]),
    );
}

/**
 * @param rootFile absolute path to spec/klabis.yaml
 * @param options.readYaml injectable reader, for tests
 * @returns {{document: object, conflicts: Array}}
 */
export function bundleSpec(rootFile, options = {}) {
    const read = options.readYaml ?? readYaml;
    const cache = new Map();
    const conflicts = [];

    const loadFile = (path) => {
        if (!cache.has(path)) {
            cache.set(path, {doc: read(path), label: path});
        }
        return cache.get(path);
    };

    const root = read(rootFile);
    const rootDir = dirname(rootFile);

    // Root components seed the accumulator so refs to them stay valid.
    const components = {};
    collectComponents(root, components, rootFile, conflicts,
        (definition) => inlineRefs(definition, rootDir, loadFile, components, conflicts));

    const bundled = inlineRefs(
        {...root, components: undefined},
        rootDir,
        loadFile,
        components,
        conflicts,
    );
    delete bundled.components;

    const document = {...bundled};
    if (Object.keys(components).length > 0) {
        document.components = components;
    }

    applyOperationAuthorityAnnotations(document);
    applyOperationOwnerVisibleAnnotations(document);

    return {document: sortKeysDeep(document), conflicts};
}
