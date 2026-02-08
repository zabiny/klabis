import {act, renderHook} from '@testing-library/react'
import {ThemeProvider, useTheme} from './ThemeContext'
import type {ReactNode} from 'react'
import {vi} from 'vitest';

describe('ThemeContext', () => {
    let mockLocalStorage: { [key: string]: string } = {}

    beforeEach(() => {
        // Mock localStorage
        mockLocalStorage = {}
        Storage.prototype.getItem = vi.fn((key) => mockLocalStorage[key] || null)
        Storage.prototype.setItem = vi.fn((key, value) => {
            mockLocalStorage[key] = value
        })
        Storage.prototype.removeItem = vi.fn((key) => {
            delete mockLocalStorage[key]
        })
        Storage.prototype.clear = vi.fn(() => {
            mockLocalStorage = {}
        })

        // Mock window.matchMedia
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: vi.fn().mockImplementation((query) => ({
                matches: query === '(prefers-color-scheme: dark)',
                media: query,
                onchange: null,
                addListener: vi.fn(),
                removeListener: vi.fn(),
                addEventListener: vi.fn(),
                removeEventListener: vi.fn(),
                dispatchEvent: vi.fn(),
            })),
        })

        // Mock document.documentElement classList methods
        vi.spyOn(document.documentElement.classList, 'add')
        vi.spyOn(document.documentElement.classList, 'remove')
    })

    afterEach(() => {
        vi.clearAllMocks()
    })

    const createWrapper = () => {
        return ({children}: { children: ReactNode }) => <ThemeProvider>{children}</ThemeProvider>
    }

    describe('Initialization', () => {
        it('should initialize with light theme from localStorage', () => {
            mockLocalStorage['theme'] = 'light'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.theme).toBe('light')
        })

        it('should initialize with dark theme from localStorage', () => {
            mockLocalStorage['theme'] = 'dark'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.theme).toBe('dark')
        })

        it('should initialize with system theme from localStorage', () => {
            mockLocalStorage['theme'] = 'system'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.theme).toBe('system')
        })

        it('should default to dark theme when system prefers dark and no saved preference', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.theme).toBe('dark')
        })

        it('should apply dark class to document.documentElement', () => {
            renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(document.documentElement.classList.add).toHaveBeenCalled()
        })
    })

    describe('setTheme', () => {
        it('should change theme to light', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.setTheme('light')
            })

            expect(result.current.theme).toBe('light')
        })

        it('should change theme to dark', () => {
            mockLocalStorage['theme'] = 'light'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.setTheme('dark')
            })

            expect(result.current.theme).toBe('dark')
        })

        it('should change theme to system', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.setTheme('system')
            })

            expect(result.current.theme).toBe('system')
        })

        it('should save theme to localStorage', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.setTheme('light')
            })

            expect(localStorage.setItem).toHaveBeenCalledWith('theme', 'light')
        })

        it('should apply theme to document.documentElement', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.setTheme('light')
            })

            expect(document.documentElement.classList.remove).toHaveBeenCalled()
        })
    })

    describe('toggleTheme', () => {
        it('should cycle from light to dark', () => {
            mockLocalStorage['theme'] = 'light'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.toggleTheme()
            })

            expect(result.current.theme).toBe('dark')
        })

        it('should cycle from dark to system', () => {
            mockLocalStorage['theme'] = 'dark'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.toggleTheme()
            })

            expect(result.current.theme).toBe('system')
        })

        it('should cycle from system to light', () => {
            mockLocalStorage['theme'] = 'system'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.toggleTheme()
            })

            expect(result.current.theme).toBe('light')
        })

        it('should cycle through all three states sequentially', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            // Start with dark (from default)
            expect(result.current.theme).toBe('dark')

            // First toggle: dark -> system
            act(() => {
                result.current.toggleTheme()
            })
            expect(result.current.theme).toBe('system')

            // Second toggle: system -> light
            act(() => {
                result.current.toggleTheme()
            })
            expect(result.current.theme).toBe('light')

            // Third toggle: light -> dark
            act(() => {
                result.current.toggleTheme()
            })
            expect(result.current.theme).toBe('dark')
        })
    })

    describe('effectiveTheme', () => {
        it('should return light when theme is light', () => {
            mockLocalStorage['theme'] = 'light'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.effectiveTheme).toBe('light')
        })

        it('should return dark when theme is dark', () => {
            mockLocalStorage['theme'] = 'dark'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.effectiveTheme).toBe('dark')
        })

        it('should return dark when theme is system and system prefers dark', () => {
            mockLocalStorage['theme'] = 'system'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.effectiveTheme).toBe('dark')
        })

        it('should return light when theme is system and system prefers light', () => {
            mockLocalStorage['theme'] = 'system'

            // Mock system preference to light
            Object.defineProperty(window, 'matchMedia', {
                writable: true,
                value: vi.fn().mockImplementation((query) => ({
                    matches: query !== '(prefers-color-scheme: dark)',
                    media: query,
                    onchange: null,
                    addListener: vi.fn(),
                    removeListener: vi.fn(),
                    addEventListener: vi.fn(),
                    removeEventListener: vi.fn(),
                    dispatchEvent: vi.fn(),
                })),
            })

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.effectiveTheme).toBe('light')
        })

        it('should never be system (always light or dark)', () => {
            mockLocalStorage['theme'] = 'system'

            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            expect(result.current.effectiveTheme).not.toBe('system')
            expect(['light', 'dark']).toContain(result.current.effectiveTheme)
        })
    })

    describe('localStorage persistence', () => {
        it('should persist theme changes to localStorage', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.setTheme('light')
            })

            expect(mockLocalStorage['theme']).toBe('light')

            act(() => {
                result.current.setTheme('system')
            })

            expect(mockLocalStorage['theme']).toBe('system')
        })

        it('should persist toggle changes to localStorage', () => {
            const {result} = renderHook(() => useTheme(), {
                wrapper: createWrapper(),
            })

            act(() => {
                result.current.toggleTheme()
            })

            expect(mockLocalStorage['theme']).toBe('system')
        })
    })

    describe('useTheme hook validation', () => {
        it('should throw error when used outside ThemeProvider', () => {
            // Suppress console.error for this test
            const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {
            })

            expect(() => {
                renderHook(() => useTheme())
            }).toThrow('useTheme must be used within ThemeProvider')

            consoleErrorSpy.mockRestore()
        })
    })
})
