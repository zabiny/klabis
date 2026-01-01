/**
 * Reusable multi-step modal form component for HAL Forms
 *
 * Provides a configurable framework for creating multi-step forms with:
 * - Step-by-step navigation with validation
 * - Progress indicator
 * - Error handling with automatic reset to step 1
 * - Field rendering via HAL Forms context
 *
 * @example
 * ```tsx
 * const formSteps = [
 *   {
 *     title: "Krok 1: Osobní údaje",
 *     fields: ['firstName', 'lastName', 'sex', 'dateOfBirth'],
 *   },
 *   {
 *     title: "Krok 2: Kontakt",
 *     fields: ['address', 'contact'],
 *   },
 *   {
 *     title: "Krok 3: Údaje",
 *     fields: ['siCard', 'bankAccount'],
 *   },
 * ];
 *
 * <MultiStepFormModal
 *   steps={formSteps}
 *   onCancel={handleCancel}
 * />
 * ```
 */

import {useContext, useEffect, useRef, useState} from 'react';
import {useFormikContext} from 'formik';
import {HalFormsFormContext} from './halforms';
import {Button} from '../UI';

/**
 * Definition for a single step in the multi-step form
 */
export interface FormStep {
    /** Display title for this step */
    title: string;
    /** Field names to render in this step */
    fields: string[];
}

/**
 * Props for MultiStepFormModal component
 */
export interface MultiStepFormModalProps {
    /** Array of step definitions */
    steps: FormStep[];
    /** Text for next button (default: "Další") */
    nextButtonLabel?: string;
    /** Text for back button (default: "Zpět") */
    backButtonLabel?: string;
    /** Text for submit button (default: "Odeslat") */
    submitButtonLabel?: string;
    /** Show step numbers in title (default: true) */
    showStepNumbers?: boolean;
}

/**
 * Multi-step form modal component
 *
 * Manages form navigation across multiple steps with validation.
 * Automatically resets to step 1 if form submission fails.
 *
 * Must be used within HalFormsForm component that provides context.
 */
export const MultiStepFormModal = ({
                                       steps,
                                       nextButtonLabel = 'Další',
                                       backButtonLabel = 'Zpět',
                                       submitButtonLabel = 'Odeslat',
                                       showStepNumbers = true,
                                   }: MultiStepFormModalProps) => {
    const contextValue = useContext(HalFormsFormContext);
    const renderField = contextValue?.renderField || (() => null);
    const formik = useFormikContext();
    const [currentStep, setCurrentStep] = useState(0);
    const submissionAttemptedRef = useRef(false);

    // Monitor for submission errors and reset to step 1
    useEffect(() => {
        if (submissionAttemptedRef.current && !formik.isSubmitting) {
            const hasErrors = Object.keys(formik.errors).length > 0;
            if (hasErrors) {
                // Reset to first step so user can review values
                setCurrentStep(0);
                submissionAttemptedRef.current = false;
            } else {
                // Form was submitted successfully, modal will close
                submissionAttemptedRef.current = false;
            }
        }
    }, [formik.isSubmitting, formik.errors]);

    /**
     * Validate current step and mark fields as touched
     */
    const validateStep = async (stepIndex: number): Promise<boolean> => {
        const step = steps[stepIndex];
        if (!step) return false;

        // Mark all fields in this step as touched to show validation errors
        for (const field of step.fields) {
            await formik.setFieldTouched(field, true);
        }

        // Validate the entire form to populate errors
        const errors = (await formik.validateForm()) as Record<string, any>;

        // Check if any of the fields for this step have errors
        const hasErrors = step.fields.some(field => errors[field]);
        return !hasErrors;
    };

    const handleNext = async () => {
        if (await validateStep(currentStep)) {
            setCurrentStep(currentStep + 1);
        }
    };

    const handleBack = () => {
        setCurrentStep(currentStep - 1);
    };

    const handleSubmit = async () => {
        if (await validateStep(currentStep)) {
            submissionAttemptedRef.current = true;
            formik.submitForm();
        }
    };

    const isFirstStep = currentStep === 0;
    const isLastStep = currentStep === steps.length - 1;
    const currentStepData = steps[currentStep];

    if (!currentStepData) {
        return null;
    }

    const stepTitle = showStepNumbers
        ? `${currentStepData.title.replace(/^Krok \d+:\s*/, '')} (${currentStep + 1}/${steps.length})`
        : currentStepData.title;

    return (
        <div className="space-y-6">
            {/* Step title */}
            <h3 className="text-lg font-medium text-gray-700">{stepTitle}</h3>

            {/* Step fields */}
            <div className="space-y-4">
                {currentStepData.fields.map((fieldName) => (
                    <div key={fieldName}>
                        {renderField(fieldName)}
                    </div>
                ))}
            </div>

            {/* Step indicator and buttons */}
            <div className="space-y-4 pt-4 border-t border-gray-200">
                {/* Progress bar */}
                <div className="flex gap-2">
                    {steps.map((_, stepIndex) => (
                        <div
                            key={stepIndex}
                            className={`flex-1 h-1 rounded ${
                                stepIndex <= currentStep ? 'bg-primary' : 'bg-gray-300'
                            }`}
                        />
                    ))}
                </div>

                {/* Navigation buttons */}
                <div className="flex gap-3 justify-between">
                    <div className="flex gap-3">
                        {!isFirstStep && (
                            <Button
                                type="button"
                                variant="secondary"
                                onClick={handleBack}
                                disabled={formik.isSubmitting}
                            >
                                {backButtonLabel}
                            </Button>
                        )}
                        {!isLastStep && (
                            <Button
                                type="button"
                                variant="primary"
                                onClick={handleNext}
                                disabled={formik.isSubmitting}
                            >
                                {nextButtonLabel}
                            </Button>
                        )}
                    </div>
                    <div className="flex gap-3">
                        {isLastStep && (
                            <Button
                                type="button"
                                variant="primary"
                                onClick={handleSubmit}
                                loading={formik.isSubmitting}
                                disabled={formik.isSubmitting}
                            >
                                {submitButtonLabel}
                            </Button>
                        )}
                        {renderField('cancel')}
                    </div>
                </div>
            </div>
        </div>
    );
};
