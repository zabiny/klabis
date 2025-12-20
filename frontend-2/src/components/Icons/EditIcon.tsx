import {PencilSquareIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
}

/**
 * EditIcon - Replaces MUI Edit icon
 */
export const EditIcon = ({size = 24, className = '', title}: IconProps) => (
    <PencilSquareIcon
        width={size}
        height={size}
        className={className}
        title={title || 'Edit'}
    />
)

EditIcon.displayName = 'EditIcon'
