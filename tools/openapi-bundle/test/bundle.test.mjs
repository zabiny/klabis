import {describe, expect, it} from 'vitest';
import {parse} from 'yaml';

import {
    applyOperationAuthorityAnnotations,
    applyOperationOwnerVisibleAnnotations,
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

describe('applyOperationAuthorityAnnotations', () => {
    it('translates x-klabis-authority on an operation into x-operation-extra-annotation', () => {
        const document = {
            paths: {
                '/api/members/{id}/suspend': {
                    post: {
                        operationId: 'suspendMember',
                        'x-klabis-authority': 'MEMBERS_MANAGE',
                        responses: {},
                    },
                },
            },
        };

        applyOperationAuthorityAnnotations(document);

        expect(document.paths['/api/members/{id}/suspend'].post['x-operation-extra-annotation'])
            .toBe('@com.klabis.common.users.HasAuthority(com.klabis.common.users.Authority.MEMBERS_MANAGE)');
    });

    it('leaves operations without x-klabis-authority untouched', () => {
        const document = {
            paths: {
                '/api/members': {
                    get: {operationId: 'listMembers', responses: {}},
                },
            },
        };

        applyOperationAuthorityAnnotations(document);

        expect(document.paths['/api/members'].get['x-operation-extra-annotation']).toBeUndefined();
    });

    it('appends to an existing x-operation-extra-annotation rather than overwriting it', () => {
        const document = {
            paths: {
                '/api/members/{id}': {
                    patch: {
                        operationId: 'updateMember',
                        'x-klabis-authority': 'MEMBERS_MANAGE',
                        'x-operation-extra-annotation': '@com.klabis.common.security.fieldsecurity.OwnerVisible',
                        responses: {},
                    },
                },
            },
        };

        applyOperationAuthorityAnnotations(document);

        expect(document.paths['/api/members/{id}'].patch['x-operation-extra-annotation']).toBe(
            '@com.klabis.common.security.fieldsecurity.OwnerVisible\n' +
            '@com.klabis.common.users.HasAuthority(com.klabis.common.users.Authority.MEMBERS_MANAGE)',
        );
    });

    it('does not touch x-klabis-authority on a schema property', () => {
        const document = {
            paths: {},
            components: {
                schemas: {
                    Thing: {
                        properties: {
                            dateOfBirth: {type: 'string', 'x-klabis-authority': 'MEMBERS_MANAGE'},
                        },
                    },
                },
            },
        };

        applyOperationAuthorityAnnotations(document);

        expect(document.components.schemas.Thing.properties.dateOfBirth['x-operation-extra-annotation'])
            .toBeUndefined();
    });
});

describe('applyOperationOwnerVisibleAnnotations', () => {
    // x-klabis-owner-visible on an operation names the parameter holding the owner ID. The
    // bundler inlines that one parameter (breaking any shared $ref) and stamps
    // x-field-extra-annotation: @OwnerId onto it, while adding @OwnerVisible to the operation via
    // x-operation-extra-annotation — same mechanism x-klabis-authority already uses. This is what
    // makes the pair impossible to split: there is a single spec key, and if the named parameter
    // cannot be found the bundle throws instead of emitting half the pair.
    const OWNER_VISIBLE_FQN = '@com.klabis.common.security.fieldsecurity.OwnerVisible';
    const OWNER_ID_FQN = '@com.klabis.common.security.fieldsecurity.OwnerId';

    it('annotates the named parameter with @OwnerId and the operation with @OwnerVisible', () => {
        const document = {
            paths: {
                '/api/members/{id}': {
                    patch: {
                        operationId: 'updateMember',
                        'x-klabis-owner-visible': 'id',
                        parameters: [{name: 'id', in: 'path', required: true, schema: {type: 'string'}}],
                        responses: {},
                    },
                },
            },
        };

        applyOperationOwnerVisibleAnnotations(document);

        const operation = document.paths['/api/members/{id}'].patch;
        expect(operation['x-operation-extra-annotation']).toBe(OWNER_VISIBLE_FQN);
        expect(operation.parameters[0]['x-field-extra-annotation']).toBe(OWNER_ID_FQN);
    });

    it('leaves operations without x-klabis-owner-visible untouched', () => {
        const document = {
            paths: {
                '/api/members': {
                    get: {operationId: 'listMembers', responses: {}},
                },
            },
        };

        applyOperationOwnerVisibleAnnotations(document);

        expect(document.paths['/api/members'].get['x-operation-extra-annotation']).toBeUndefined();
    });

    it('appends to an existing x-operation-extra-annotation rather than overwriting it', () => {
        const document = {
            paths: {
                '/api/members/{id}': {
                    patch: {
                        operationId: 'updateMember',
                        'x-klabis-owner-visible': 'id',
                        'x-operation-extra-annotation':
                            '@com.klabis.common.users.HasAuthority(com.klabis.common.users.Authority.MEMBERS_MANAGE)',
                        parameters: [{name: 'id', in: 'path', required: true, schema: {type: 'string'}}],
                        responses: {},
                    },
                },
            },
        };

        applyOperationOwnerVisibleAnnotations(document);

        expect(document.paths['/api/members/{id}'].patch['x-operation-extra-annotation']).toBe(
            '@com.klabis.common.users.HasAuthority(com.klabis.common.users.Authority.MEMBERS_MANAGE)\n' +
            OWNER_VISIBLE_FQN,
        );
    });

    it('resolves a local $ref parameter, inlining only that operation\'s copy', () => {
        const memberIdParam = {name: 'id', in: 'path', required: true, schema: {type: 'string'}};
        const document = {
            paths: {
                '/api/members/{id}': {
                    get: {
                        operationId: 'getMember',
                        parameters: [{$ref: '#/components/parameters/MemberIdParam'}],
                        responses: {},
                    },
                    patch: {
                        operationId: 'updateMember',
                        'x-klabis-owner-visible': 'id',
                        parameters: [{$ref: '#/components/parameters/MemberIdParam'}],
                        responses: {},
                    },
                },
            },
            components: {parameters: {MemberIdParam: memberIdParam}},
        };

        applyOperationOwnerVisibleAnnotations(document);

        const patchParam = document.paths['/api/members/{id}'].patch.parameters[0];
        expect(patchParam.$ref).toBeUndefined();
        expect(patchParam.name).toBe('id');
        expect(patchParam['x-field-extra-annotation']).toBe(OWNER_ID_FQN);

        // The sibling operation still references the shared parameter unmodified — no leakage.
        const getParam = document.paths['/api/members/{id}'].get.parameters[0];
        expect(getParam.$ref).toBe('#/components/parameters/MemberIdParam');
        expect(document.components.parameters.MemberIdParam['x-field-extra-annotation']).toBeUndefined();
    });

    it('throws when the named parameter does not exist on the operation', () => {
        const document = {
            paths: {
                '/api/members/{id}': {
                    patch: {
                        operationId: 'updateMember',
                        'x-klabis-owner-visible': 'memberId',
                        parameters: [{name: 'id', in: 'path', required: true, schema: {type: 'string'}}],
                        responses: {},
                    },
                },
            },
        };

        expect(() => applyOperationOwnerVisibleAnnotations(document)).toThrow(/memberId/);
    });

    it('throws when the operation has x-klabis-owner-visible but no parameters', () => {
        const document = {
            paths: {
                '/api/members/{id}': {
                    patch: {
                        operationId: 'updateMember',
                        'x-klabis-owner-visible': 'id',
                        responses: {},
                    },
                },
            },
        };

        expect(() => applyOperationOwnerVisibleAnnotations(document)).toThrow(/updateMember/);
    });

    // The parameter is declared, so the generic "not declared on that operation" check passes —
    // but x-spring-paginated replaces page/size/sort with a Pageable argument, so @OwnerId would
    // land on a parameter the generator drops, leaving @OwnerVisible unpaired.
    it('throws when the named parameter is one x-spring-paginated folds into Pageable', () => {
        const document = {
            paths: {
                '/api/members': {
                    get: {
                        operationId: 'listMembers',
                        'x-spring-paginated': true,
                        'x-klabis-owner-visible': 'page',
                        parameters: [{name: 'page', in: 'query', schema: {type: 'integer'}}],
                        responses: {},
                    },
                },
            },
        };

        expect(() => applyOperationOwnerVisibleAnnotations(document)).toThrow(/Pageable/);
    });

    it('allows a parameter named page when the operation is not paginated', () => {
        const document = {
            paths: {
                '/api/pages/{page}': {
                    get: {
                        operationId: 'getPage',
                        'x-klabis-owner-visible': 'page',
                        parameters: [{name: 'page', in: 'path', schema: {type: 'string'}}],
                        responses: {},
                    },
                },
            },
        };

        applyOperationOwnerVisibleAnnotations(document);

        const operation = document.paths['/api/pages/{page}'].get;
        expect(operation.parameters[0]['x-field-extra-annotation']).toContain('OwnerId');
    });
});

describe('bundleSpec — x-klabis-authority on operations end to end', () => {
    it('rewrites x-klabis-authority into x-operation-extra-annotation on the bundled document', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /api/members/{id}/resume:
    post:
      operationId: resumeMember
      x-klabis-authority: MEMBERS_MANAGE
      responses:
        '204':
          description: ok
`,
        });

        const {document} = bundleSpec('/spec/klabis.yaml', {readYaml});

        expect(document.paths['/api/members/{id}/resume'].post['x-operation-extra-annotation'])
            .toBe('@com.klabis.common.users.HasAuthority(com.klabis.common.users.Authority.MEMBERS_MANAGE)');
    });
});

describe('bundleSpec — x-klabis-owner-visible on operations end to end', () => {
    it('rewrites x-klabis-owner-visible into paired @OwnerVisible/@OwnerId annotations, MANAGE-or-owner semantics preserved', () => {
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
      x-klabis-owner-visible: id
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
      schema:
        type: string
        format: uuid
  schemas: {}
`,
        });

        const {document} = bundleSpec('/spec/klabis.yaml', {readYaml});

        const patch = document.paths['/api/members/{id}'].patch;
        expect(patch['x-operation-extra-annotation']).toBe(
            '@com.klabis.common.users.HasAuthority(com.klabis.common.users.Authority.MEMBERS_MANAGE)\n' +
            '@com.klabis.common.security.fieldsecurity.OwnerVisible',
        );
        expect(patch.parameters[0]['x-field-extra-annotation'])
            .toBe('@com.klabis.common.security.fieldsecurity.OwnerId');

        // getMember shares the same $ref'd parameter but never opted into ownership — it must
        // stay a plain MANAGE-only check with no @OwnerId leaking onto its parameter.
        const get = document.paths['/api/members/{id}'].get;
        expect(get['x-operation-extra-annotation'])
            .toBe('@com.klabis.common.users.HasAuthority(com.klabis.common.users.Authority.MEMBERS_READ)');
        expect(get.parameters[0].$ref).toBe('#/components/parameters/MemberIdParam');
    });

    it('throws when x-klabis-owner-visible names a parameter absent from the operation', () => {
        const readYaml = fakeReader({
            'klabis.yaml': `
openapi: 3.1.0
paths:
  /api/members/{id}:
    patch:
      operationId: updateMember
      x-klabis-owner-visible: memberId
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '204':
          description: ok
`,
        });

        expect(() => bundleSpec('/spec/klabis.yaml', {readYaml})).toThrow(/memberId/);
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
