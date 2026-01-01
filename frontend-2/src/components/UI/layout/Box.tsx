import type {ReactNode} from 'react'
import clsx from 'clsx'

interface BoxProps {
    children: ReactNode
    className?: string
    as?: 'div' | 'section' | 'article' | 'aside' | 'header' | 'footer' | 'main'
    display?: 'flex' | 'grid' | 'block' | 'inline' | 'inline-block'
    p?: number | string // padding
    px?: number | string // padding-x
    py?: number | string // padding-y
    m?: number | string // margin
    mx?: number | string // margin-x
    my?: number | string // margin-y
    gap?: number | string
    flex?: 'row' | 'col'
    flexGrow?: boolean
    flexShrink?: boolean
    justifyContent?: 'start' | 'center' | 'end' | 'between' | 'around' | 'evenly'
    alignItems?: 'start' | 'center' | 'end' | 'stretch'
    bgcolor?: string
    width?: string
    height?: string
    minHeight?: string
    maxWidth?: string
    rounded?: boolean | 'sm' | 'base' | 'md' | 'lg'
    shadow?: boolean | 'sm' | 'md' | 'lg'
    border?: boolean
    borderColor?: string
    onClick?: () => void
    role?: string
    ariaLabel?: string
    id?: string

    [key: string]: any
}

const getSpacingClass = (value?: number | string): string => {
    if (value === undefined) return ''
    if (typeof value === 'number') {
        const multiplier = Math.ceil(value / 4)
        return `${multiplier}`
    }
    return String(value)
}

/**
 * Box component - Replaces MUI Box
 * A flexible layout primitive that can be styled with Tailwind classes
 */
export const Box = ({
                        children,
                        className = '',
                        as: Component = 'div',
                        display,
                        p,
                        px,
                        py,
                        m,
                        mx,
                        my,
                        gap,
                        flex,
                        flexGrow,
                        flexShrink,
                        justifyContent,
                        alignItems,
                        bgcolor,
                        width = 'auto',
                        height,
                        minHeight,
                        maxWidth,
                        rounded,
                        shadow,
                        border,
                        borderColor,
                        onClick,
                        role,
                        ariaLabel,
                        id,
                        ...rest
                    }: BoxProps) => {
    const displayClass = {
        'flex': 'flex',
        'grid': 'grid',
        'block': 'block',
        'inline': 'inline',
        'inline-block': 'inline-block',
    }[display as keyof typeof displayMap] || 'block'

    const displayMap = {
        'flex': 'flex',
        'grid': 'grid',
        'block': 'block',
        'inline': 'inline',
        'inline-block': 'inline-block'
    }

    const flexDirClass = flex === 'col' ? 'flex-col' : flex === 'row' ? 'flex-row' : ''

    const justifyMap = {
        'start': 'justify-start',
        'center': 'justify-center',
        'end': 'justify-end',
        'between': 'justify-between',
        'around': 'justify-around',
        'evenly': 'justify-evenly',
    }

    const justifyClass = justifyContent && justifyContent in justifyMap ? justifyMap[justifyContent as keyof typeof justifyMap] : ''

    const alignMap = {
        'start': 'items-start',
        'center': 'items-center',
        'end': 'items-end',
        'stretch': 'items-stretch',
    }

    const alignClass = alignItems && alignItems in alignMap ? alignMap[alignItems as keyof typeof alignMap] : ''

    const roundedMap = {
        'true': 'rounded-lg',
        'sm': 'rounded-sm',
        'base': 'rounded-base',
        'md': 'rounded-md',
        'lg': 'rounded-lg',
    }

    const roundedClass = rounded ? (rounded === true ? 'rounded-lg' : roundedMap[rounded as keyof typeof roundedMap] || '') : ''

    const shadowMap = {
        'true': 'shadow-md',
        'sm': 'shadow-sm',
        'md': 'shadow-md',
        'lg': 'shadow-lg',
    }

    const shadowClass = shadow ? (shadow === true ? 'shadow-md' : shadowMap[shadow as keyof typeof shadowMap] || '') : ''

    const classes = clsx(
        displayClass,
        display === 'flex' && flexDirClass,
        display === 'flex' && gap && `gap-${getSpacingClass(gap)}`,
        display === 'flex' && flexGrow && 'flex-grow',
        display === 'flex' && flexShrink && 'flex-shrink',
        display === 'flex' && justifyClass,
        display === 'flex' && alignClass,
        p && `p-${getSpacingClass(p)}`,
        px && `px-${getSpacingClass(px)}`,
        py && `py-${getSpacingClass(py)}`,
        m && `m-${getSpacingClass(m)}`,
        mx && `mx-${getSpacingClass(mx)}`,
        my && `my-${getSpacingClass(my)}`,
        bgcolor,
        width !== 'auto' && (width.includes('w-') ? width : `w-${width}`),
        height && `h-${getSpacingClass(height)}`,
        minHeight && `min-h-${getSpacingClass(minHeight)}`,
        maxWidth && `max-w-${getSpacingClass(maxWidth)}`,
        roundedClass,
        shadowClass,
        border && 'border border-gray-300 dark:border-gray-600',
        borderColor,
        className
    )

    return (
        <Component
            className={classes}
            onClick={onClick}
            role={role}
            aria-label={ariaLabel}
            id={id}
            {...rest}
        >
            {children}
        </Component>
    )
}

Box.displayName = 'Box'
