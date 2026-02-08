import {ComputerDesktopIcon as HeroComputerDesktopIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
}

/**
 * ComputerDesktopIcon - System preference mode indicator
 */
export const ComputerDesktopIcon = ({size = 24, className = '', title}: IconProps) => (
    <HeroComputerDesktopIcon
        width={size}
        height={size}
        className={className}
        title={title || 'System preference'}
    />
)

ComputerDesktopIcon.displayName = 'ComputerDesktopIcon'
