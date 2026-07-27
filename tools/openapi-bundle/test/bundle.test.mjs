import {describe, expect, it} from 'vitest';
import {parse} from 'yaml';

import {bundleSpec, sortKeysDeep} from '../lib/bundle.mjs';

/** Builds a readYaml stub backed by an in-memory {absolutePath: yamlSource} map. */
function fakeReader(files) {
    return (path) => {
        const key = Object.keys(files).find((f) => path.endsWith(f));
        if (key === undefined) throw new Error(`No stub for ${path}`);
        return parse(files[key]);
    };
}

describe('bundleSpec', () => {
    it('hoists components from a referenced file and rewrites the ref to a local one', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /api/members:
    get:
      operationId: listMembers
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: './_shared/hal.yaml#/components/schemas/Link'
components:
  schemas: {}
`,
            '_shared/hal.yaml': `
components:
  schemas:
    Link:
      type: object
      properties:
        href:
          type: string
`,
        });

        const {document, conflicts} = bundleSpec('/spec/klabis.yaml', {readYaml});

        expect(conflicts).toEqual([]);
        expect(document.components.schemas.Link.properties.href.type).toBe('string');
        expect(document.paths['/api/members'].get.responses['200']
            .content['application/json'].schema.$ref)
            .toBe('#/components/schemas/Link');
    });

    it('inlines a whole-file path fragment reference', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /api/members:
    $ref: './members.yaml#/paths/~1api~1members'
`,
            'members.yaml': `
paths:
  /api/members:
    get:
      operationId: listMembers
      responses:
        '200':
          description: ok
`,
        });

        const {document} = bundleSpec('/spec/klabis.yaml', {readYaml});
        expect(document.paths['/api/members'].get.operationId).toBe('listMembers');
    });

    it('resolves a cross-file ref nested inside a hoisted schema', () => {
        // MemberDetailsResponse is hoisted out of members.yaml, but itself points at a third file.
        // Leaving that ref alone would emit a bundle referencing files that are not there.
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /api/members/{id}:
    get:
      operationId: getMember
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: './members.yaml#/components/schemas/MemberDetailsResponse'
`,
            'members.yaml': `
components:
  schemas:
    MemberDetailsResponse:
      type: object
      properties:
        address:
          $ref: './_shared/address.yaml#/components/schemas/Address'
`,
            '_shared/address.yaml': `
components:
  schemas:
    Address:
      type: object
      properties:
        city:
          type: string
`,
        });

        const {document, conflicts} = bundleSpec('/spec/klabis.yaml', {readYaml});

        expect(conflicts).toEqual([]);
        expect(document.components.schemas.MemberDetailsResponse.properties.address.$ref)
            .toBe('#/components/schemas/Address');
        expect(document.components.schemas.Address.properties.city.type).toBe('string');
    });

    it('terminates on a self-referencing schema', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /tree:
    get:
      operationId: getTree
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: './tree.yaml#/components/schemas/Node'
`,
            'tree.yaml': `
components:
  schemas:
    Node:
      type: object
      properties:
        children:
          type: array
          items:
            $ref: '#/components/schemas/Node'
`,
        });

        const {document} = bundleSpec('/spec/klabis.yaml', {readYaml});
        expect(document.components.schemas.Node.properties.children.items.$ref)
            .toBe('#/components/schemas/Node');
    });

    it('leaves same-file refs untouched', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths: {}
components:
  schemas:
    A:
      $ref: '#/components/schemas/B'
    B:
      type: string
`,
        });

        const {document} = bundleSpec('/spec/klabis.yaml', {readYaml});
        expect(document.components.schemas.A.$ref).toBe('#/components/schemas/B');
    });

    it('reports a conflict when two files define the same schema differently', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /a:
    get:
      operationId: a
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: './one.yaml#/components/schemas/Shared'
  /b:
    get:
      operationId: b
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: './two.yaml#/components/schemas/Shared'
`,
            'one.yaml': `
components:
  schemas:
    Shared:
      type: string
`,
            'two.yaml': `
components:
  schemas:
    Shared:
      type: integer
`,
        });

        const {conflicts} = bundleSpec('/spec/klabis.yaml', {readYaml});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0].name).toBe('Shared');
    });

    it('does not report a conflict for identical definitions', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /a:
    get:
      operationId: a
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: './one.yaml#/components/schemas/Shared'
  /b:
    get:
      operationId: b
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: './two.yaml#/components/schemas/Shared'
`,
            'one.yaml': `
components:
  schemas:
    Shared:
      type: string
`,
            'two.yaml': `
components:
  schemas:
    Shared:
      type: string
`,
        });

        expect(bundleSpec('/spec/klabis.yaml', {readYaml}).conflicts).toEqual([]);
    });

    it('throws on an unresolvable cross-file ref', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /a:
    $ref: './members.yaml#/paths/~1nope'
`,
            'members.yaml': `
paths: {}
`,
        });

        expect(() => bundleSpec('/spec/klabis.yaml', {readYaml})).toThrow(/Unresolvable/);
    });
});

describe('sortKeysDeep', () => {
    it('produces a stable key order regardless of input order', () => {
        const a = sortKeysDeep({b: 1, a: {d: 2, c: 3}});
        const b = sortKeysDeep({a: {c: 3, d: 2}, b: 1});
        expect(JSON.stringify(a)).toBe(JSON.stringify(b));
    });

    it('does not reorder arrays', () => {
        expect(sortKeysDeep({x: [3, 1, 2]}).x).toEqual([3, 1, 2]);
    });
});
