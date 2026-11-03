import {useCallback, useMemo, useRef} from 'react';
import {useSearchParams} from 'react-router-dom';

type StringFilter = {type: 'string'; default?: string; param?: string};
type BooleanFilter = {type: 'boolean'; param?: string; trueValue?: string};
type StringArrayFilter = {type: 'string[]'; param?: string};

export type UrlFilterDefinition = StringFilter | BooleanFilter | StringArrayFilter;
export type UrlFilterSchema = Record<string, UrlFilterDefinition>;

type ValueOf<D extends UrlFilterDefinition> =
    D extends {type: 'boolean'} ? boolean : D extends {type: 'string[]'} ? string[] : string;

export type UrlFilterValues<S extends UrlFilterSchema> = {-readonly [K in keyof S]: ValueOf<S[K]>};

const paramOf = (key: string, def: UrlFilterDefinition) => def.param ?? key;

function readValue(params: URLSearchParams, key: string, def: UrlFilterDefinition): unknown {
    const param = paramOf(key, def);
    switch (def.type) {
        case 'string':
            return params.get(param) ?? def.default ?? '';
        case 'boolean':
            return params.get(param) === (def.trueValue ?? 'true');
        case 'string[]':
            return params.getAll(param);
    }
}

function writeValue(params: URLSearchParams, key: string, def: UrlFilterDefinition, value: unknown) {
    const param = paramOf(key, def);
    switch (def.type) {
        case 'string': {
            const text = value as string;
            if (def.default !== undefined || text !== '') params.set(param, text);
            else params.delete(param);
            return;
        }
        case 'boolean':
            if (value) params.set(param, def.trueValue ?? 'true');
            else params.delete(param);
            return;
        case 'string[]':
            params.delete(param);
            (value as string[]).forEach((item) => params.append(param, item));
            return;
    }
}

export function useUrlFilters<const S extends UrlFilterSchema>(schema: S) {
    const [searchParams, setSearchParams] = useSearchParams();
    const schemaRef = useRef(schema);
    schemaRef.current = schema;

    const signature = JSON.stringify(
        Object.entries(schema).map(([key, def]) => [key, readValue(searchParams, key, def)]),
    );

    const values = useMemo(
        () => Object.fromEntries(JSON.parse(signature) as [string, unknown][]) as UrlFilterValues<S>,
        [signature],
    );

    const setValues = useCallback((next: Partial<UrlFilterValues<S>>) => {
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);
            Object.entries(next).forEach(([key, value]) => {
                const def = schemaRef.current[key];
                if (def) writeValue(params, key, def, value);
            });
            return params;
        });
    }, [setSearchParams]);

    return {values, setValues};
}
