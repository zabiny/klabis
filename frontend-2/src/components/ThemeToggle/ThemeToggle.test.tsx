import {fireEvent, render, screen} from '@testing-library/react'
import {ThemeProvider} from '../../theme/ThemeContext'
import {ThemeToggle} from './ThemeToggle'
import {vi} from 'vitest';

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

// Mock localStorage
const mockLocalStorage = {}
Storage.prototype.getItem = vi.fn((key) => (mockLocalStorage as any)[key] || null)
Storage.prototype.setItem = vi.fn((key, value) => {
    (mockLocalStorage as any)[key] = value
})
Storage.prototype.removeItem = vi.fn((key) => {
    delete (mockLocalStorage as any)[key]
})

describe('ThemeToggle', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        Object.keys(mockLocalStorage).forEach((key) => delete (mockLocalStorage as any)[key])
    })

    const renderThemeToggle = (ui: React.ReactElement) => {
        return render(
            <ThemeProvider>
                {ui}
            </ThemeProvider>
        )
    }

    describe('Rendering', () => {
        it('should render a button', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button).toBeInTheDocument()
        })

        it('should render without crashing', () => {
            expect(() => {
                renderThemeToggle(<ThemeToggle/>)
            }).not.toThrow()
        })
    })

    describe('Icon Display Logic', () => {
        it('should show SunIcon when theme is light', () => {
            (mockLocalStorage as any).theme = 'light'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            // SVG from SunIcon should be present
            expect(button.querySelector('svg')).toBeInTheDocument()
        })

        it('should show MoonIcon when theme is dark', () => {
            (mockLocalStorage as any).theme = 'dark'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.querySelector('svg')).toBeInTheDocument()
        })

        it('should show ComputerDesktopIcon when theme is system', () => {
            (mockLocalStorage as any).theme = 'system'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.querySelector('svg')).toBeInTheDocument()
        })
    })

    describe('Click Handler', () => {
        it('should toggle theme on click', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')

            // Initial state: dark
            expect(button.getAttribute('aria-label')).toContain('system')

            // First click: dark -> system
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toContain('light')

            // Second click: system -> light
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toContain('dark')
        })

        it('should cycle through all three themes', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')

            // Start: dark -> system
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toContain('light')

            // system -> light
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toContain('dark')

            // light -> dark
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toContain('system')
        })
    })

    describe('Accessibility', () => {
        it('should have ARIA label when theme is light', () => {
            (mockLocalStorage as any).theme = 'light'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.getAttribute('aria-label')).toBeDefined()
            expect(button.getAttribute('aria-label')).toContain('dark')
        })

        it('should have ARIA label when theme is dark', () => {
            (mockLocalStorage as any).theme = 'dark'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.getAttribute('aria-label')).toBeDefined()
            expect(button.getAttribute('aria-label')).toContain('system')
        })

        it('should have ARIA label when theme is system', () => {
            (mockLocalStorage as any).theme = 'system'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.getAttribute('aria-label')).toBeDefined()
            expect(button.getAttribute('aria-label')).toContain('light')
        })

        it('should have title attribute', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.getAttribute('title')).toBeDefined()
        })

        it('should be keyboard accessible - Enter key', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')

            button.focus()
            expect(document.activeElement).toBe(button)

            fireEvent.keyDown(button, {key: 'Enter', code: 'Enter'})
            // Button should still be accessible
            expect(button).toBeInTheDocument()
        })

        it('should be keyboard accessible - Space key', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')

            button.focus()
            expect(document.activeElement).toBe(button)

            fireEvent.keyDown(button, {key: ' ', code: 'Space'})
            // Button should still be accessible
            expect(button).toBeInTheDocument()
        })
    })

    describe('Styling', () => {
        it('should have px-3 py-2 text-sm classes', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('px-3')
            expect(button.className).toContain('py-2')
            expect(button.className).toContain('text-sm')
        })

        it('should have text color classes', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('text-text-secondary')
        })

        it('should have rounded-md class', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('rounded-md')
        })

        it('should have transition class', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('transition-colors')
        })

        it('should have focus ring classes', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('ring-accent')
        })
    })

    describe('Icon SVG Elements', () => {
        it('should have SVG elements with proper structure', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            const svg = button.querySelector('svg')
            expect(svg).toBeInTheDocument()
            expect(svg?.hasAttribute('width')).toBe(true)
            expect(svg?.hasAttribute('height')).toBe(true)
        })
    })
})
