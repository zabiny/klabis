/**
 * Shared presentational component for HAL Forms template button
 * Used by both HalFormButton and HalFormsSection
 */

import type {ReactElement, ReactNode} from 'react';
import type {HalFormsTemplate} from '../../api';
import {Button} from '../UI';
import {getTemplateLabel} from '../../localization';

export interface HalFormTemplateButtonProps {
    /** The HAL Forms template */
    template: HalFormsTemplate;

    /** The template name (used as fallback if template.title is not set) */
    templateName: string;

    /** Click handler */
    onClick: () => void;

    /** Optional explicit label — overrides template.title and templateName */
    label?: string;

    /** Optional additional CSS classes */
    className?: string;

    /** Optional button variant — defaults to 'primary' */
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost';

    /** Optional icon rendered before the label */
    icon?: ReactNode;
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
                                          label,
                                          className = '',
                                          variant = 'primary',
                                          icon,
                                      }: HalFormTemplateButtonProps): ReactElement {
    const displayText = label || getTemplateLabel(templateName) || template.title || templateName;

    return (
        <Button
            onClick={onClick}
            variant={variant}
            size="sm"
            className={className}
            title={displayText}
            aria-label={`Select ${displayText} form`}
            data-testid={`form-template-button-${templateName}`}
        >
            {icon && <span className="inline-flex">{icon}</span>}
            {displayText}
        </Button>
    );
}
