/**
 * Shared presentational component for HAL Forms template button
 * Used by both HalFormButton and HalFormsSection
 */

import type {ReactElement} from 'react';
import type {HalFormsTemplate} from '../../api';
import {Button} from '../UI';

export interface HalFormTemplateButtonProps {
    /** The HAL Forms template */
    template: HalFormsTemplate;

    /** The template name (used as fallback if template.title is not set) */
    templateName: string;

    /** Click handler */
    onClick: () => void;

    /** Optional additional CSS classes */
    className?: string;
}

/**
 * Button component for displaying a HAL Forms template action
 * Pure presentational component with consistent styling
 *
 * @example
 * <HalFormTemplateButton
 *   template={template}
 *   templateName="create"
 *   onClick={() => handleClick()}
 * />
 */
export function HalFormTemplateButton({
                                          template,
                                          templateName,
                                          onClick,
                                          className = '',
                                      }: HalFormTemplateButtonProps): ReactElement {
    const displayText = template.title || templateName;

    return (
        <Button
            onClick={onClick}
            variant="primary"
            size="sm"
            className={className}
            title={displayText}
            aria-label={`Select ${displayText} form`}
            data-testid={`form-template-button-${templateName}`}
        >
            {displayText}
        </Button>
    );
}
