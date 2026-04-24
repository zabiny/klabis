import { useNavigate } from 'react-router-dom';
import { RequestNewTokenForm } from '../components/auth/RequestNewTokenForm';
import { useRequestNewToken } from '../hooks/useRequestNewToken';

const PasswordExpiredPage = () => {
    const navigate = useNavigate();

    const { submit, isSubmitting, serverError, clearServerError, showSuccess } = useRequestNewToken({
        onSuccess: () => {
            navigate('/login', {
                state: {
                    message: 'Pokud je váš účet stále čekající na aktivaci, obdržíte email s novým odkazem.',
                },
            });
        },
    });

    return (
        <div className="min-h-screen flex items-center justify-center bg-surface-base p-4">
            <RequestNewTokenForm
                onSubmit={submit}
                isSubmitting={isSubmitting}
                serverError={serverError}
                onClearServerError={clearServerError}
                showSuccess={showSuccess}
            />
        </div>
    );
};

export default PasswordExpiredPage;
