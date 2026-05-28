import {renderHook, act} from '@testing-library/react';
import {vi, describe, it, expect, beforeEach} from 'vitest';
import {useOrisEventImport} from './useOrisEventImport';
import type {HalFormsTemplate} from '../api/types';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: undefined, isError: false, isSuccess: false}),
    useAuthorizedMutation: vi.fn().mockReturnValue({mutate: vi.fn()}),
}));

vi.mock('./useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn().mockReturnValue({invalidateAllCaches: vi.fn()}),
}));

import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';

const makeTemplate = (overrides: Partial<HalFormsTemplate> = {}): HalFormsTemplate => ({
    method: 'POST',
    target: '/api/events/import-batch',
    properties: [
        {name: 'orisIds', type: 'number', multi: true, max: 10},
    ],
    ...overrides,
});

describe('useOrisEventImport', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(useAuthorizedQuery).mockReturnValue({data: undefined, isError: false, isSuccess: false} as any);
        vi.mocked(useAuthorizedMutation).mockReturnValue({mutate: vi.fn()} as any);
    });

    describe('template undefined — guard disabled', () => {
        it('does not throw when template is undefined', () => {
            expect(() => {
                renderHook(() => useOrisEventImport(undefined, true));
            }).not.toThrow();
        });

        it('disables query when template is undefined', () => {
            renderHook(() => useOrisEventImport(undefined, true));
            expect(useAuthorizedQuery).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({enabled: false}),
            );
        });

        it('disables query when isOpen is false', () => {
            renderHook(() => useOrisEventImport(makeTemplate(), false));
            expect(useAuthorizedQuery).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({enabled: false}),
            );
        });
    });

    describe('submit URL and method derived from affordance', () => {
        it('passes template.target as mutation URL', () => {
            const mutateMock = vi.fn();
            vi.mocked(useAuthorizedMutation).mockReturnValue({mutate: mutateMock} as any);

            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: [
                    {id: 1, name: 'Závod A', date: '2025-06-01', organizer: 'ZBM', location: null},
                    {id: 2, name: 'Závod B', date: '2025-07-01', organizer: 'HBT', location: null},
                ],
                isError: false,
                isSuccess: true,
            } as any);

            const template = makeTemplate({target: '/api/events/import-batch', method: 'POST'});
            const {result} = renderHook(() => useOrisEventImport(template, true));

            act(() => result.current.onToggleId(1));
            act(() => result.current.onImportBatch());

            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({url: '/api/events/import-batch'}),
                expect.anything(),
            );
        });

        it('uses method from affordance (non-POST)', () => {
            vi.mocked(useAuthorizedMutation).mockReturnValue({mutate: vi.fn()} as any);
            const template = makeTemplate({method: 'PUT'});

            renderHook(() => useOrisEventImport(template, true));

            expect(useAuthorizedMutation).toHaveBeenCalledWith(
                expect.objectContaining({method: 'PUT'}),
            );
        });

        it('falls back to POST when method is absent in affordance', () => {
            vi.mocked(useAuthorizedMutation).mockReturnValue({mutate: vi.fn()} as any);
            const template = makeTemplate({method: undefined});

            renderHook(() => useOrisEventImport(template, true));

            expect(useAuthorizedMutation).toHaveBeenCalledWith(
                expect.objectContaining({method: 'POST'}),
            );
        });

        it('uses property name as body key when orisIds property found', () => {
            const mutateMock = vi.fn();
            vi.mocked(useAuthorizedMutation).mockReturnValue({mutate: mutateMock} as any);
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: [{id: 7, name: 'Z', date: '2025-01-01', organizer: 'A', location: null}],
                isError: false,
                isSuccess: true,
            } as any);

            const template = makeTemplate({
                properties: [{name: 'eventOrisIds', type: 'number', multi: true, max: 10}],
            });
            const {result} = renderHook(() => useOrisEventImport(template, true));

            act(() => result.current.onToggleId(7));
            act(() => result.current.onImportBatch());

            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({data: {eventOrisIds: [7]}}),
                expect.anything(),
            );
        });

        it('falls back to orisIds key when no multi property found', () => {
            const mutateMock = vi.fn();
            vi.mocked(useAuthorizedMutation).mockReturnValue({mutate: mutateMock} as any);
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: [{id: 5, name: 'Z', date: '2025-01-01', organizer: 'A', location: null}],
                isError: false,
                isSuccess: true,
            } as any);

            const template = makeTemplate({properties: []});
            const {result} = renderHook(() => useOrisEventImport(template, true));

            act(() => result.current.onToggleId(5));
            act(() => result.current.onImportBatch());

            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({data: {orisIds: [5]}}),
                expect.anything(),
            );
        });
    });

    describe('limit derived from affordance', () => {
        it('reads limit from orisIds.max property', () => {
            const template = makeTemplate({
                properties: [{name: 'orisIds', type: 'number', multi: true, max: 5}],
            });
            const {result} = renderHook(() => useOrisEventImport(template, true));
            expect(result.current.selectionLimit).toBe(5);
        });

        it('falls back to 50 when max is absent', () => {
            const template = makeTemplate({
                properties: [{name: 'orisIds', type: 'number', multi: true}],
            });
            const {result} = renderHook(() => useOrisEventImport(template, true));
            expect(result.current.selectionLimit).toBe(50);
        });

        it('falls back to 50 when template is undefined', () => {
            const {result} = renderHook(() => useOrisEventImport(undefined, true));
            expect(result.current.selectionLimit).toBe(50);
        });

        it('falls back to 50 when properties array is empty', () => {
            const template = makeTemplate({properties: []});
            const {result} = renderHook(() => useOrisEventImport(template, true));
            expect(result.current.selectionLimit).toBe(50);
        });
    });

    describe('isSelectionLimitReached', () => {
        const templateWith3Limit = makeTemplate({
            properties: [{name: 'orisIds', type: 'number', multi: true, max: 3}],
        });

        const events = [
            {id: 1, name: 'A', date: '2025-01-01', organizer: 'O', location: null},
            {id: 2, name: 'B', date: '2025-01-02', organizer: 'O', location: null},
            {id: 3, name: 'C', date: '2025-01-03', organizer: 'O', location: null},
        ];

        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({data: events, isError: false, isSuccess: true} as any);
        });

        it('is false when selection is below limit', () => {
            const {result} = renderHook(() => useOrisEventImport(templateWith3Limit, true));
            act(() => result.current.onToggleId(1));
            act(() => result.current.onToggleId(2));
            expect(result.current.isSelectionLimitReached).toBe(false);
        });

        it('is true when selection equals limit', () => {
            const {result} = renderHook(() => useOrisEventImport(templateWith3Limit, true));
            act(() => result.current.onToggleId(1));
            act(() => result.current.onToggleId(2));
            act(() => result.current.onToggleId(3));
            expect(result.current.isSelectionLimitReached).toBe(true);
        });
    });

    describe('onToggleId limit enforcement', () => {
        const template = makeTemplate({
            properties: [{name: 'orisIds', type: 'number', multi: true, max: 2}],
        });

        const events = [
            {id: 1, name: 'A', date: '2025-01-01', organizer: 'O', location: null},
            {id: 2, name: 'B', date: '2025-01-02', organizer: 'O', location: null},
            {id: 3, name: 'C', date: '2025-01-03', organizer: 'O', location: null},
        ];

        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({data: events, isError: false, isSuccess: true} as any);
        });

        it('does not add id when limit is reached', () => {
            const {result} = renderHook(() => useOrisEventImport(template, true));
            act(() => result.current.onToggleId(1));
            act(() => result.current.onToggleId(2));
            act(() => result.current.onToggleId(3));
            expect(result.current.selectedIds.has(3)).toBe(false);
            expect(result.current.selectedIds.size).toBe(2);
        });

        it('still allows removal when limit is reached', () => {
            const {result} = renderHook(() => useOrisEventImport(template, true));
            act(() => result.current.onToggleId(1));
            act(() => result.current.onToggleId(2));
            act(() => result.current.onToggleId(1));
            expect(result.current.selectedIds.has(1)).toBe(false);
            expect(result.current.selectedIds.size).toBe(1);
        });
    });

    describe('onToggleAll with limit', () => {
        const template = makeTemplate({
            properties: [{name: 'orisIds', type: 'number', multi: true, max: 2}],
        });

        const events = [
            {id: 10, name: 'A', date: '2025-01-01', organizer: 'O', location: null},
            {id: 20, name: 'B', date: '2025-01-02', organizer: 'O', location: null},
            {id: 30, name: 'C', date: '2025-01-03', organizer: 'O', location: null},
        ];

        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({data: events, isError: false, isSuccess: true} as any);
        });

        it('selects at most `limit` events when toggling all from empty selection', () => {
            const {result} = renderHook(() => useOrisEventImport(template, true));
            act(() => result.current.onToggleAll());
            expect(result.current.selectedIds.size).toBe(2);
        });

        it('deselects all when selection already equals limit', () => {
            const {result} = renderHook(() => useOrisEventImport(template, true));
            act(() => result.current.onToggleId(10));
            act(() => result.current.onToggleId(20));
            act(() => result.current.onToggleAll());
            expect(result.current.selectedIds.size).toBe(0);
        });

        it('deselects all when all events are selected (not exceeding limit)', () => {
            const template2 = makeTemplate({
                properties: [{name: 'orisIds', type: 'number', multi: true, max: 10}],
            });
            const {result} = renderHook(() => useOrisEventImport(template2, true));
            act(() => result.current.onToggleAll());
            expect(result.current.selectedIds.size).toBe(3);
            act(() => result.current.onToggleAll());
            expect(result.current.selectedIds.size).toBe(0);
        });
    });
});
