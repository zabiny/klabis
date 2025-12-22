/**
 * Reusable component for displaying and managing HAL form templates
 * Shows available forms and handles form selection and submission
 */

import {type ReactElement} from 'react';
import {Alert, Button} from './UI';
import {halFormsFieldsFactory, HalFormsForm} from './HalFormsForm';
import {isFormValidationError} from '../api/hateoas';
import type {HalFormsTemplate} from '../api';
import {UI_MESSAGES} from '../constants/messages';

interface HalFormsSectionProps {
	templates?: Record<string, HalFormsTemplate>;
	data: Record<string, unknown>;
	selectedTemplate: HalFormsTemplate | null;
	onSelectTemplate: (template: HalFormsTemplate) => void;
	onSubmit: (formData: Record<string, unknown>) => Promise<void>;
	submitError: Error | null;
	isSubmitting: boolean;
}

/**
 * Component to display available form templates from a HAL resource
 * Shows form buttons or the form itself when selected
 */
export function HalFormsSection({
	templates,
	data,
	selectedTemplate,
	onSelectTemplate,
	onSubmit,
	submitError,
	isSubmitting,
}: HalFormsSectionProps): ReactElement | null {
	if (!templates || Object.keys(templates).length === 0) {
		return null;
	}

	return (
		<div className="mt-4 p-4 border rounded bg-green-50 dark:bg-green-900">
			<h3 className="font-semibold mb-2">{UI_MESSAGES.AVAILABLE_FORMS}</h3>
			{selectedTemplate ? (
				<div className="space-y-4">
					<div className="flex items-center justify-between mb-4">
						<h4 className="font-semibold">{selectedTemplate.title || UI_MESSAGES.FORM}</h4>
						<Button
							onClick={() => onSelectTemplate(null)}
							variant="secondary"
							size="sm"
						>
							{UI_MESSAGES.CLOSE}
						</Button>
					</div>

					{submitError && (
						<Alert severity="error">
							<div className="space-y-1">
								<p>{submitError.message}</p>
								{isFormValidationError(submitError) && (
									<ul className="list-disc list-inside text-sm">
										{Object.entries(submitError.validationErrors).map(([field, error]) => (
											<li key={field}>{field}: {error}</li>
										))}
									</ul>
								)}
							</div>
						</Alert>
					)}

					<HalFormsForm
						data={data}
						template={selectedTemplate}
						onSubmit={onSubmit}
						onCancel={() => onSelectTemplate(null)}
						isSubmitting={isSubmitting}
						fieldsFactory={halFormsFieldsFactory}
					/>
				</div>
			) : (
				<div className="flex flex-wrap gap-2">
					{Object.entries(templates).map(([templateName, template]) => (
						<button
							key={templateName}
							onClick={() => onSelectTemplate(template)}
							className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 text-sm border-none cursor-pointer"
							title={template.title || templateName}
						>
							{template.title || templateName}
						</button>
					))}
				</div>
			)}
		</div>
	);
}
