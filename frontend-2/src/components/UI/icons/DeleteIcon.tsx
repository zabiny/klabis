import {TrashIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
}

/**
 * DeleteIcon - Replaces MUI Delete icon
 */
export const DeleteIcon = ({size = 24, className = '', title}: IconProps) => (
    <TrashIcon
        width={size}
        height={size}
        className={className}
        title={title || 'Delete'}
    />
)

DeleteIcon.displayName = 'DeleteIcon'
