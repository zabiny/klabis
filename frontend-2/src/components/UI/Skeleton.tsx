import type {ReactNode} from 'react'

interface SkeletonProps {
    width?: string | number
    height?: string | number
    count?: number
    circle?: boolean
    className?: string
    children?: ReactNode
}

/**
 * Skeleton - Loading placeholder component
 * Displays animated skeleton while content is loading
 */
export const Skeleton = ({
                             width = '100%',
                             height = '1rem',
                             count = 1,
                             circle = false,
                             className = '',
                             children
                         }: SkeletonProps) => {
    const widthStyle = typeof width === 'number' ? `${width}px` : width
    const heightStyle = typeof height === 'number' ? `${height}px` : height

    const circleClass = circle ? 'rounded-full' : 'rounded'

    if (children) {
        return (
            <div className={`space-y-2 ${className}`}>
                {children}
            </div>
        )
    }

    return (
        <div className={`space-y-2 ${className}`}>
            {Array.from({length: count}).map((_, i) => (
                <div
                    key={i}
                    style={{
                        width: widthStyle,
                        height: heightStyle
                    }}
                    className={`bg-gray-200 dark:bg-gray-700 animate-pulse ${circleClass}`}
                />
            ))}
        </div>
    )
}

Skeleton.displayName = 'Skeleton'
