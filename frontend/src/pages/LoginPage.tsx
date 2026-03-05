import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Button} from '../components/UI';
import {useAuth} from '../contexts/AuthContext2';

const LoginPage = () => {
    const {login, isAuthenticated, isLoading} = useAuth();
    const navigate = useNavigate();

    // Redirect to home if already authenticated
    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated, navigate]);

    // // Automaticky zahájí login, pokud není autentizováno a není načítání
    useEffect(() => {
        if (!isAuthenticated && !isLoading) {
            login();
        }
    }, [isAuthenticated, isLoading, login]);

    if (!isAuthenticated && !isLoading) {
        return (
            <div className="max-w-sm mx-auto px-4">
                <div className="mt-8 flex flex-col items-center">
                    <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-6">
                        Přihlášení
                    </h2>
                    <Button onClick={() => login()}>
                        Přihlásit se
                    </Button>
                </div>
            </div>
        );
    }

    // Po přihlášení bude automaticky přesměrován, takže zde není potřeba žádné další UI
    return null;
};

export default LoginPage;