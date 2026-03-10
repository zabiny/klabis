import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../contexts/AuthContext2';

const LoginPage = () => {
    const {isAuthenticated, isLoading} = useAuth();
    const navigate = useNavigate();
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated, navigate]);

    useEffect(() => {
        const loginError = new URLSearchParams(window.location.search).get('error');
        if (loginError) {
            setError('Nesprávné registrační číslo nebo heslo.');
        }
    }, []);

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

                {error && (
                    <div className="w-full bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg px-3.5 py-2.5 text-sm text-red-600 dark:text-red-400">
                        {error}
                    </div>
                )}

                {/* Form POSTs directly to Spring Security's /login endpoint */}
                <form method="post" action="/login" className="w-full flex flex-col gap-6">
                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="username" className="text-[13px] font-medium text-zinc-700 dark:text-zinc-300">
                            Registrační číslo
                        </label>
                        <div className="relative flex items-center">
                            <svg className="absolute left-3 w-4 h-4 text-zinc-400 dark:text-zinc-500 pointer-events-none"
                                 xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                                 strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                                <line x1="4" x2="20" y1="9" y2="9"/>
                                <line x1="4" x2="20" y1="15" y2="15"/>
                                <line x1="10" x2="8" y1="3" y2="21"/>
                                <line x1="16" x2="14" y1="3" y2="21"/>
                            </svg>
                            <input
                                id="username"
                                name="username"
                                type="text"
                                placeholder="např. 12345"
                                autoComplete="username"
                                // eslint-disable-next-line jsx-a11y/no-autofocus
                                autoFocus
                                className="w-full h-11 pl-9 pr-3 border border-zinc-300 dark:border-zinc-700 rounded-lg text-sm text-zinc-900 dark:text-zinc-100 bg-white dark:bg-zinc-800 placeholder:text-zinc-400 dark:placeholder:text-zinc-600 focus:outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-600/15"
                            />
                        </div>
                    </div>

                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="password" className="text-[13px] font-medium text-zinc-700 dark:text-zinc-300">
                            Heslo
                        </label>
                        <div className="relative flex items-center">
                            <svg className="absolute left-3 w-4 h-4 text-zinc-400 dark:text-zinc-500 pointer-events-none"
                                 xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                                 strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                                <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                            </svg>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                placeholder="••••••••"
                                autoComplete="current-password"
                                className="w-full h-11 pl-9 pr-3 border border-zinc-300 dark:border-zinc-700 rounded-lg text-sm text-zinc-900 dark:text-zinc-100 bg-white dark:bg-zinc-800 placeholder:text-zinc-400 dark:placeholder:text-zinc-600 focus:outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-600/15"
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="w-full h-11 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white rounded-[10px] text-[15px] font-semibold flex items-center justify-center gap-2 transition-colors"
                    >
                        <svg className="w-[18px] h-[18px]" xmlns="http://www.w3.org/2000/svg"
                             fill="none" stroke="currentColor" strokeWidth="2"
                             strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                            <polyline points="10 17 15 12 10 7"/>
                            <line x1="15" x2="3" y1="12" y2="12"/>
                        </svg>
                        Přihlásit se
                    </button>
                </form>
            </div>
        </div>
    );
};

export default LoginPage;
