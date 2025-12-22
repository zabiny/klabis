import {type ReactElement, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useHalRoute} from '../contexts/HalRouteContext';
import type {HalCollectionResponse, HalFormsTemplate, HalResponse, TemplateTarget} from '../api';
import {Alert, Button, Spinner} from '../components/UI';
import {JsonPreview} from '../components/JsonPreview';
import {halFormsFieldsFactory, HalFormsForm} from '../components/HalFormsForm';
import {isFormValidationError, submitHalFormsData} from '../api/hateoas';

/**
 * Convert a full API URL to a navigation path
 * Removes hostname and strips /api prefix (HalRouteContext will add it back)
 * Example: http://localhost:8080/api/members/123 -> /members/123
 *
 * This works with HalRouteContext which automatically prefixes /api
 * when fetching data, so the route path should not include /api.
 */
function extractNavigationPath(url: string): string {
    try {
        const parsedUrl = new URL(url);
        let path = parsedUrl.pathname;

        // Remove /api prefix if present, since HalRouteContext adds it back
        if (path.startsWith('/api')) {
            path = path.substring(4); // Remove '/api'
        }

        return path;
    } catch {
        // If URL parsing fails, assume it's already a path
        // Remove /api prefix if present
        if (url.startsWith('/api')) {
            return url.substring(4);
        }
        return url;
    }
}

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
function isHalCollection(data: any): data is HalCollectionResponse {
    return (data?.page !== undefined) || (data?._embedded !== undefined && !isEmptyObject(data._embedded));
}

/**
 * Check if an object is empty
 */
function isEmptyObject(obj: any): boolean {
    return typeof obj === 'object' && Object.keys(obj || {}).length === 0;
}

/**
 * Display a collection of items in a table format
 */
interface GenericCollectionDisplayProps {
    data: HalCollectionResponse;
}

