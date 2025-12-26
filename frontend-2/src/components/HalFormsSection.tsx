/**
 * Reusable component for displaying and managing HAL form templates
 * Shows available forms and handles form selection and submission
 */

import {type ReactElement} from 'react';
import {Alert, Button, Spinner} from './UI';
import {HalFormsForm} from './HalFormsForm';
import {isFormValidationError} from '../api/hateoas';
import type {HalFormsTemplate} from '../api';
import {UI_MESSAGES} from '../constants/messages';
import {klabisFieldsFactory} from "./KlabisFieldsFactory.tsx";
import {useHalFormData} from '../hooks/useHalFormData';
import {useHalRoute} from '../contexts/HalRouteContext';
import {HalFormTemplateButton} from './HalFormTemplateButton';

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
				<HalFormTemplateButton
					key={templateName}
					template={template}
					templateName={templateName}
					onClick={() => onSelectTemplate(template)}
				/>
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
	const {pathname} = useHalRoute();
	const {formData, isLoadingTargetData, targetFetchError, refetchTargetData} = useHalFormData(
		selectedTemplate,
		data,
		pathname
	);

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

			{/* Loading state while fetching target data */}
			{isLoadingTargetData && (
				<div className="flex items-center gap-2 p-4 bg-surface-raised rounded">
					<Spinner size="sm"/>
					<span>{UI_MESSAGES.LOADING_FORM_DATA}</span>
				</div>
			)}

			{/* Error state when target fetch fails */}
			{targetFetchError && (
				<Alert severity="error">
					<div className="space-y-2">
						<p>{UI_MESSAGES.FORM_DATA_LOAD_ERROR}</p>
						{selectedTemplate.target && (
							<p className="text-sm text-text-secondary">
								Endpoint: {selectedTemplate.target}
							</p>
						)}
						<p className="text-sm text-text-secondary">{targetFetchError.message}</p>
						<div className="flex gap-2">
							<Button size="sm" onClick={refetchTargetData}>
								{UI_MESSAGES.RETRY}
							</Button>
							<Button size="sm" variant="secondary" onClick={() => onSelectTemplate(null)}>
								{UI_MESSAGES.CANCEL}
							</Button>
						</div>
					</div>
				</Alert>
			)}

			{/* Form submission error (different from target fetch error) */}
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

			{/* Only show form when data is ready and no target fetch error */}
			{!isLoadingTargetData && !targetFetchError && formData && (
				<HalFormsForm
					data={formData}
					template={selectedTemplate}
					onSubmit={onSubmit}
					onCancel={() => onSelectTemplate(null)}
					isSubmitting={isSubmitting}
					fieldsFactory={klabisFieldsFactory}
				/>
			)}
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
		<div className="mt-4 p-4 border border-border rounded bg-surface-raised">
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
