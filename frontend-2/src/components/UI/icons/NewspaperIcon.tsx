import {DocumentDuplicateIcon} from '@heroicons/react/24/outline'

interface IconProps {
    size?: number
    className?: string
    title?: string
}

/**
 * NewspaperIcon - Replaces MUI Newspaper icon
 */
export const NewspaperIcon = ({size = 24, className = '', title}: IconProps) => (
    <DocumentDuplicateIcon
        width={size}
        height={size}
        className={className}
        title={title || 'Newspaper'}
    />
)

NewspaperIcon.displayName = 'NewspaperIcon'
