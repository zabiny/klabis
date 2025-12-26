/**
 * Reusable component for displaying HAL form templates as modal buttons
 * Shows available forms using HalFormButton components with modal overlays
 */

import {type ReactElement} from 'react';
import type {HalFormsTemplate} from '../api';
import {UI_MESSAGES} from '../constants/messages';
import {HalFormButton} from './HalFormButton';

/**
 * Props for HalFormsSection component
 */
interface HalFormsSectionProps {
	templates?: Record<string, HalFormsTemplate>;
	modal?: boolean
}

/**
 * Component to display available form templates from a HAL resource
 * Shows all available templates as buttons that open forms in modal overlays
 *
 * @example
 * <HalFormsSection templates={data._templates} />
 */
export function HalFormsSection({
									templates, modal = true
								}: HalFormsSectionProps): ReactElement | null {
	if (!templates || Object.keys(templates).length === 0) {
		return null;
	}

	return (
		<div className="mt-4 p-4 border border-border rounded bg-surface-raised">
			<h3 className="font-semibold mb-2">{UI_MESSAGES.AVAILABLE_FORMS}</h3>
			<div className="flex flex-wrap gap-2">
				{Object.keys(templates).map((templateName) => (
					<HalFormButton
						key={templateName}
						name={templateName}
						modal={modal}
					/>
				))}
			</div>
		</div>
	);
}
