import {ArrowPathIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
    animate?: boolean
}

/**
 * SyncIcon - Replaces MUI SyncSharp icon
 */
export const SyncIcon = ({size = 24, className = '', title, animate = false}: IconProps) => (
    <ArrowPathIcon
        width={size}
        height={size}
        className={`${className} ${animate ? 'animate-spin' : ''}`}
        title={title || 'Sync'}
    />
)

SyncIcon.displayName = 'SyncIcon'
