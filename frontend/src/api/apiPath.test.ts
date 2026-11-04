import {toApiHref, toRoutePath} from './apiPath';

const cases: Array<[string, string, string, string]> = [
    ['absolute URL with /api', 'https://localhost:8443/api/members/1', '/api/members/1', '/members/1'],
    ['absolute URL without /api', 'https://example.com/members/123', '/api/members/123', '/members/123'],
    ['absolute URL with query', 'https://example.com/api/members/123?view=edit', '/api/members/123?view=edit', '/members/123?view=edit'],
    ['/api path', '/api/members/1', '/api/members/1', '/members/1'],
    ['origin-relative path', '/members/1', '/api/members/1', '/members/1'],
    ['bare path', 'members/1', '/api/members/1', '/members/1'],
    ['query kept', '/api/members/123?view=edit', '/api/members/123?view=edit', '/members/123?view=edit'],
    ['hash dropped', '/api/members/1#section', '/api/members/1', '/members/1'],
    ['hash dropped after query', '/members/1?x=1#section', '/api/members/1?x=1', '/members/1?x=1'],
    ['absolute URL with hash', 'https://h/api/members/1#top', '/api/members/1', '/members/1'],
    ['template in query position', '/api/members{?q,page}', '/api/members', '/members'],
    ['template in path position', '/api/members/{id}/events', '/api/members//events', '/members//events'],
    ['template in absolute URL', 'https://h/api/members{?q}', '/api/members', '/members'],
    ['template in query value', '/members?x={id}&y=1', '/api/members?x=&y=1', '/members?x=&y=1'],
    ['empty input', '', '/api', '/'],
    ['root', '/', '/api', '/'],
    ['bare /api', '/api', '/api', '/'],
    ['/api with trailing slash', '/api/', '/api', '/'],
    ['/api followed by query', '/api?x=1', '/api?x=1', '/?x=1'],
    ['/api followed by hash', '/api#h', '/api', '/'],
    ['/apiary is not the prefix', '/apiary', '/api/apiary', '/apiary'],
    ['/apiary/x is not the prefix', '/apiary/x?y=1', '/api/apiary/x?y=1', '/apiary/x?y=1'],
    ['empty query dropped', '/members?', '/api/members', '/members'],
    ['query with only a template dropped', '/members?{q}', '/api/members', '/members'],
    ['/api with empty query', '/api?', '/api', '/'],
    ['continuation template', '/members?x=1{&page}', '/api/members?x=1', '/members?x=1'],
    ['template-only input', '{?q}', '/api', '/'],
    ['bare apiary', 'apiary', '/api/apiary', '/apiary'],
];

describe('toApiHref', () => {
    it.each(cases)('%s', (_name, input, expectedApi) => {
        expect(toApiHref(input)).toBe(expectedApi);
    });
});

describe('toRoutePath', () => {
    it.each(cases)('%s', (_name, input, _expectedApi, expectedRoute) => {
        expect(toRoutePath(input)).toBe(expectedRoute);
    });
});

describe('idempotency', () => {
    it.each(cases)('%s', (_name, input) => {
        expect(toApiHref(toApiHref(input))).toBe(toApiHref(input));
        expect(toRoutePath(toApiHref(input))).toBe(toRoutePath(input));
    });
});
