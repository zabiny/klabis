import {SunIcon as HeroSunIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
}

/**
 * SunIcon - Light mode indicator
 */
export const SunIcon = ({size = 24, className = '', title}: IconProps) => (
    <HeroSunIcon
        width={size}
        height={size}
        className={className}
        title={title || 'Light mode'}
    />
)

SunIcon.displayName = 'SunIcon'
