import {ArrowLeftOnRectangleIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
}

/**
 * LogoutIcon - Replaces MUI LogoutIcon
 */
export const LogoutIcon = ({size = 24, className = '', title}: IconProps) => (
    <ArrowLeftOnRectangleIcon
        width={size}
        height={size}
        className={className}
        title={title || 'Logout'}
    />
)

LogoutIcon.displayName = 'LogoutIcon'
