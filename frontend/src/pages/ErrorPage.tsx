import {type ReactElement} from 'react';
import {Card} from '../components/UI';
import NotFoundPage from './NotFoundPage';
import ForbiddenPage from './ForbiddenPage';
import {describeError} from '../localization';

interface ErrorWithStatus {
    status?: number;
}

interface ErrorPageProps {
    error: ErrorWithStatus | Error | null | undefined;
}

function extractStatus(error: ErrorWithStatus | Error): number | undefined {
    return 'status' in error && typeof error.status === 'number' ? error.status : undefined;
}

export function ErrorPage({error}: ErrorPageProps): ReactElement | null {
    if (!error) {
        return null;
    }

    const status = extractStatus(error);

    if (status === 404) {
        return <NotFoundPage/>;
    }

    if (status === 403 || status === 401) {
        return <ForbiddenPage/>;
    }

    return (
        <div className="flex justify-center items-center min-h-screen">
            <Card className="p-8 flex flex-col items-center max-w-md">
                <h1 className="text-2xl font-semibold text-gray-900 dark:text-white mb-4">
                    Něco se pokazilo
                </h1>
                <p className="text-center text-gray-500 dark:text-gray-400 text-sm">
                    {describeError(error)}
                </p>
            </Card>
        </div>
    );
}
