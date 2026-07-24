import type {ReactNode} from 'react'
import clsx from 'clsx'

interface ContainerProps {
    children: ReactNode
    className?: string
    maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full'
    disableGutters?: boolean
    component?: 'div' | 'section' | 'article' | 'main'
}

const maxWidthMap = {
    'sm': 'max-w-sm',
    'md': 'max-w-md',
    'lg': 'max-w-lg',
    'xl': 'max-w-xl',
    '2xl': 'max-w-2xl',
    'full': 'max-w-full',
}

/**
 * Container component - Replaces MUI Container
 * A centered, max-width constrained container
 */
export const Container = ({
                              children,
                              className = '',
                              maxWidth = 'lg',
                              disableGutters = false,
                              component: Component = 'div',
                          }: ContainerProps) => {
    const classes = clsx(
        'w-full mx-auto',
        !disableGutters && 'px-4 sm:px-6 lg:px-8',
        maxWidthMap[maxWidth],
        className
    )

    return (
        <Component className={classes}>
            {children}
        </Component>
    )
}

Container.displayName = 'Container'
