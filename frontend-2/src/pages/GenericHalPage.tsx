import {type ReactElement, useState} from 'react';
import {useHalRoute} from '../contexts/HalRouteContext';
import type {HalCollectionResponse, HalResponse} from '../api';
import {Alert, Modal, Spinner} from '../components/UI';
import {JsonPreview} from '../components/JsonPreview';
import {HalLinksSection} from '../components/HalNavigator2/HalLinksSection.tsx';
import {HalFormsSection} from '../components/HalNavigator2/HalFormsSection.tsx';
import {useHalActions} from '../hooks/useHalActions';
import {useIsAdmin} from '../hooks/useIsAdmin';
import {TABLE_HEADERS, UI_MESSAGES} from '../constants/messages';
import {isHalResponse} from "../components/HalNavigator2/halforms/utils.ts";
import NotFoundPage from "./NotFoundPage.tsx";

/**
 * Generic page for displaying HAL resources
 * Automatically detects whether the resource is a collection or item
 * and renders the appropriate display component
 *
 * Supports inline form display via query parameter (e.g., /resource?form=templateName)
 * Inline form display is automatically handled by HalFormsPageLayout wrapper
 * Used as a fallback for routes that don't have specialized pages
 */
export const GenericHalPage = (): ReactElement => {
    const {resourceData, isLoading, error, pathname} = useHalRoute();

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-12">
                <Spinner/>
            </div>
        );
    }

    if (error) {
        if (error.message.includes("HTTP 404")) {
            return <NotFoundPage/>
        }
        return (
            <Alert severity="error">
                <div className="space-y-2">
                    <p>Nepodařilo se načíst data z {pathname}</p>
                    <p className="text-sm text-gray-600">{error.message}</p>
                </div>
            </Alert>
        );
    }

    if (!resourceData) {
        return (
            <Alert severity="warning">
                <p>Žádná data dostupná</p>
            </Alert>
        );
    }

    // Determine if the resource is a collection or a single item
    const isCollection = isHalCollection(resourceData);

    return (
        <div className="p-4">
            {isCollection ? (
                <GenericCollectionDisplay data={resourceData as HalCollectionResponse}/>
            ) : (
                <GenericItemDisplay data={resourceData}/>
            )}
        </div>
    );
};

/**
 * Check if a HAL response is a collection
 */
function isHalCollection(data: unknown): data is HalCollectionResponse {
    return isHalResponse(data) && ((data?.page !== undefined) || (data?._embedded !== undefined && !isEmptyObject(data._embedded)));
}

/**
 * Check if an object is empty
 */
function isEmptyObject(obj: unknown): boolean {
    return typeof obj === 'object' && Object.keys(obj || {}).length === 0;
}

/**
 * Strip HAL/HAL+JSON meta attributes from an object
 */
function stripHalMetadata(obj: HalResponse): Record<string, any> {
    const {_links, _templates, _embedded, ...cleaned} = obj;
    return cleaned;
}

/**
 * Extract attribute names from collection items (excluding HAL metadata)
 */
function getCollectionAttributes(items: any[], maxAttributes: number = 6): string[] {
    const attributes = new Set<string>();

    items.forEach(item => {
        Object.keys(item).forEach(key => {
            if (!key.startsWith('_')) {
                attributes.add(key);
            }
        });
    });

    return Array.from(attributes).slice(0, maxAttributes);
}

/**
 * Format attribute value for display in table cell
 */
function formatAttributeValue(value: unknown, maxLength: number = 50): string {
    if (typeof value === 'object') {
        const json = JSON.stringify(value);
        return json.length > maxLength ? json.substring(0, maxLength) + '...' : json;
    }
    const str = String(value);
    return str.length > maxLength ? str.substring(0, maxLength) + '...' : str;
}

/**
 * Display a collection of items in a table format
 */
interface GenericCollectionDisplayProps {
    data: HalCollectionResponse;
}

