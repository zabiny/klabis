import {useEffect, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {Alert, Button, Spinner} from '../components/UI';
import {PasswordSetupForm} from '../components/auth/PasswordSetupForm';
import {TokenValidationError, validateToken} from '../api/passwordSetup';
import {usePasswordSetup} from '../hooks/usePasswordSetup';

/**
 * PasswordSetupPage - Public page for setting up password via token from email
 * Route: /password-setup?token=XYZ
 */
const PasswordSetupPage = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token');

    const [isValidating, setIsValidating] = useState(true);
    const [isValid, setIsValid] = useState(false);
    const [validationError, setValidationError] = useState<string | null>(null);

    useEffect(() => {
        if (!token) {
            setValidationError('Token nebyl nalezen. Prosím použijte odkaz z emailu.');
            setIsValidating(false);
            return;
        }

        const validateTokenAsync = async () => {
            try {
                await validateToken(token);
                setIsValid(true);
            } catch (error) {
                if (error instanceof TokenValidationError) {
                    if (error.status === 410) {
                        setValidationError('Platnost tokenu vypršela nebo byl již použit. Požádejte si o nový.');
                    } else if (error.status === 404) {
                        setValidationError('Neplatný token. Požádejte si o nový.');
                    } else {
                        setValidationError(error.detail.detail || 'Token nelze ověřit.');
                    }
                } else {
                    setValidationError('Došlo k neočekávané chybě při ověřování tokenu.');
                }
            } finally {
                setIsValidating(false);
            }
        };

        validateTokenAsync();
    }, [token]);

    const handleSuccess = () => {
        navigate('/login', {
            state: {
                message: 'Účet byl úspěšně aktivován. Nyní se můžete přihlásit.',
            },
        });
    };

    const {submit, isSubmitting, serverError, clearServerError, showSuccess} = usePasswordSetup(
        token ?? '',
        {onSuccess: handleSuccess},
    );

    const handleRequestNewToken = () => {
        navigate('/password-setup/request');
    };

    if (isValidating) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-surface-base">
                <div className="text-center">
                    <Spinner size="lg" />
                    <p className="mt-4 text-text-secondary">Ověřuji token...</p>
                </div>
            </div>
        );
    }

    if (validationError || !isValid) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-surface-base p-4">
                <div className="max-w-md w-full">
                    <Alert severity="error" className="mb-6">
                        {validationError || 'Token je neplatný.'}
                    </Alert>

                    <div className="bg-surface-raised rounded-md border border-border p-6">
                        <h2 className="text-xl font-semibold text-text-primary mb-4">
                            Potřebujete nový token?
                        </h2>
                        <p className="text-text-secondary mb-6">
                            Pokud platnost vašeho tokenu vypršela nebo jste ho již použili,
                            můžete si vyžádat nový pomocí registračního čísla a emailu.
                        </p>
                        <Button
                            onClick={handleRequestNewToken}
                            variant="primary"
                            fullWidth
                        >
                            Vyžádat nový token
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-surface-base p-4">
            <PasswordSetupForm
                onSubmit={submit}
                isSubmitting={isSubmitting}
                serverError={serverError}
                onClearServerError={clearServerError}
                showSuccess={showSuccess}
            />
        </div>
    );
};

export default PasswordSetupPage;
