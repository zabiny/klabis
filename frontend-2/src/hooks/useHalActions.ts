/**
 * Custom hook for HAL resource actions
 * Handles common navigation, form selection, and form submission logic
 */

import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useHalRoute} from '../contexts/HalRouteContext';
import type {HalFormsTemplate, TemplateTarget} from '../api';
import {submitHalFormsData} from '../api/hateoas';
import {extractNavigationPath} from '../utils/navigationPath';

interface UseHalActionsReturn {
	selectedTemplate: HalFormsTemplate | null;
	setSelectedTemplate: (template: HalFormsTemplate | null) => void;
	submitError: Error | null;
	setSubmitError: (error: Error | null) => void;
	isSubmitting: boolean;
	handleNavigateToItem: (href: string) => void;
	handleFormSubmit: (formData: Record<string, unknown>) => Promise<void>;
}

/**
 * Hook to manage HAL resource actions (navigation, forms, etc.)
 * Shared between collection and item display components
 */
export function useHalActions(): UseHalActionsReturn {
	const navigate = useNavigate();
	const {pathname, refetch} = useHalRoute();
	const [selectedTemplate, setSelectedTemplate] = useState<HalFormsTemplate | null>(null);
	const [submitError, setSubmitError] = useState<Error | null>(null);
	const [isSubmitting, setIsSubmitting] = useState(false);

	const handleNavigateToItem = (href: string) => {
		const path = extractNavigationPath(href);
		navigate(path);
	};

	const handleFormSubmit = async (formData: Record<string, unknown>) => {
		if (!selectedTemplate) {
			throw new Error("Can't submit form - template is missing!")
		}

		setIsSubmitting(true);
		setSubmitError(null);
		console.log("Submitting form data " + JSON.stringify(formData));
		try {
			const submitTarget: TemplateTarget = {
				target: selectedTemplate.target || '/api' + pathname,
				method: selectedTemplate.method || 'POST'
			}

			await submitHalFormsData(submitTarget, formData);
			// Refetch data after successful submission
			await refetch();
			// Close the form
			setSelectedTemplate(null);
		} catch (err) {
			setSubmitError(err instanceof Error ? err : new Error('Failed to submit form'));
		} finally {
			setIsSubmitting(false);
		}
	};

	return {
		selectedTemplate,
		setSelectedTemplate,
		submitError,
		setSubmitError,
		isSubmitting,
		handleNavigateToItem,
		handleFormSubmit,
	};
}
