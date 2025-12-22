import {type ReactElement, useState} from 'react';
import {useHalRoute} from '../contexts/HalRouteContext';
import type {HalCollectionResponse, HalResponse} from '../api';
import {Alert, Modal, Spinner} from '../components/UI';
import {JsonPreview} from '../components/JsonPreview';
import {HalLinksSection} from '../components/HalLinksSection';
import {HalFormsSection} from '../components/HalFormsSection';
import {useHalActions} from '../hooks/useHalActions';
import {TABLE_HEADERS, UI_MESSAGES} from '../constants/messages';
import {isHalResponse} from "../components/HalFormsForm/utils.ts";
import NotFoundPage from "./NotFoundPage.tsx";

/**
 * Generic page for displaying HAL resources
 * Automatically detects whether the resource is a collection or item
 * and renders the appropriate display component
 *
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
 * Get truncated JSON preview and check if it was truncated
 */
function getTruncatedJsonPreview(obj: any, maxLength: number = 100): { preview: string; isTruncated: boolean } {
    const cleaned = stripHalMetadata(obj);
    const fullJson = JSON.stringify(cleaned);
    const preview = fullJson.substring(0, maxLength);
    return {
        preview: preview + (fullJson.length > maxLength ? '...' : ''),
        isTruncated: fullJson.length > maxLength,
    };
}

/**
 * Display a collection of items in a table format
 */
interface GenericCollectionDisplayProps {
    data: HalCollectionResponse;
}

const GenericCollectionDisplay = ({data}: GenericCollectionDisplayProps): ReactElement => {
    const [selectedItemForJsonView, setSelectedItemForJsonView] = useState<Record<string, unknown> | null>(null);
    const {selectedTemplate, setSelectedTemplate, submitError, isSubmitting, handleNavigateToItem, handleFormSubmit} = useHalActions();
    const items = Object.values(data._embedded || {}).flat();

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
                            <th className="px-4 py-2 text-left font-semibold">{TABLE_HEADERS.DATA}</th>
                            <th className="px-4 py-2 text-left font-semibold w-fit">{TABLE_HEADERS.ACTIONS}</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y">
                        {items.map((item: any, index: number) => {
                            const {preview, isTruncated} = getTruncatedJsonPreview(item);
                            return (
                                <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                                    <td className="px-4 py-2">
                                        <div className="flex items-center gap-2">
                                            <code className="text-xs text-gray-600 dark:text-gray-400 truncate">
                                                {preview}
                                            </code>
                                            {isTruncated && (
                                                <button
                                                    onClick={() => setSelectedItemForJsonView(item)}
                                                    className="flex-shrink-0 text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 transition-colors"
                                                    title="View full JSON"
                                                    aria-label="View full JSON"
                                                >
                                                    <svg
                                                        className="w-4 h-4"
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
                                            )}
                                        </div>
                                    </td>
                                    <td className="px-4 py-2 w-fit">
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
                            );
                        })}
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

            <HalFormsSection
                templates={data._templates}
                data={data}
                selectedTemplate={selectedTemplate}
                onSelectTemplate={setSelectedTemplate}
                onSubmit={handleFormSubmit}
                submitError={submitError}
                isSubmitting={isSubmitting}
            />

            {/* Full JSON preview for advanced users */}
            <details className="mt-4 p-2 border rounded bg-gray-50 dark:bg-gray-900">
                <summary className="cursor-pointer font-semibold">{UI_MESSAGES.SHOW_RAW_JSON}</summary>
                <JsonPreview data={data} label={`Kompletní odpověď (${items.length} položek)`}/>
            </details>

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
    const {selectedTemplate, setSelectedTemplate, submitError, isSubmitting, handleNavigateToItem, handleFormSubmit} = useHalActions();

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

            <HalFormsSection
                templates={data._templates}
                data={data}
                selectedTemplate={selectedTemplate}
                onSelectTemplate={setSelectedTemplate}
                onSubmit={handleFormSubmit}
                submitError={submitError}
                isSubmitting={isSubmitting}
            />

            {/* Full JSON preview */}
            <details className="mt-4 p-2 border rounded bg-gray-50 dark:bg-gray-900">
                <summary className="cursor-pointer font-semibold">{UI_MESSAGES.SHOW_RAW_JSON}</summary>
                <JsonPreview data={data} label="Kompletní odpověď"/>
            </details>
        </div>
    );
};
