import React from 'react';
import {act, renderHook} from '@testing-library/react';
import {useHalActions} from './useHalActions';
import * as ReactRouterDom from 'react-router-dom';
import {BrowserRouter} from 'react-router-dom';
import {type Mock, vi} from 'vitest';
import * as NavigationPathModule from '../utils/navigationPath';

// Mock dependencies
vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual('react-router-dom')),
    useNavigate: vi.fn(),
}));

vi.mock('../contexts/HalRouteContext', async () => ({
    ...(await vi.importActual('../contexts/HalRouteContext')),
    useHalRoute: vi.fn(),
}));

vi.mock('../api/hateoas', async () => ({
    ...(await vi.importActual('../api/hateoas')),
    submitHalFormsData: vi.fn(),
}));

vi.mock('../utils/navigationPath', async () => ({
    ...(await vi.importActual('../utils/navigationPath')),
    extractNavigationPath: vi.fn((href) => href),
}));

const useNavigate = vi.mocked(ReactRouterDom.useNavigate);
const extractNavigationPath = vi.mocked(NavigationPathModule.extractNavigationPath);

describe('useHalActions Hook', () => {
    let mockNavigate: Mock;

    beforeEach(() => {
        vi.clearAllMocks();

        mockNavigate = vi.fn();

        useNavigate.mockReturnValue(mockNavigate);
    });

    const createWrapper = () => {
        return ({children}: { children: React.ReactNode }) => (
            <BrowserRouter>
                {children}
            </BrowserRouter>
        );
    };

    describe('Initial State', () => {
        it('should return all required properties', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            expect(result.current).toHaveProperty('handleNavigateToItem');
        });
    });

    describe('Navigation', () => {
        it('should navigate to item href', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleNavigateToItem('/api/items/1');
            });

            expect(mockNavigate).toHaveBeenCalledWith('/api/items/1');
        });

        it('should extract navigation path before navigating', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleNavigateToItem('/api/items/123');
            });

            expect(extractNavigationPath).toHaveBeenCalledWith('/api/items/123');
            expect(mockNavigate).toHaveBeenCalled();
        });

        it('should handle complex URLs', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const complexUrl = '/api/items/1?filter=active&sort=name';

            act(() => {
                result.current.handleNavigateToItem(complexUrl);
            });

            expect(extractNavigationPath).toHaveBeenCalledWith(complexUrl);
        });
    });

});
