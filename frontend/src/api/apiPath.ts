const API_PREFIX = '/api';
const URI_TEMPLATE = /\{[^}]*\}/g;
const ORIGIN = /^[a-z][a-z0-9+.-]*:\/\/[^/?#]*/i;

interface PathParts {
    path: string;
    query: string;
}

function split(href: string): PathParts {
    let rest = href.replace(URI_TEMPLATE, '').replace(ORIGIN, '');

    const hashIndex = rest.indexOf('#');
    if (hashIndex >= 0) {
        rest = rest.substring(0, hashIndex);
    }

    const queryIndex = rest.indexOf('?');
    const rawQuery = queryIndex >= 0 ? rest.substring(queryIndex) : '';
    let path = queryIndex >= 0 ? rest.substring(0, queryIndex) : rest;

    if (!path.startsWith('/')) {
        path = '/' + path;
    }

    // Exact match only: '/apiary' is a different resource, not '/api' + 'iary'.
    if (path === API_PREFIX || path.startsWith(API_PREFIX + '/')) {
        path = path.substring(API_PREFIX.length);
    }

    return {path, query: rawQuery === '?' ? '' : rawQuery};
}

export function toApiHref(href: string): string {
    const {path, query} = split(href);
    return API_PREFIX + (path === '/' ? '' : path) + query;
}

export function toRoutePath(href: string): string {
    const {path, query} = split(href);
    return (path || '/') + query;
}
