import type {ReactElement} from 'react'
import {labels} from '../../../../localization'

interface ClearSelectionButtonProps {
    onClick: () => void;
    className?: string;
}

export const ClearSelectionButton = ({onClick, className = 'right-20'}: ClearSelectionButtonProps): ReactElement => (
    <button
        type="button"
        onClick={onClick}
        className={`absolute ${className} top-1/2 transform -translate-y-1/2 text-text-secondary hover:text-text-primary transition-colors`}
        title={labels.ui.clearSelection}
        aria-label={labels.ui.clearSelection}
        data-testid="clear-member-button"
    >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12"/>
        </svg>
    </button>
)
