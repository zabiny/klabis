#!/usr/bin/env node
/**
 * Migration aid: compares the springdoc (code-first) output against the hand-written spec and
 * reports which operations are still unmigrated or migrated inaccurately.
 *
 * Never fails the build — an unmigrated operation is the expected state during the migration.
 * Delete this script together with the springdoc plugin once the migration completes.
 *
 * Usage: node drift.mjs [--module <prefix>]
 *   --module  restrict the report to paths starting with the prefix, e.g. /api/members
 */
import {existsSync, mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

import {bundleSpec} from './lib/bundle.mjs';
import {compareDocuments} from './lib/compare.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '../..');

const SPEC_ROOT = resolve(repoRoot, 'docs/openapi/spec/klabis.yaml');
const CODE_FIRST = resolve(repoRoot, 'docs/openapi/generated/klabis-codefirst.json');
const REPORT = resolve(repoRoot, 'docs/openapi/build/drift-report.json');

function parseArgs(argv) {
    const args = {module: undefined};
    for (let i = 0; i < argv.length; i++) {
        if (argv[i] === '--module') args.module = argv[++i];
    }
    return args;
}

const pathOf = (operationKey) => operationKey.slice(operationKey.indexOf(' ') + 1);

function filterByModule(result, prefix) {
    if (!prefix) return result;
    const keep = (key) => pathOf(key).startsWith(prefix);
    return {
        missingInSpec: result.missingInSpec.filter(keep),
        extraInSpec: result.extraInSpec.filter(keep),
        mismatched: result.mismatched.filter((m) => keep(m.operation)),
        matched: result.matched,
    };
}

function main() {
    const args = parseArgs(process.argv.slice(2));

    if (!existsSync(CODE_FIRST)) {
        console.error(`Code-first spec not found: ${CODE_FIRST}`);
        console.error('Run  ./gradlew :backend:generateOpenApiDocs  first.');
        process.exit(1);
    }

    const codeFirstDoc = JSON.parse(readFileSync(CODE_FIRST, 'utf8'));
    const {document: specDoc} = bundleSpec(SPEC_ROOT);

    const result = filterByModule(compareDocuments(codeFirstDoc, specDoc), args.module);
    const scope = args.module ? ` for ${args.module}` : '';

    console.log(`\nOpenAPI migration drift${scope}`);
    console.log('─'.repeat(60));
    console.log(`  migrated and matching : ${result.matched}`);
    console.log(`  not yet in spec       : ${result.missingInSpec.length}`);
    console.log(`  mismatched            : ${result.mismatched.length}`);
    console.log(`  in spec only          : ${result.extraInSpec.length}`);

    if (result.mismatched.length > 0) {
        console.log('\nMismatched operations (spec and implementation disagree):');
        for (const {operation, differences} of result.mismatched) {
            console.log(`\n  ${operation}`);
            for (const d of differences) {
                console.log(`    ${d.field}:`);
                console.log(`      code-first: ${JSON.stringify(d.codeFirst)}`);
                console.log(`      spec-first: ${JSON.stringify(d.specFirst)}`);
            }
        }
    }

    if (result.extraInSpec.length > 0) {
        console.log('\nIn spec but not implemented:');
        for (const op of result.extraInSpec) console.log(`  ${op}`);
    }

    if (result.missingInSpec.length > 0) {
        console.log(`\nNot yet migrated (${result.missingInSpec.length}):`);
        for (const op of result.missingInSpec) console.log(`  ${op}`);
    }

    mkdirSync(dirname(REPORT), {recursive: true});
    writeFileSync(REPORT, `${JSON.stringify(result, null, 2)}\n`);
    console.log(`\nReport: ${REPORT}\n`);
}

main();
