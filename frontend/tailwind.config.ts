import type {Config} from 'tailwindcss'

const config: Config = {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    darkMode: 'class',
    theme: {
        extend: {
            colors: {
                // Semantic color mapping using CSS variables
                bg: {
                    base: 'var(--color-bg-base)',
                    elevated: 'var(--color-bg-elevated)',
                    subtle: 'var(--color-bg-subtle)',
                },
                text: {
                    primary: 'var(--color-text-primary)',
                    secondary: 'var(--color-text-secondary)',
                    tertiary: 'var(--color-text-tertiary)',
                },
                border: {
                    subtle: 'var(--color-border-subtle)',
                    DEFAULT: 'var(--color-border-default)',
                    strong: 'var(--color-border-strong)',
                },
                primary: {
                    DEFAULT: 'var(--color-primary)',
                    hover: 'var(--color-primary-hover)',
                    active: 'var(--color-primary-active)',
                    subtle: 'var(--color-primary-subtle)',
                },
                secondary: {
                    DEFAULT: 'var(--color-secondary)',
                    hover: 'var(--color-secondary-hover)',
                    active: 'var(--color-secondary-active)',
                    subtle: 'var(--color-secondary-subtle)',
                },
                accent: {
                    DEFAULT: 'var(--color-accent)',
                    hover: 'var(--color-accent-hover)',
                    subtle: 'var(--color-accent-subtle)',
                },
                success: {
                    DEFAULT: 'var(--color-success)',
                    bg: 'var(--color-success-bg)',
                },
                warning: {
                    DEFAULT: 'var(--color-warning)',
                    bg: 'var(--color-warning-bg)',
                },
                error: {
                    DEFAULT: 'var(--color-error)',
                    bg: 'var(--color-error-bg)',
                },
                info: {
                    DEFAULT: 'var(--color-info)',
                    bg: 'var(--color-info-bg)',
                },
            },
            fontFamily: {
                sans: ['Plus Jakarta Sans', 'system-ui', 'sans-serif'],
                display: ['Space Grotesk', 'Plus Jakarta Sans', 'system-ui', 'sans-serif'],
                mono: ['JetBrains Mono', 'monospace'],
            },
            spacing: {
                'xs': '0.125rem',  // 2px
                'sm': '0.25rem',   // 4px
                'md': '0.5rem',    // 8px
                'lg': '0.75rem',   // 12px
                'xl': '1rem',       // 16px
                '2xl': '1.5rem',   // 24px
                '3xl': '2rem',      // 32px
                '4xl': '3rem',      // 48px
            },
            borderRadius: {
                'sm': '6px',
                'base': '8px',
                'md': '8px',
                'lg': '12px',
                'xl': '16px',
                'pill': '9999px',
            },
            boxShadow: {
                'sm': 'var(--color-shadow-sm)',
                'md': 'var(--color-shadow-md)',
                'lg': 'var(--color-shadow-lg)',
            },
            transitionDuration: {
                'fast': '150ms',
                'base': '200ms',
                'slow': '300ms',
            },
            keyframes: {
                'fade-in': {
                    '0%': { opacity: '0' },
                    '100%': { opacity: '1' },
                },
                'slide-up': {
                    '0%': { transform: 'translateY(1rem)', opacity: '0' },
                    '100%': { transform: 'translateY(0)', opacity: '1' },
                },
                'slide-down': {
                    '0%': { transform: 'translateY(-1rem)', opacity: '0' },
                    '100%': { transform: 'translateY(0)', opacity: '1' },
                },
                'scale-in': {
                    '0%': { transform: 'scale(0.95)', opacity: '0' },
                    '100%': { transform: 'scale(1)', opacity: '1' },
                },
                'bounce-gentle': {
                    '0%, 100%': { transform: 'translateY(0)' },
                    '50%': { transform: 'translateY(-4px)' },
                },
                'shake': {
                    '0%, 100%': { transform: 'translateX(0)' },
                    '25%': { transform: 'translateX(-3px)' },
                    '75%': { transform: 'translateX(3px)' },
                },
                'shimmer': {
                    '0%': { backgroundPosition: '-1000px 0' },
                    '100%': { backgroundPosition: '1000px 0' },
                },
                'spin-slow': {
                    '0%': { transform: 'rotate(0deg)' },
                    '100%': { transform: 'rotate(360deg)' },
                },
            },
            animation: {
                'fade-in': 'fade-in 200ms ease-out',
                'slide-up': 'slide-up 300ms cubic-bezier(0.4, 0, 0.2, 1)',
                'slide-down': 'slide-down 300ms cubic-bezier(0.4, 0, 0.2, 1)',
                'scale-in': 'scale-in 300ms cubic-bezier(0.4, 0, 0.2, 1)',
                'bounce-gentle': 'bounce-gentle 400ms ease-in-out',
                'shake': 'shake 300ms ease-in-out',
                'shimmer': 'shimmer 2s infinite',
                'spin-slow': 'spin-slow 2s linear infinite',
            },
        },
    },
    plugins: [
        require('@tailwindcss/forms'),
        require('@tailwindcss/typography'),
    ],
}

export default config
