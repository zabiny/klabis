import {createContext, useContext} from 'react';
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

export const ToastContext = createContext<ToastContextValue>(noopToastContext);

export function useToast() {
    return useContext(ToastContext);
}
