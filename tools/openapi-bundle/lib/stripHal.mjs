const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);

const HAL_CONTENT_TYPES = ['application/prs.hal-forms+json', 'application/hal+json'];

/**
 * Strips HAL/HAL+FORMS response *schemas* from a bundled OpenAPI document, for backend codegen input.
 *
 * The generated `*Api` interface must declare the bare payload type (Page<X>/Collection<X>/X), never
 * the HAL envelope — see klabis-api-spec skill. Rather than reconstructing that from the envelope via
 * a large schemaMappings table, every HAL response that also declares an `application/json` sibling
 * (referencing the bare payload) has its HAL content type's *schema* removed here, leaving
 * application/json as the only content type carrying a schema.
 *
 * The HAL content-type key itself is kept (emptied, not deleted): api.mustache derives each method's
 * Spring `produces` clause from the full set of content-type keys on the response, and the frontend
 * sends `Accept: application/prs.hal-forms+json` on every request. Deleting the key outright drops
 * that media type from `produces` and the real endpoint starts answering 406 to its actual caller —
 * same failure mode the skill documents for a 204 with no HAL content block at all.
 *
 * x-hal-links/x-hal-templates are dropped since they describe the envelope, which backend codegen no
 * longer sees. A response with no application/json sibling is left completely untouched, so nothing
 * silently disappears from backend codegen input.
 */
export function stripHalForCodegen(document) {
    return mapResponses(document, (response) => {
        if (!isPlainObject(response) || !isPlainObject(response.content)) return response;
        if (!Object.hasOwn(response.content, 'application/json')) return response;

        const halTypesPresent = HAL_CONTENT_TYPES.filter((type) => Object.hasOwn(response.content, type));
        if (halTypesPresent.length === 0) return response;

        const content = {...response.content};
        for (const type of halTypesPresent) content[type] = {};

        const stripped = {...response, content};
        delete stripped['x-hal-links'];
        delete stripped['x-hal-templates'];
        return stripped;
    });
}

function mapResponses(document, transform) {
    const paths = document?.paths;
    if (!isPlainObject(paths)) return document;

    const newPaths = {};
    for (const [path, pathItem] of Object.entries(paths)) {
        if (!isPlainObject(pathItem)) {
            newPaths[path] = pathItem;
            continue;
        }
        const newPathItem = {};
        for (const [key, operation] of Object.entries(pathItem)) {
            if (!isPlainObject(operation) || !isPlainObject(operation.responses)) {
                newPathItem[key] = operation;
                continue;
            }
            const newResponses = {};
            for (const [status, response] of Object.entries(operation.responses)) {
                newResponses[status] = transform(response);
            }
            newPathItem[key] = {...operation, responses: newResponses};
        }
        newPaths[path] = newPathItem;
    }

    return {...document, paths: newPaths};
}
