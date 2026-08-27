#!/usr/bin/env node
/**
 * Generates frontend/src/api/halTypes.ts from the x-hal-* extensions in docs/openapi/spec/.
 *
 * Runs as the second half of `npm run openapi`, after openapi-typescript has produced
 * klabisApi.d.ts — the generated resource types intersect the schemas from that file.
 *
 * Emits a .ts, not a .d.ts: the `*Rels` constants are real runtime values, and a declaration file
 * would type-check yet blow up on import.
 *
 * Usage: node haltypes.mjs [--out <file>] [--check]
 */
import {existsSync, mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

import {bundleSpec} from './lib/bundle.mjs';
import {collectHalResources, renderHalTypes} from './lib/haltypes.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '../..');

const SPEC_ROOT = resolve(repoRoot, 'docs/openapi/spec/klabis.yaml');
const DEFAULT_OUT = resolve(repoRoot, 'frontend/src/api/halTypes.ts');

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

    const {document} = bundleSpec(SPEC_ROOT);
    const resources = collectHalResources(document);
    const rendered = renderHalTypes(document);

    if (args.check) {
        console.log(`HAL types: ${resources.length} resource(s). Not written (--check).`);
        return;
    }

    if (existsSync(args.out) && readFileSync(args.out, 'utf8') === rendered) {
        console.log(`Unchanged: ${args.out} (${resources.length} HAL resources)`);
        return;
    }

    mkdirSync(dirname(args.out), {recursive: true});
    writeFileSync(args.out, rendered);
    console.log(`Written: ${args.out} (${resources.length} HAL resources)`);
}

main();
