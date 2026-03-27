import {useEffect} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {ArrowRightEndOnRectangleIcon, HashtagIcon, LockClosedIcon} from '@heroicons/react/24/outline';
import {useAuth} from '../contexts/AuthContext2';
import {Alert} from '../components/UI/Alert';
import {Button} from '../components/UI/Button';
import {TextField} from '../components/UI/forms/TextField';
import {labels} from '../localization/labels';

const LoginPage = () => {
    const {isAuthenticated, isLoading} = useAuth();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    let loginError: string | null = null;
    if (searchParams.has('error')) {
        const errorParam = searchParams.get('error');
        if (!errorParam) {
            loginError = labels.errors.invalidCredentials;
        } else {
            loginError = labels.errors.configurationError;
        }
    }

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated, navigate]);

    if (isLoading || isAuthenticated) {
        return null;
    }

    return (
        <div className="min-h-screen bg-slate-100 dark:bg-zinc-950 flex items-center justify-center p-8">
            <div className="bg-white dark:bg-zinc-900 dark:border dark:border-zinc-800 rounded-2xl p-10 w-[360px] flex flex-col items-center gap-6">
                {/* Logo */}
                <div className="w-14 h-14 bg-blue-600 rounded-[14px] flex items-center justify-center">
                    <svg className="w-[30px] h-[30px] text-white" xmlns="http://www.w3.org/2000/svg"
                         fill="none" stroke="currentColor" strokeWidth="2"
                         strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                        <circle cx="12" cy="12" r="10"/>
                        <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>
                    </svg>
                </div>

                <div className="text-center -mb-2">
                    <h1 className="text-[28px] font-bold text-zinc-900 dark:text-zinc-50">Klabis</h1>
                    <p className="text-sm text-zinc-400 dark:text-zinc-500 mt-1">Přihlaste se ke svému účtu</p>
                </div>

                {loginError && <Alert severity="error">{loginError}</Alert>}

                {/* Form POSTs directly to Spring Security's /login endpoint */}
                <form method="post" action="/login" className="w-full flex flex-col gap-6">
                    <div className="relative">
                        <HashtagIcon className="absolute left-3 top-[2.1rem] w-4 h-4 text-zinc-400 dark:text-zinc-500 pointer-events-none z-10"/>
                        <TextField
                            name="username"
                            label="Registrační číslo"
                            placeholder="např. 12345"
                            autoComplete="username"
                            // eslint-disable-next-line jsx-a11y/no-autofocus
                            autoFocus
                            className="pl-9"
                        />
                    </div>

                    <div className="relative">
                        <LockClosedIcon className="absolute left-3 top-[2.1rem] w-4 h-4 text-zinc-400 dark:text-zinc-500 pointer-events-none z-10"/>
                        <TextField
                            name="password"
                            type="password"
                            label="Heslo"
                            placeholder="••••••••"
                            autoComplete="current-password"
                            className="pl-9"
                        />
                    </div>

                    <Button
                        type="submit"
                        fullWidth
                        startIcon={<ArrowRightEndOnRectangleIcon className="w-[18px] h-[18px]"/>}
                    >
                        Přihlásit se
                    </Button>
                </form>
            </div>
        </div>
    );
};

export default LoginPage;
