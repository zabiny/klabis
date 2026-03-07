import {createContext, type ReactElement, type ReactNode, useCallback, useContext, useState} from 'react';
import type {ToastMessage} from '../components/UI/Toast';

interface ToastContextValue {
    toasts: ToastMessage[];
    addToast: (message: string, type: ToastMessage['type'], duration?: number) => void;
    removeToast: (id: string) => void;
}

const noopToastContext: ToastContextValue = {
    toasts: [],
    addToast: () => {},
    removeToast: () => {},
};

const ToastContext = createContext<ToastContextValue>(noopToastContext);

export function ToastProvider({children}: { children: ReactNode }): ReactElement {
    const [toasts, setToasts] = useState<ToastMessage[]>([]);

    const addToast = useCallback((message: string, type: ToastMessage['type'], duration: number = 5000) => {
        const id = `toast-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
        setToasts(prev => [...prev, {id, message, type, duration}]);
    }, []);

    const removeToast = useCallback((id: string) => {
        setToasts(prev => prev.filter(t => t.id !== id));
    }, []);

    return (
        <ToastContext.Provider value={{toasts, addToast, removeToast}}>
            {children}
        </ToastContext.Provider>
    );
}

export function useToast(): ToastContextValue {
    return useContext(ToastContext);
}
