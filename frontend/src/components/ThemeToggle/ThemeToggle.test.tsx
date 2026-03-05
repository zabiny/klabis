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
const mockLocalStorage: any = {}
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
            expect(() => renderThemeToggle(<ThemeToggle/>)).not.toThrow()
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
    })

    describe('Click Handler', () => {
        it('should toggle theme on click', () => {
            (mockLocalStorage as any).theme = 'light'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')

            // Initial state: light
            expect(button.getAttribute('aria-label')).toContain('Přepnout do tmavého režimu')

            // Click: light -> dark
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toContain('Přepnout do světlého režimu')
        })

        it('should cycle through both themes', () => {
            (mockLocalStorage as any).theme = 'light'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')

            // Start with light
            expect(button.getAttribute('aria-label')).toBe('Přepnout do tmavého režimu')

            // First click: light -> dark
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toBe('Přepnout do světlého režimu')

            // Second click: dark -> light
            fireEvent.click(button)
            expect(button.getAttribute('aria-label')).toBe('Přepnout do tmavého režimu')
        })
    })

    describe('ARIA Attributes', () => {
        it('should have proper aria-label for light mode', () => {
            (mockLocalStorage as any).theme = 'light'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.getAttribute('aria-label')).toBe('Přepnout do tmavého režimu')
        })

        it('should have proper aria-label for dark mode', () => {
            (mockLocalStorage as any).theme = 'dark'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.getAttribute('aria-label')).toBe('Přepnout do světlého režimu')
        })

        it('should have title attribute', () => {
            (mockLocalStorage as any).theme = 'light'
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.getAttribute('title')).toBeTruthy()
        })
    })

    describe('Button Styling', () => {
        it('should have proper button classes', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('p-2.5')
            expect(button.className).toContain('rounded-lg')
        })

        it('should have focus ring classes', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('focus:ring-2')
            expect(button.className).toContain('focus:ring-primary')
        })

        it('should have transition classes', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            expect(button.className).toContain('transition-all')
        })
    })

    describe('Icon SVG Elements', () => {
        it('should have SVG elements with proper structure', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            const svg = button.querySelector('svg')
            expect(svg).toBeInTheDocument()
            expect(svg).toHaveAttribute('viewBox')
            expect(svg).toHaveAttribute('stroke-width')
        })

        it('should have SVG with fill="none" and stroke="currentColor"', () => {
            renderThemeToggle(<ThemeToggle/>)
            const button = screen.getByRole('button')
            const svg = button.querySelector('svg')
            expect(svg).toHaveAttribute('fill', 'none')
            expect(svg).toHaveAttribute('stroke', 'currentColor')
        })
    })
})
