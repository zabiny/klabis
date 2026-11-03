import {type ReactNode} from 'react';
import {act, renderHook} from '@testing-library/react';
import {MemoryRouter, useLocation, useNavigate} from 'react-router-dom';
import {describe, expect, it} from 'vitest';
import {useUrlFilters} from './useUrlFilters';

const schema = {
    status: {type: 'string', default: 'ACTIVE'},
    q: {type: 'string'},
    incomplete: {type: 'boolean'},
    registeredByMe: {type: 'boolean', param: 'registeredBy', trueValue: 'me'},
    dateFrom: {type: 'string', param: 'occurredAtFrom'},
    eventTypeIds: {type: 'string[]', param: 'eventTypeId'},
} as const;

const setup = (initialUrl: string) => {
    const wrapper = ({children}: {children: ReactNode}) => (
        <MemoryRouter initialEntries={[initialUrl]}>{children}</MemoryRouter>
    );
    return renderHook(() => ({filters: useUrlFilters(schema), location: useLocation(), navigate: useNavigate()}), {wrapper});
};

describe('useUrlFilters', () => {
    describe('reading', () => {
        it('reads strings, booleans and string arrays, honouring param renames and trueValue', () => {
            const {result} = setup('/p?status=ALL&q=nov&incomplete=true&registeredBy=me&occurredAtFrom=2026-01-01&eventTypeId=1&eventTypeId=2');

            expect(result.current.filters.values).toEqual({
                status: 'ALL',
                q: 'nov',
                incomplete: true,
                registeredByMe: true,
                dateFrom: '2026-01-01',
                eventTypeIds: ['1', '2'],
            });
        });

        it('treats a boolean as false unless the param equals trueValue', () => {
            const {result} = setup('/p?registeredBy=true&incomplete=yes');

            expect(result.current.filters.values.registeredByMe).toBe(false);
            expect(result.current.filters.values.incomplete).toBe(false);
        });

        it('yields the default or empty values for missing params and writes nothing to the URL', () => {
            const {result} = setup('/p');

            expect(result.current.filters.values).toEqual({
                status: 'ACTIVE',
                q: '',
                incomplete: false,
                registeredByMe: false,
                dateFrom: '',
                eventTypeIds: [],
            });
            expect(result.current.location.search).toBe('');
        });

        it('keeps an explicitly empty param instead of falling back to the default', () => {
            const {result} = setup('/p?status=');

            expect(result.current.filters.values.status).toBe('');
        });

        it('returns defaults again after navigating to a bare URL', () => {
            const {result} = setup('/p?status=INACTIVE');
            expect(result.current.filters.values.status).toBe('INACTIVE');

            act(() => result.current.navigate('/p'));

            expect(result.current.filters.values.status).toBe('ACTIVE');
            expect(result.current.location.search).toBe('');
        });
    });

    describe('writing', () => {
        it('writes a key with a default even when empty', () => {
            const {result} = setup('/p');

            act(() => result.current.filters.setValues({status: ''}));

            expect(new URLSearchParams(result.current.location.search).get('status')).toBe('');
        });

        it('deletes an empty key without a default', () => {
            const {result} = setup('/p?q=nov&incomplete=true&eventTypeId=1&occurredAtFrom=2026-01-01');

            act(() => result.current.filters.setValues({q: '', incomplete: false, eventTypeIds: [], dateFrom: ''}));

            expect(result.current.location.search).toBe('');
        });

        it('writes booleans with trueValue and string arrays as repeated params', () => {
            const {result} = setup('/p');

            act(() => result.current.filters.setValues({registeredByMe: true, eventTypeIds: ['1', '2']}));

            const params = new URLSearchParams(result.current.location.search);
            expect(params.get('registeredBy')).toBe('me');
            expect(params.getAll('eventTypeId')).toEqual(['1', '2']);
        });

        it('replaces existing string array values', () => {
            const {result} = setup('/p?eventTypeId=1&eventTypeId=2');

            act(() => result.current.filters.setValues({eventTypeIds: ['3']}));

            expect(new URLSearchParams(result.current.location.search).getAll('eventTypeId')).toEqual(['3']);
        });

        it('keeps params outside the schema', () => {
            const {result} = setup('/p?tab=attempts&q=a');

            act(() => result.current.filters.setValues({q: 'abc'}));

            const params = new URLSearchParams(result.current.location.search);
            expect(params.get('tab')).toBe('attempts');
            expect(params.get('q')).toBe('abc');
        });
    });

    describe('identity', () => {
        it('keeps values identity when unrelated params change', () => {
            const {result} = setup('/p?q=a&tab=one');
            const before = result.current.filters.values;

            act(() => result.current.navigate('/p?q=a&tab=two'));

            expect(result.current.filters.values).toBe(before);
        });

        it('changes values identity when a schema param changes', () => {
            const {result} = setup('/p?q=a');
            const before = result.current.filters.values;

            act(() => result.current.filters.setValues({q: 'ab'}));

            expect(result.current.filters.values).not.toBe(before);
        });
    });
});
