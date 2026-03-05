import type {ReactNode} from 'react'
import clsx from 'clsx'

interface AppBarProps {
    children: ReactNode
    className?: string
    position?: 'static' | 'relative' | 'fixed' | 'sticky'
    elevation?: 'sm' | 'md' | 'lg' | 'none'
    bgcolor?: string
    color?: 'default' | 'primary'
}

const elevationMap = {
    'none': '',
    'sm': 'shadow-sm',
    'md': 'shadow-md',
    'lg': 'shadow-lg',
}

const positionMap = {
    'static': 'static',
    'relative': 'relative',
    'fixed': 'fixed',
    'sticky': 'sticky',
}

const colorMap = {
    'default': 'bg-surface-raised border-b border-border',
    'primary': 'bg-primary text-white shadow-md',
}

/**
 * AppBar component - Replaces MUI AppBar
 * Header/navigation bar component
 */
export const AppBar = ({
                           children,
                           className = '',
                           position = 'relative',
                           elevation = 'md',
                           bgcolor,
                           color = 'default',
                       }: AppBarProps) => {
    const classes = clsx(
        'w-full',
        `${positionMap[position]}`,
        position === 'fixed' && 'top-0 left-0 right-0 z-40',
        position === 'sticky' && 'top-0 z-40',
        elevationMap[elevation],
        bgcolor || colorMap[color],
        className
    )

    return (
        <header className={classes} role="banner">
            {children}
        </header>
    )
}

AppBar.displayName = 'AppBar'

/**
 * Toolbar component - Helper for AppBar content layout
 */
interface ToolbarProps {
    children: ReactNode
    className?: string
    variant?: 'dense' | 'regular'
}

export const Toolbar = ({children, className = '', variant = 'regular'}: ToolbarProps) => {
    const heightClass = {
        'dense': 'h-12',
        'regular': 'h-16',
    }[variant]

    return (
        <div
            className={clsx(
                'flex items-center justify-between px-4 sm:px-6 lg:px-8',
                heightClass,
                className
            )}
            role="toolbar"
        >
            {children}
        </div>
    )
}

Toolbar.displayName = 'Toolbar'
