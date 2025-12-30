/**
 * Reusable component for displaying HAL form templates as modal buttons
 * Shows available forms using HalFormButton components with modal overlays
 * Automatically uses resourceData._templates if templates prop is not provided
 */

import {type ReactElement, type ReactNode} from 'react';
import type {HalFormsTemplate} from '../../api';
import {useHalRoute} from '../../contexts/HalRouteContext';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {HalFormButton} from './HalFormButton.tsx';
import {formsSectionStyles} from '../../theme/designTokens';
import type {RenderFormCallback} from '../HalFormsForm';

/**
 * Props for HalFormsSection component
 */
interface HalFormsSectionProps {
	/** Templates object from HAL resource. If not provided, uses resourceData._templates */
	templates?: Record<string, HalFormsTemplate>;
	/** Whether to open forms in modal (default: true). If false, displays inline */
	modal?: boolean;
	/** Optional custom layouts per template name */
	customLayouts?: Record<string, ReactNode | RenderFormCallback>;
}

/**
 * Component to display available form templates from a HAL resource
 * Shows all available templates as buttons that open forms in modal overlays or inline
 *
 * If `templates` is not provided, automatically fetches from useHalRoute()._templates
 *
 * @example
 * // Automatic - uses resourceData._templates
 * <HalFormsSection />
 *
 * @example
 * // Manual - provides explicit templates
 * <HalFormsSection templates={customTemplates} modal={false} />
 */
export function HalFormsSection({
									templates: propsTemplates,
									modal = true,
									customLayouts,
								}: HalFormsSectionProps): ReactElement | null {
	const {resourceData} = useHalRoute();

	// Use provided templates or fallback to resourceData._templates
	const templates = propsTemplates || resourceData?._templates;

	if (!templates || Object.keys(templates).length === 0) {
		return null;
	}

	return (
		<div className={formsSectionStyles.container}>
			<h3 className={formsSectionStyles.heading}>{UI_MESSAGES.AVAILABLE_FORMS}</h3>
			<div className={formsSectionStyles.buttonContainer}>
				{Object.keys(templates).map((templateName) => (
					<HalFormButton
						key={templateName}
						name={templateName}
						modal={modal}
						customLayout={customLayouts?.[templateName]}
					/>
				))}
			</div>
		</div>
	);
}
