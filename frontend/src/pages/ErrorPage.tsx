import {type ReactElement} from 'react';
import {Card} from '../components/UI';
import NotFoundPage from './NotFoundPage';
import ForbiddenPage from './ForbiddenPage';

interface ErrorWithStatus {
    responseStatus?: number;
    message: string;
}

interface ErrorPageProps {
    error: ErrorWithStatus | Error | null | undefined;
}

function extractStatus(error: ErrorWithStatus | Error): number | undefined {
    if ('responseStatus' in error && typeof error.responseStatus === 'number') {
        return error.responseStatus;
    }
    const match = error.message.match(/HTTP (\d{3})/);
    return match ? parseInt(match[1], 10) : undefined;
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
                    {error.message}
                </p>
            </Card>
        </div>
    );
}
