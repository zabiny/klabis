import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { usePersistedState } from './usePersistedState';

describe('usePersistedState', () => {
    const TEST_KEY = 'test-key';

    beforeEach(() => {
        localStorage.clear();
        vi.clearAllMocks();
    });

    it('should return default value when localStorage is empty', () => {
        const { result } = renderHook(() => usePersistedState(TEST_KEY, 10));
        expect(result.current[0]).toBe(10);
    });

    it('should persist value to localStorage on change', () => {
        const { result } = renderHook(() => usePersistedState(TEST_KEY, 10));

        act(() => {
            result.current[1](25);
        });

        expect(result.current[0]).toBe(25);
        expect(localStorage.getItem(TEST_KEY)).toBe('25');
    });

    it('should load persisted value from localStorage on mount', () => {
        // Pre-populate localStorage
        localStorage.setItem(TEST_KEY, '50');

        const { result } = renderHook(() => usePersistedState(TEST_KEY, 10));
        expect(result.current[0]).toBe(50);
    });

    it('should handle complex objects', () => {
        const defaultValue = { name: 'test', count: 5 };
        const { result } = renderHook(() => usePersistedState(TEST_KEY, defaultValue));

        const newValue = { name: 'updated', count: 10 };
        act(() => {
            result.current[1](newValue);
        });

        expect(result.current[0]).toEqual(newValue);
        expect(JSON.parse(localStorage.getItem(TEST_KEY)!)).toEqual(newValue);
    });

    it('should handle localStorage parse errors gracefully', () => {
        // Set invalid JSON in localStorage
        localStorage.setItem(TEST_KEY, 'invalid-json');

        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        const { result } = renderHook(() => usePersistedState(TEST_KEY, 10));

        // Should fall back to default value
        expect(result.current[0]).toBe(10);
        expect(consoleWarnSpy).toHaveBeenCalledWith(
            expect.stringContaining('Failed to read localStorage'),
            expect.any(Error)
        );

        consoleWarnSpy.mockRestore();
    });

    it('should handle localStorage write errors gracefully', () => {
        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        // Mock localStorage.setItem to throw
        const setItemSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
            throw new Error('Storage quota exceeded');
        });

        const { result } = renderHook(() => usePersistedState(TEST_KEY, 10));

        act(() => {
            result.current[1](25);
        });

        // Value should still be updated in memory
        expect(result.current[0]).toBe(25);

        // Warning should be logged
        expect(consoleWarnSpy).toHaveBeenCalledWith(
            expect.stringContaining('Failed to write localStorage'),
            expect.any(Error)
        );

        setItemSpy.mockRestore();
        consoleWarnSpy.mockRestore();
    });

    it('should use different keys for different instances', () => {
        const { result: result1 } = renderHook(() => usePersistedState('key1', 10));
        const { result: result2 } = renderHook(() => usePersistedState('key2', 20));

        act(() => {
            result1.current[1](100);
        });

        act(() => {
            result2.current[1](200);
        });

        expect(result1.current[0]).toBe(100);
        expect(result2.current[0]).toBe(200);
        expect(localStorage.getItem('key1')).toBe('100');
        expect(localStorage.getItem('key2')).toBe('200');
    });
});
