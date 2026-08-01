import {describe, expect, it} from 'vitest';
import {parse} from 'yaml';

import {
    bundleSpec,
    sortKeysDeep,
} from '../lib/bundle.mjs';

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

describe('x-klabis-authority on an operation', () => {
    // Emitting @HasAuthority is api.mustache's job, straight off this key — the same way
    // pojo.mustache reads it off a schema property. The bundler used to rewrite it into
    // x-operation-extra-annotation, which put a derived Java string into the published
    // contract next to the key it was derived from. This pins that it no longer does.
    it('is carried through untouched, not rewritten into an annotation', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /api/members/{id}/suspend:
    post:
      operationId: suspendMember
      x-klabis-authority: MEMBERS_MANAGE
      responses: {}
`,
        });

        const {document} = bundleSpec('/spec/klabis.yaml', {readYaml});
        const operation = document.paths['/api/members/{id}/suspend'].post;

        expect(operation['x-klabis-authority']).toBe('MEMBERS_MANAGE');
        expect(operation['x-operation-extra-annotation']).toBeUndefined();
    });
});

describe('x-klabis-owner-visible on an operation', () => {
    // Both halves of the pair are now emitted by the templates from their own spec key —
    // @OwnerVisible from the operation (api.mustache), @OwnerId from the parameter
    // (pathParams.mustache). The bundler used to synthesise both, which forced it to inline any
    // shared $ref parameter so @OwnerId reached one operation only. It no longer needs to:
    // @OwnerId is inert without @OwnerVisible, so it can sit on the shared parameter.
    // validate.mjs enforces that the pair stays together; see validate.test.mjs.
    it('is carried through untouched, and leaves a shared parameter $ref intact', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /api/members/{id}:
    get:
      operationId: getMember
      x-klabis-authority: MEMBERS_READ
      parameters:
        - $ref: '#/components/parameters/MemberIdParam'
      responses:
        '200':
          description: ok
    patch:
      operationId: updateMember
      x-klabis-authority: MEMBERS_MANAGE
      x-klabis-owner-visible: true
      parameters:
        - $ref: '#/components/parameters/MemberIdParam'
      responses:
        '204':
          description: ok
components:
  parameters:
    MemberIdParam:
      name: id
      in: path
      required: true
      x-klabis-owner-id: true
      schema:
        type: string
        format: uuid
  schemas: {}
`,
        });

        const {document} = bundleSpec('/spec/klabis.yaml', {readYaml});

        const patch = document.paths['/api/members/{id}'].patch;
        expect(patch['x-klabis-owner-visible']).toBe(true);
        expect(patch['x-klabis-authority']).toBe('MEMBERS_MANAGE');
        expect(patch['x-operation-extra-annotation']).toBeUndefined();

        // The shared parameter stays a $ref on both operations — nothing is inlined any more.
        expect(patch.parameters[0].$ref).toBe('#/components/parameters/MemberIdParam');
        const get = document.paths['/api/members/{id}'].get;
        expect(get.parameters[0].$ref).toBe('#/components/parameters/MemberIdParam');

        // getMember shares the owner-id parameter but never opted into ownership. That is
        // harmless: @OwnerId is only consulted for a method already marked @OwnerVisible.
        expect(get['x-klabis-owner-visible']).toBeUndefined();
        expect(document.components.parameters.MemberIdParam['x-klabis-owner-id']).toBe(true);
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
