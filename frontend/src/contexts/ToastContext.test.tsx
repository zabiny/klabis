import {renderHook, act} from '@testing-library/react';
import {ToastProvider, useToast} from './ToastContext';

describe('ToastContext', () => {
    const wrapper = ({children}: {children: React.ReactNode}) => (
        <ToastProvider>{children}</ToastProvider>
    );

    it('starts with empty toasts', () => {
        const {result} = renderHook(() => useToast(), {wrapper});
        expect(result.current.toasts).toEqual([]);
    });

    it('addToast adds a toast', () => {
        const {result} = renderHook(() => useToast(), {wrapper});
        act(() => {
            result.current.addToast('Test message', 'success');
        });
        expect(result.current.toasts).toHaveLength(1);
        expect(result.current.toasts[0].message).toBe('Test message');
        expect(result.current.toasts[0].type).toBe('success');
    });

    it('removeToast removes a toast by id', () => {
        const {result} = renderHook(() => useToast(), {wrapper});
        act(() => {
            result.current.addToast('First', 'success');
            result.current.addToast('Second', 'error');
        });
        const firstId = result.current.toasts[0].id;
        act(() => {
            result.current.removeToast(firstId);
        });
        expect(result.current.toasts).toHaveLength(1);
        expect(result.current.toasts[0].message).toBe('Second');
    });

    it('returns no-op context when used outside provider', () => {
        const {result} = renderHook(() => useToast());
        expect(result.current.toasts).toEqual([]);
        act(() => {
            result.current.addToast('Should be ignored', 'success');
        });
        expect(result.current.toasts).toEqual([]);
    });
});
