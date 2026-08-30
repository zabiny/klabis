#!/usr/bin/env node
/**
 * Bundles docs/openapi/spec/ into a single OpenAPI document.
 *
 * Usage: node bundle.mjs [--out <file>] [--check]
 *   --out        destination (default: docs/openapi/klabis-full.json)
 *   --check      validate and bundle, but do not write
 *
 * This is the sole producer of docs/openapi/klabis-full.json, which the frontend's TypeScript types
 * and Swagger UI generate from. The backend's Java codegen reads docs/openapi/spec/<module>.yaml
 * directly instead, so the HAL envelopes derived here exist only for those consumers — see
 * openspec/changes/derive-hal-envelopes-in-bundler/design.md. Run it via `./gradlew openapiBundle`
 * from backend/, or directly. See docs/openapi/spec/README.md.
 */
import {existsSync, mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {parse} from 'yaml';

import {bundleSpec, countOperations} from './lib/bundle.mjs';
import {loadAuthorities, moduleFileNames, validateModuleDocuments, validateSpec} from './lib/validate.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '../..');

const SPEC_ROOT = resolve(repoRoot, 'docs/openapi/spec/klabis.yaml');
const AUTHORITY_JAVA = resolve(repoRoot, 'backend/src/main/java/com/klabis/common/users/Authority.java');
const DEFAULT_OUT = resolve(repoRoot, 'docs/openapi/klabis-full.json');

function parseArgs(argv) {
    const args = {out: DEFAULT_OUT, check: false};
    for (let i = 0; i < argv.length; i++) {
        if (argv[i] === '--out') args.out = resolve(repoRoot, argv[++i]);
        else if (argv[i] === '--check') args.check = true;
    }
    return args;
}

function main() {
    const args = parseArgs(process.argv.slice(2));

    if (!existsSync(SPEC_ROOT)) {
        console.error(`Spec root not found: ${SPEC_ROOT}`);
        process.exit(1);
    }

    // bundleSpec throws on a spec it cannot merge — an unresolvable cross-file $ref. Reported in
    // the same shape as validation errors rather than as a stack trace.
    let bundled;
    try {
        bundled = bundleSpec(SPEC_ROOT);
    } catch (e) {
        console.error(`Spec bundling failed: ${e.message}`);
        process.exit(1);
    }
    const {document, conflicts, collisions} = bundled;

    if (conflicts.length > 0) {
        console.error('Conflicting component definitions (same name, different shape):');
        for (const c of conflicts) console.error(`  components.${c.bucket}.${c.name}  (${c.source})`);
        process.exit(1);
    }

    if (collisions?.length > 0) {
        console.error('HAL envelope derivation failed:');
        for (const c of collisions) console.error(`  components.schemas.${c.name}: ${c.message}`);
        process.exit(1);
    }

    // Must stay downstream of bundleSpec's envelope derivation: the enveloped-payload rule reads a
    // missing hal-forms entry as "the deriver skipped this", which is only true post-derivation.
    // Validating first would flag every hand-written envelope instead.
    const authorities = loadAuthorities(AUTHORITY_JAVA);
    const errors = validateSpec(document, {authorities});

    const specDir = dirname(SPEC_ROOT);
    const rootDocument = parse(readFileSync(SPEC_ROOT, 'utf8'));
    const moduleFiles = moduleFileNames(rootDocument)
        .map((f) => ({name: f, document: parse(readFileSync(resolve(specDir, f), 'utf8'))}));

    errors.push(...validateModuleDocuments(rootDocument, moduleFiles));

    if (errors.length > 0) {
        console.error(`Spec validation failed (${errors.length} error(s)):`);
        for (const e of errors) console.error(`  ${e.path || '<root>'}: ${e.message}`);
        process.exit(1);
    }

    const operationCount = countOperations(document);

    if (args.check) {
        console.log(`Spec valid: ${operationCount} operation(s), ${Object.keys(document.components?.schemas ?? {}).length} schema(s). Not written (--check).`);
        return;
    }

    const serialized = `${JSON.stringify(document, null, 1)}\n`;

    // Only write on change — this file is committed and rewriting it churns 400+ KB of diff.
    if (existsSync(args.out) && readFileSync(args.out, 'utf8') === serialized) {
        console.log(`Unchanged: ${args.out} (${operationCount} operations)`);
        return;
    }

    mkdirSync(dirname(args.out), {recursive: true});
    writeFileSync(args.out, serialized);
    console.log(`Written: ${args.out} (${operationCount} operations)`);
}

main();
