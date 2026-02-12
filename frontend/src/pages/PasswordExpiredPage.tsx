import { useNavigate } from 'react-router-dom';
import { RequestNewTokenForm } from '../components/auth/RequestNewTokenForm';

/**
 * PasswordExpiredPage - Public page for requesting a new password setup token
 * Route: /password-setup/request
 */
const PasswordExpiredPage = () => {
    const navigate = useNavigate();

    const handleSuccess = () => {
        // Redirect to login after showing success message
        setTimeout(() => {
            navigate('/login', {
                state: {
                    message: 'Pokud je váš účet stále čekající na aktivaci, obdržíte email s novým odkazem.',
                },
            });
        }, 3000);
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-surface-base p-4">
            <RequestNewTokenForm onSuccess={handleSuccess} />
        </div>
    );
};

export default PasswordExpiredPage;
