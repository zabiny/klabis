import {Button, Card} from '../components/UI';
import {Link as RouterLink} from 'react-router-dom';

const NotFoundPage = () => {
    return (
        <div className="flex justify-center items-center min-h-screen">
            <Card className="p-8 flex flex-col items-center max-w-md">
                <h1 className="text-8xl font-bold text-red-600 dark:text-red-500 mb-4">
                    404
                </h1>
                <h2 className="text-2xl font-semibold text-gray-900 dark:text-white mb-4">
                    Stránka nenalezena
                </h2>
                <p className="text-center text-gray-600 dark:text-gray-400 mb-6">
                    Omlouváme se, ale stránka, kterou hledáte, neexistuje nebo byla přesunuta.
                </p>
                <RouterLink to="/">
                    <Button className="flex items-center gap-2">
                        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                            <path
                                d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"/>
                        </svg>
                        Zpět na úvodní stránku
                    </Button>
                </RouterLink>
            </Card>
        </div>
    );
};

export default NotFoundPage;