const GenericCollectionDisplay = ({data}: GenericCollectionDisplayProps): ReactElement => {
    const [selectedItemForJsonView, setSelectedItemForJsonView] = useState<Record<string, unknown> | null>(null);
    const {handleNavigateToItem} = useHalActions();
    const {isAdmin} = useIsAdmin();
    const items = Object.values(data._embedded || {}).flat();
    const attributes = items.length > 0 ? getCollectionAttributes(items) : [];

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold">{TABLE_HEADERS.ITEMS}</h2>
                {data.page && (
                    <div className="text-sm text-gray-600">
                        Celkem: {data.page.totalElements} položek
                        ({(data.page.number ?? 0) + 1} z {data.page.totalPages} stran)
                    </div>
                )}
            </div>

            {/* Simple table display or empty state */}
            {items && items.length > 0 ? (
                <div className="overflow-x-auto border rounded-lg">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-100 dark:bg-gray-800">
                        <tr>
                            {attributes.map(attr => (
                                <th key={attr} className="px-4 py-2 text-left font-semibold">
                                    {attr}
                                </th>
                            ))}
                            <th className="px-4 py-2 text-left font-semibold w-fit">{TABLE_HEADERS.ACTIONS}</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y">
                        {items.map((item: any, index: number) => (
                            <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                                {attributes.map(attr => (
                                    <td key={attr} className="px-4 py-2">
                                        <code className="text-xs text-gray-600 dark:text-gray-400">
                                            {formatAttributeValue(item[attr])}
                                        </code>
                                    </td>
                                ))}
                                <td className="px-4 py-2 w-fit">
                                    <button
                                        onClick={() => setSelectedItemForJsonView(item)}
                                        className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 transition-colors mr-3"
                                        title="View full JSON"
                                        aria-label="View full JSON"
                                    >
                                        <svg
                                            className="w-4 h-4 inline"
                                            fill="none"
                                            stroke="currentColor"
                                            viewBox="0 0 24 24"
                                        >
                                            <path
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                strokeWidth={2}
                                                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                                            />
                                            <path
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                strokeWidth={2}
                                                d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                                            />
                                        </svg>
                                    </button>
                                    {item._links?.self?.href && (
                                        <button
                                            onClick={() => handleNavigateToItem(item._links.self.href)}
                                            className="text-blue-600 hover:underline dark:text-blue-400 bg-none border-none cursor-pointer"
                                        >
                                            Zobrazit
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            ) : (
                <Alert severity="info">
                    <p>{UI_MESSAGES.COLLECTION_EMPTY}</p>
                </Alert>
            )}

            <HalLinksSection
                links={data._links}
                onNavigate={handleNavigateToItem}
            />

            <HalFormsSection templates={data._templates}/>

            {/* Full JSON preview for advanced users - admin only */}
            {isAdmin && (
                <details className="mt-4 p-2 border rounded bg-gray-50 dark:bg-gray-900">
                    <summary className="cursor-pointer font-semibold">{UI_MESSAGES.SHOW_RAW_JSON}</summary>
                    <JsonPreview data={data} label={`Kompletní odpověď (${items.length} položek)`}/>
                </details>
            )}

            {/* Modal for viewing full item JSON */}
            <Modal
                isOpen={selectedItemForJsonView !== null}
                onClose={() => setSelectedItemForJsonView(null)}
                title={UI_MESSAGES.COMPLETE_JSON}
                size="xl"
            >
                {selectedItemForJsonView && (
                    <div className="max-h-96 overflow-y-auto">
                        <JsonPreview
                            data={stripHalMetadata(selectedItemForJsonView)}
                            label=""
                        />
                    </div>
                )}
            </Modal>
        </div>
    );
};

/**
 * Display a single item
 */
interface GenericItemDisplayProps {
    data: HalResponse;
}

const GenericItemDisplay = ({data}: GenericItemDisplayProps): ReactElement => {
    const {handleNavigateToItem} = useHalActions();
    const {isAdmin} = useIsAdmin();

    return (
        <div className="space-y-4">
            <h2 className="text-2xl font-bold">{TABLE_HEADERS.DETAILS}</h2>

            {/* Properties table */}
            <div className="overflow-x-auto border rounded-lg">
                <table className="w-full text-sm">
                    <thead className="bg-gray-100 dark:bg-gray-800">
                    <tr>
                        <th className="px-4 py-2 text-left font-semibold w-1/4">{TABLE_HEADERS.ATTRIBUTE}</th>
                        <th className="px-4 py-2 text-left font-semibold">{TABLE_HEADERS.VALUE}</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y">
                    {Object.entries(data)
                        .filter(([key]) => !key.startsWith('_'))
                        .map(([key, value]: [string, unknown]) => (
                            <tr key={key} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                                <td className="px-4 py-2 font-mono text-sm text-gray-600 dark:text-gray-400">{key}</td>
                                <td className="px-4 py-2">
                                    {typeof value === 'object' ? (
                                        <code className="text-xs text-gray-600 dark:text-gray-400">
                                            {JSON.stringify(value, null, 2).substring(0, 200)}
                                        </code>
                                    ) : (
                                        <span>{String(value)}</span>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <HalLinksSection
                links={data._links}
                onNavigate={handleNavigateToItem}
            />

            <HalFormsSection templates={data._templates}/>

            {/* Full JSON preview - admin only */}
            {isAdmin && (
                <details className="mt-4 p-2 border rounded bg-gray-50 dark:bg-gray-900">
                    <summary className="cursor-pointer font-semibold">{UI_MESSAGES.SHOW_RAW_JSON}</summary>
                    <JsonPreview data={data} label="Kompletní odpověď"/>
                </details>
            )}
        </div>
    );
};
