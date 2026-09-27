import type {ReactNode} from 'react';
import React from 'react';
import {renderHook} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {usePermissionsEditor} from './usePermissionsEditor';

vi.mock('./useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);
const mockUseAuthorizedMutation = vi.mocked(useAuthorizedMutation);

describe('usePermissionsEditor', () => {
    beforeEach(() => {
        mockUseAuthorizedMutation.mockReturnValue({
            mutate: vi.fn(),
            isPending: false,
            error: null,
        } as unknown as ReturnType<typeof useAuthorizedMutation>);
    });

    const createWrapper = () => {
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
        return ({children}: { children: ReactNode }) =>
            React.createElement(QueryClientProvider, {client: queryClient}, children);
    };

    it('exposes the assignable authorities from the updatePermissions template options', () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                authorities: ['MEMBERS:MANAGE'],
                _links: {self: {href: '/api/users/1/permissions'}},
                _templates: {
                    updatePermissions: {
                        method: 'PUT',
                        properties: [
                            {
                                name: 'authorities',
                                type: 'checkboxGroup',
                                options: {inline: ['MEMBERS:MANAGE', 'SYNC:MANAGE']},
                            },
                        ],
                    },
                },
            },
            isLoading: false,
        } as unknown as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => usePermissionsEditor('/api/users/1/permissions'), {
            wrapper: createWrapper(),
        });

        expect(result.current.assignableAuthorities).toEqual(['MEMBERS:MANAGE', 'SYNC:MANAGE']);
        expect(result.current.permissions).toEqual(['MEMBERS:MANAGE']);
    });

    it('returns an empty array when the template is not present yet (loading)', () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: undefined,
            isLoading: true,
        } as unknown as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => usePermissionsEditor('/api/users/1/permissions'), {
            wrapper: createWrapper(),
        });

        expect(result.current.assignableAuthorities).toEqual([]);
    });

    it('returns an empty array when the authorities property has no inline options', () => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                authorities: [],
                _templates: {updatePermissions: {method: 'PUT', properties: []}},
            },
            isLoading: false,
        } as unknown as ReturnType<typeof useAuthorizedQuery>);

        const {result} = renderHook(() => usePermissionsEditor('/api/users/1/permissions'), {
            wrapper: createWrapper(),
        });

        expect(result.current.assignableAuthorities).toEqual([]);
    });
});
