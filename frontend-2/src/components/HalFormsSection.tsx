/**
 * Reusable component for displaying and managing HAL form templates
 * Shows available forms and handles form selection and submission
 */

import {type ReactElement} from 'react';
import {Alert, Button} from './UI';
import {HalFormsForm} from './HalFormsForm';
import {isFormValidationError} from '../api/hateoas';
import type {HalFormsTemplate} from '../api';
import {UI_MESSAGES} from '../constants/messages';
import {klabisFieldsFactory} from "./KlabisFieldsFactory.tsx";

/**
 * Form state - tracks which template is selected and submission status
 */
export interface HalFormsState {
	selectedTemplate: HalFormsTemplate | null;
	submitError: Error | null;
	isSubmitting: boolean;
}

/**
 * Form handlers - callbacks for form interactions
 */
export interface HalFormsHandlers {
	onSelectTemplate: (template: HalFormsTemplate | null) => void;
	onSubmit: (formData: Record<string, unknown>) => Promise<void>;
}

/**
 * Props for HalFormsSection component
 * Grouped into data props, state props (via formState), and handler props (via handlers)
 */
interface HalFormsSectionProps {
	templates?: Record<string, HalFormsTemplate>;
	data: Record<string, unknown>;
	formState: HalFormsState;
	handlers: HalFormsHandlers;
}

/**
 * Template selector - displays available form templates as buttons
 */
interface HalFormsTemplateSelectorProps {
	templates: Record<string, HalFormsTemplate>;
	onSelectTemplate: (template: HalFormsTemplate) => void;
}

const HalFormsTemplateSelector = ({
	templates,
	onSelectTemplate,
								  }: HalFormsTemplateSelectorProps): ReactElement => {
	return (
		<div className="flex flex-wrap gap-2">
			{Object.entries(templates).map(([templateName, template]) => (
				<button
					key={templateName}
					onClick={() => onSelectTemplate(template)}
					className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 text-sm border-none cursor-pointer"
					title={template.title || templateName}
					aria-label={`Select ${template.title || templateName} form`}
					data-testid={`form-template-button-${templateName}`}
				>
					{template.title || templateName}
				</button>
			))}
		</div>
	);
};

/**
 * Form display - shows the selected form with error handling and submission controls
 */
interface HalFormsDisplayProps {
	selectedTemplate: HalFormsTemplate;
	data: Record<string, unknown>;
	formState: HalFormsState;
	handlers: HalFormsHandlers;
}

const HalFormsDisplay = ({
							 selectedTemplate,
							 data,
							 formState,
							 handlers,
						 }: HalFormsDisplayProps): ReactElement => {
	const {submitError, isSubmitting} = formState;
	const {onSelectTemplate, onSubmit} = handlers;

	return (
		<div className="space-y-4">
			<div className="flex items-center justify-between mb-4">
				<h4 className="font-semibold">{selectedTemplate.title || UI_MESSAGES.FORM}</h4>
				<Button
					onClick={() => onSelectTemplate(null)}
					variant="secondary"
					size="sm"
					data-testid="close-form-button"
					aria-label="Close form"
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
				fieldsFactory={klabisFieldsFactory}
			/>
		</div>
	);
};

/**
 * Component to display available form templates from a HAL resource
 * Shows form buttons or the form itself when selected
 *
 * @example
 * <HalFormsSection
 *   templates={data._templates}
 *   data={data}
 *   formState={{selectedTemplate, submitError, isSubmitting}}
 *   handlers={{onSelectTemplate: setSelectedTemplate, onSubmit: handleFormSubmit}}
 * />
 */
export function HalFormsSection({
									templates,
									data,
									formState,
									handlers,
								}: HalFormsSectionProps): ReactElement | null {
	if (!templates || Object.keys(templates).length === 0) {
		return null;
	}

	const {selectedTemplate} = formState;

	return (
		<div className="mt-4 p-4 border rounded bg-green-50 dark:bg-green-900">
			<h3 className="font-semibold mb-2">{UI_MESSAGES.AVAILABLE_FORMS}</h3>
			{selectedTemplate ? (
				<HalFormsDisplay
					selectedTemplate={selectedTemplate}
					data={data}
					formState={formState}
					handlers={handlers}
				/>
			) : (
				<HalFormsTemplateSelector
					templates={templates}
					onSelectTemplate={handlers.onSelectTemplate}
				/>
			)}
		</div>
	);
}
