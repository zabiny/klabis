import type {FallbackProps} from 'react-error-boundary'
import {Button} from './UI'

/**
 * ErrorFallback component
 * Displays when an unhandled error occurs in the application
 */
export default function ErrorFallback({
                                          error,
                                          resetErrorBoundary
                                      }: FallbackProps) {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
            <div className="bg-white dark:bg-gray-800 p-8 rounded-lg shadow-lg max-w-md w-full">
                <div className="mb-4">
                    <svg
                        className="w-16 h-16 text-red-600 mx-auto"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M12 9v2m0 4v2m0 4v2M6.343 3h11.314a2 2 0 011.961 2.515l-1.286 7.086a2 2 0 01-1.961 1.488H8.972a2 2 0 01-1.961-1.488L5.382 5.515A2 2 0 016.343 3z"
                        />
                    </svg>
                </div>

                <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2 text-center">
                    Něco se pokazilo
                </h1>

                <p className="text-gray-600 dark:text-gray-300 mb-4 text-center text-sm">
                    Aplikace narazila na neočekávanou chybu. Prosím, zkuste to znovu nebo se vrátit později.
                </p>

                <details className="mb-6 bg-gray-50 dark:bg-gray-700 p-3 rounded text-sm">
                    <summary
                        className="cursor-pointer font-semibold text-gray-700 dark:text-gray-200 hover:text-gray-900 dark:hover:text-white">
                        Podrobnosti chyby
                    </summary>
                    <pre
                        className="mt-2 bg-gray-100 dark:bg-gray-900 p-2 rounded overflow-auto text-xs text-gray-800 dark:text-gray-200">
                        {error?.message || 'Neznámá chyba'}
                    </pre>
                    {error?.stack && (
                        <pre
                            className="mt-2 bg-gray-100 dark:bg-gray-900 p-2 rounded overflow-auto text-xs text-gray-600 dark:text-gray-400 max-h-32">
                            {error.stack}
                        </pre>
                    )}
                </details>

                <div className="flex gap-3">
                    <Button
                        onClick={resetErrorBoundary}
                        variant="primary"
                        size="md"
                        className="flex-1"
                    >
                        Zkusit znovu
                    </Button>
                    <Button
                        onClick={() => window.location.href = '/'}
                        variant="secondary"
                        size="md"
                        className="flex-1"
                    >
                        Domů
                    </Button>
                </div>
            </div>
        </div>
    )
}
