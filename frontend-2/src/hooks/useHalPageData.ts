import {useMemo} from 'react';
import {useHalRoute} from '../contexts/HalRouteContext';
import {useHalActions} from './useHalActions';
import {useIsAdmin} from './useIsAdmin';
import type {HalFormsTemplate, HalResourceLinks, HalResponse, Link, PageMetadata} from '../api';

/**
 * Return type for useHalPageData hook
 *
 * Combines data from useHalRoute, useHalActions, and useIsAdmin with helper methods
 * for common page patterns.
 *
 * @template T - The specific HalResponse type for resource-specific typing
 */
export interface UseHalPageDataReturn<T extends HalResponse = HalResponse> {
    // --- Top-level properties (most commonly used) ---

    /** The fetched HAL resource data */
    resourceData: T | null;

    /** Combined loading state (true if ANY underlying hook is loading) */
    isLoading: boolean;

    /** Error from fetching resource data */
    error: Error | null;

    /** Whether the current user is an admin */
    isAdmin: boolean;

    // --- Grouped properties (less common) ---

    /** Route-related properties */
    route: {
        /** Current pathname being displayed */
        pathname: string;

        /** Navigate to a resource (accepts HalResponse or Link) */
        navigateToResource: (resource: HalResponse | Link) => void;

        /** Manual refetch function for updating data after form submissions */
        refetch: () => Promise<void>;

        /** React Query query state */
        queryState: 'idle' | 'pending' | 'success' | 'error';

        /** Get a specific link from current resource */
        getResourceLink: (linkName?: string) => Link | null;
    };

    /** Action-related properties */
    actions: {
        /** Navigate to item by href string */
        handleNavigateToItem: (href: string) => void;
    };

    // --- Helper methods (common page patterns) ---

    /** Get all links from the resource */
    getLinks: () => Record<string, HalResourceLinks> | undefined;

    /** Get all templates from the resource */
    getTemplates: () => Record<string, HalFormsTemplate> | undefined;

    /** Check if resource has embedded data */
    hasEmbedded: () => boolean;

    /** Get embedded data as a flattened array */
    getEmbeddedItems: () => unknown[];

    /** Check if this is a collection resource */
    isCollection: () => boolean;

    /** Check if resource has a specific link */
    hasLink: (linkName: string) => boolean;

    /** Check if resource has a specific template */
    hasTemplate: (templateName: string) => boolean;

    /** Check if resource has any templates (forms) */
    hasForms: () => boolean;

    /** Get page metadata (if this is a collection) */
    getPageMetadata: () => PageMetadata | undefined;
}

/**
 * Custom hook that combines useHalRoute, useHalActions, and useIsAdmin
 *
 * Provides:
 * - Combined loading state from all three hooks
 * - Top-level access to commonly-used properties
 * - Grouped access to route and action properties
 * - Helper methods for common page patterns (checking for links, templates, collections, etc.)
 *
 * @template T - Specific HalResponse type for resource-specific typing
 *
 * @example
 * ```typescript
 * const {
 *   resourceData,
 *   isLoading,
 *   error,
 *   isAdmin,
 *   isCollection,
 *   getEmbeddedItems,
 *   actions,
 *   route
 * } = useHalPageData();
 *
 * if (isLoading) return <Spinner />;
 * if (error) return <Alert severity="error">{error.message}</Alert>;
 *
 * return (
 *   <div>
 *     {isCollection() && <Table items={getEmbeddedItems()} />}
 *     <HalLinksSection links={getLinks()} onNavigate={actions.handleNavigateToItem} />
 *     <HalFormsSection templates={getTemplates()} />
 *     {isAdmin && <AdminPanel />}
 *   </div>
 * );
 * ```
 */
export function useHalPageData<T extends HalResponse = HalResponse>(): UseHalPageDataReturn<T> {
    // Call the three underlying hooks
    const halRoute = useHalRoute();
    const halActions = useHalActions();
    const adminState = useIsAdmin();

    // Compute combined loading state - only from route, admin loading is separate
    const isLoading = halRoute.isLoading;

    // Memoize helper methods to prevent unnecessary re-renders
    const helpers = useMemo(() => {
        const resourceData = halRoute.resourceData as T | null;

        return {
            getLinks: (): Record<string, HalResourceLinks> | undefined => {
                return resourceData?._links;
            },

            getTemplates: (): Record<string, HalFormsTemplate> | undefined => {
                return resourceData?._templates;
            },

            hasEmbedded: (): boolean => {
                if (!resourceData?._embedded) return false;
                return Object.keys(resourceData._embedded).length > 0;
            },

            getEmbeddedItems: (): unknown[] => {
                if (!resourceData?._embedded) return [];
                return Object.values(resourceData._embedded).flat();
            },

            isCollection: (): boolean => {
                if (!resourceData) return false;
                const hasPage = (resourceData as any).page !== undefined;
                const hasEmbedded =
                    resourceData._embedded !== undefined &&
                    Object.keys(resourceData._embedded).length > 0;
                return hasPage || hasEmbedded;
            },

            hasLink: (linkName: string): boolean => {
                return !!resourceData?._links?.[linkName];
            },

            hasTemplate: (templateName: string): boolean => {
                return !!resourceData?._templates?.[templateName];
            },

            hasForms: (): boolean => {
                return (
                    !!resourceData?._templates &&
                    Object.keys(resourceData._templates).length > 0
                );
            },

            getPageMetadata: (): PageMetadata | undefined => {
                return (resourceData as any)?.page;
            },
        };
    }, [halRoute.resourceData]);

    // Return the organized interface
    return {
        // Top-level commonly used properties
        resourceData: halRoute.resourceData as T | null,
        isLoading,
        error: halRoute.error,
        isAdmin: adminState.isAdmin,

        // Grouped by concern
        route: {
            pathname: halRoute.pathname,
            navigateToResource: halRoute.navigateToResource,
            refetch: halRoute.refetch,
            queryState: halRoute.queryState,
            getResourceLink: halRoute.getResourceLink,
        },

        actions: {
            handleNavigateToItem: halActions.handleNavigateToItem,
        },

        // Spread helper methods
        ...helpers,
    };
}
