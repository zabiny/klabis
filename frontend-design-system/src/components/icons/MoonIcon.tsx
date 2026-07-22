import {MoonIcon as HeroMoonIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
}

/**
 * MoonIcon - Dark mode indicator
 */
export const MoonIcon = ({size = 24, className = '', title}: IconProps) => (
    <HeroMoonIcon
        width={size}
        height={size}
        className={className}
        title={title || 'Dark mode'}
    />
)

MoonIcon.displayName = 'MoonIcon'