const GenericCollectionDisplay = ({data}: GenericCollectionDisplayProps): ReactElement => {
    const navigate = useNavigate();
    const {pathname, refetch} = useHalRoute();
    const [selectedTemplate, setSelectedTemplate] = useState<HalFormsTemplate | null>(null);
    const [submitError, setSubmitError] = useState<Error | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const items = Object.values(data._embedded || {}).flat();

    if (!items || items.length === 0) {
        return (
            <Alert severity="info">
                <p>Kolekce je prázdná</p>
            </Alert>
        );
    }

    const handleNavigateToItem = (href: string) => {
        const path = extractNavigationPath(href);
        navigate(path);
    };

    const handleFormSubmit = async (formData: Record<string, any>) => {
        if (!selectedTemplate) return;

        setIsSubmitting(true);
        setSubmitError(null);

        try {
            const submitTarget: TemplateTarget = {
                target: '/api' + pathname,
                method: selectedTemplate.method || 'POST',
            };
            await submitHalFormsData(submitTarget, formData);
            // Refetch data after successful submission
            await refetch();
            // Close the form
            setSelectedTemplate(null);
        } catch (err) {
            setSubmitError(err instanceof Error ? err : new Error('Failed to submit form'));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold">Položky</h2>
                {data.page && (
                    <div className="text-sm text-gray-600">
                        Celkem: {data.page.totalElements} položek
                        ({(data.page.number ?? 0) + 1} z {data.page.totalPages} stran)
                    </div>
                )}
            </div>

            {/* Simple table display */}
            <div className="overflow-x-auto border rounded-lg">
                <table className="w-full text-sm">
                    <thead className="bg-gray-100 dark:bg-gray-800">
                    <tr>
                        <th className="px-4 py-2 text-left font-semibold">ID</th>
                        <th className="px-4 py-2 text-left font-semibold">Údaje</th>
                        <th className="px-4 py-2 text-left font-semibold">Akce</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y">
                    {items.map((item: any, index: number) => (
                        <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                            <td className="px-4 py-2">{item?.id || item?._links?.self?.href || index}</td>
                            <td className="px-4 py-2 max-w-md truncate">
                                <code className="text-xs text-gray-600 dark:text-gray-400">
                                    {JSON.stringify(item).substring(0, 100)}...
                                </code>
                            </td>
                            <td className="px-4 py-2">
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

            {/* Links section */}
            {data._links && Object.keys(data._links).length > 0 ? (
                <div className="mt-4 p-4 border rounded bg-blue-50 dark:bg-blue-900">
                    <h3 className="font-semibold mb-2">Dostupné akce</h3>
                    <div className="flex flex-wrap gap-2">
                        {Object.entries(data._links as Record<string, any>)
                            .filter(([rel]) => rel !== 'self')
                            .map(([rel, link]: [string, any]) => {
                                const links = Array.isArray(link) ? link : [link];
                                return links.map((l: any, idx: number) => (
                                    <button
                                        key={`${rel}-${idx}`}
                                        onClick={() => handleNavigateToItem(l.href)}
                                        className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm border-none cursor-pointer"
                                        title={rel}
                                    >
                                        {l.title || rel}
                                    </button>
                                ));
                            })}
                    </div>
                </div>
            ) : null}

            {/* Templates/Forms section */}
            {data._templates && Object.keys(data._templates).length > 0 ? (
                <div className="mt-4 p-4 border rounded bg-green-50 dark:bg-green-900">
                    <h3 className="font-semibold mb-2">Dostupné formuláře</h3>
                    {selectedTemplate ? (
                        <div className="space-y-4">
                            <div className="flex items-center justify-between mb-4">
                                <h4 className="font-semibold">{selectedTemplate.title || 'Formulář'}</h4>
                                <Button
                                    onClick={() => setSelectedTemplate(null)}
                                    variant="secondary"
                                    size="sm"
                                >
                                    Zavřít
                                </Button>
                            </div>

                            {submitError && (
                                <Alert severity="error">
                                    <div className="space-y-1">
                                        <p>{submitError.message}</p>
                                        {isFormValidationError(submitError) && (
                                            <ul className="list-disc list-inside text-sm">
                                                {Object.entries(submitError.validationErrors).map(([field, error]) => (
                                                    <li key={field}>{field}: {error}</li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                </Alert>
                            )}

                            <HalFormsForm
                                data={data}
                                template={selectedTemplate}
                                onSubmit={handleFormSubmit}
                                onCancel={() => setSelectedTemplate(null)}
                                isSubmitting={isSubmitting}
                                fieldsFactory={halFormsFieldsFactory}
                            />
                        </div>
                    ) : (
                        <div className="flex flex-wrap gap-2">
                            {Object.entries(data._templates as Record<string, any>).map(([templateName, template]: [string, any]) => (
                                <button
                                    key={templateName}
                                    onClick={() => setSelectedTemplate(template)}
                                    className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 text-sm border-none cursor-pointer"
                                    title={template.title || templateName}
                                >
                                    {template.title || templateName}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            ) : null}

            {/* Full JSON preview for advanced users */}
            <details className="mt-4 p-2 border rounded bg-gray-50 dark:bg-gray-900">
                <summary className="cursor-pointer font-semibold">Zobrazit surový JSON</summary>
                <JsonPreview data={data} label={`Kompletní odpověď (${items.length} položek)`}/>
            </details>
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
    const navigate = useNavigate();
    const {pathname, refetch} = useHalRoute();
    const [selectedTemplate, setSelectedTemplate] = useState<HalFormsTemplate | null>(null);
    const [submitError, setSubmitError] = useState<Error | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleNavigateToItem = (href: string) => {
        const path = extractNavigationPath(href);
        navigate(path);
    };

    const handleFormSubmit = async (formData: Record<string, any>) => {
        if (!selectedTemplate) return;

        setIsSubmitting(true);
        setSubmitError(null);

        try {
            const submitTarget: TemplateTarget = {
                target: '/api' + pathname,
                method: selectedTemplate.method || 'POST',
            };
            await submitHalFormsData(submitTarget, formData);
            // Refetch data after successful submission
            await refetch();
            // Close the form
            setSelectedTemplate(null);
        } catch (err) {
            setSubmitError(err instanceof Error ? err : new Error('Failed to submit form'));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="space-y-4">
            <h2 className="text-2xl font-bold">Detaily</h2>

            {/* Properties table */}
            <div className="overflow-x-auto border rounded-lg">
                <table className="w-full text-sm">
                    <thead className="bg-gray-100 dark:bg-gray-800">
                    <tr>
                        <th className="px-4 py-2 text-left font-semibold w-1/4">Atribut</th>
                        <th className="px-4 py-2 text-left font-semibold">Hodnota</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y">
                    {Object.entries(data)
                        .filter(([key]) => !key.startsWith('_'))
                        .map(([key, value]: [string, any]) => (
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

            {/* Links section */}
            {data._links && Object.keys(data._links).length > 0 ? (
                <div className="mt-4 p-4 border rounded bg-blue-50 dark:bg-blue-900">
                    <h3 className="font-semibold mb-2">Dostupné akce</h3>
                    <div className="flex flex-wrap gap-2">
                        {Object.entries(data._links as Record<string, any>)
                            .filter(([rel]) => rel !== 'self')
                            .map(([rel, link]: [string, any]) => {
                                const links = Array.isArray(link) ? link : [link];
                                return links.map((l: any, idx: number) => (
                                    <button
                                        key={`${rel}-${idx}`}
                                        onClick={() => handleNavigateToItem(l.href)}
                                        className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm border-none cursor-pointer"
                                        title={rel}
                                    >
                                        {l.title || rel}
                                    </button>
                                ));
                            })}
                    </div>
                </div>
            ) : null}

            {/* Templates/Forms section */}
            {data._templates && Object.keys(data._templates).length > 0 ? (
                <div className="mt-4 p-4 border rounded bg-green-50 dark:bg-green-900">
                    <h3 className="font-semibold mb-2">Dostupné formuláře</h3>
                    {selectedTemplate ? (
                        <div className="space-y-4">
                            <div className="flex items-center justify-between mb-4">
                                <h4 className="font-semibold">{selectedTemplate.title || 'Formulář'}</h4>
                                <Button
                                    onClick={() => setSelectedTemplate(null)}
                                    variant="secondary"
                                    size="sm"
                                >
                                    Zavřít
                                </Button>
                            </div>

                            {submitError && (
                                <Alert severity="error">
                                    <div className="space-y-1">
                                        <p>{submitError.message}</p>
                                        {isFormValidationError(submitError) && (
                                            <ul className="list-disc list-inside text-sm">
                                                {Object.entries(submitError.validationErrors).map(([field, error]) => (
                                                    <li key={field}>{field}: {error}</li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                </Alert>
                            )}

                            <HalFormsForm
                                data={data}
                                template={selectedTemplate}
                                onSubmit={handleFormSubmit}
                                onCancel={() => setSelectedTemplate(null)}
                                isSubmitting={isSubmitting}
                                fieldsFactory={halFormsFieldsFactory}
                            />
                        </div>
                    ) : (
                        <div className="flex flex-wrap gap-2">
                            {Object.entries(data._templates as Record<string, any>).map(([templateName, template]: [string, any]) => (
                                <button
                                    key={templateName}
                                    onClick={() => setSelectedTemplate(template)}
                                    className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 text-sm border-none cursor-pointer"
                                    title={template.title || templateName}
                                >
                                    {template.title || templateName}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            ) : null}

            {/* Full JSON preview */}
            <details className="mt-4 p-2 border rounded bg-gray-50 dark:bg-gray-900">
                <summary className="cursor-pointer font-semibold">Zobrazit surový JSON</summary>
                <JsonPreview data={data} label="Kompletní odpověď"/>
            </details>
        </div>
    );
};
