import { useState, useEffect, type FormEvent } from 'react';
import { Button, Card, Alert } from '../UI';
import { PasswordField } from './PasswordField';
import { PasswordStrengthIndicator, type PasswordRequirement } from './PasswordStrengthIndicator';
import {
    completePasswordSetup,
    PasswordSetupError,
    type SetPasswordRequest,
    type PasswordSetupResponse,
} from '../../api/passwordSetup';
import {labels} from '../../localization';

interface PasswordSetupFormProps {
    token: string;
    onSuccess: (registrationNumber: string) => void;
}

interface FormData {
    password: string;
    passwordConfirmation: string;
}

interface ValidationErrors {
    password?: string;
    passwordConfirmation?: string;
}

/**
 * PasswordSetupForm - Form for setting up user password
 * Validates password complexity and submits to backend
 */
export const PasswordSetupForm = ({ token, onSuccess }: PasswordSetupFormProps) => {
    const [formData, setFormData] = useState<FormData>({
        password: '',
        passwordConfirmation: '',
    });
    const [errors, setErrors] = useState<ValidationErrors>({});
    const [serverError, setServerError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showSuccess, setShowSuccess] = useState(false);

    // Password requirements from spec
    const [requirements, setRequirements] = useState<PasswordRequirement[]>([
        { id: 'length', label: 'Minimálně 12 znaků', met: false },
        { id: 'uppercase', label: 'Alespoň 1 velké písmeno', met: false },
        { id: 'lowercase', label: 'Alespoň 1 malé písmeno', met: false },
        { id: 'digit', label: 'Alespoň 1 číslo', met: false },
        { id: 'special', label: 'Alespoň 1 speciální znak', met: false },
    ]);

    // Update requirements when password changes
    useEffect(() => {
        const password = formData.password;
        setRequirements([
            { id: 'length', label: 'Minimálně 12 znaků', met: password.length >= 12 },
            { id: 'uppercase', label: 'Alespoň 1 velké písmeno', met: /[A-Z]/.test(password) },
            { id: 'lowercase', label: 'Alespoň 1 malé písmeno', met: /[a-z]/.test(password) },
            { id: 'digit', label: 'Alespoň 1 číslo', met: /\d/.test(password) },
            { id: 'special', label: 'Alespoň 1 speciální znak', met: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password) },
        ]);
    }, [formData.password]);

    const validateForm = (): boolean => {
        const newErrors: ValidationErrors = {};

        // Check if password meets all requirements
        const allMet = requirements.every((req) => req.met);
        if (!allMet) {
            newErrors.password = labels.validation.passwordRequirements;
        }

        // Check password confirmation
        if (formData.password !== formData.passwordConfirmation) {
            newErrors.passwordConfirmation = labels.validation.passwordsMismatch;
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setServerError(null);

        if (!validateForm()) {
            return;
        }

        setIsSubmitting(true);

        try {
            const request: SetPasswordRequest = {
                token,
                password: formData.password,
                passwordConfirmation: formData.passwordConfirmation,
            };

            const response: PasswordSetupResponse = await completePasswordSetup(request);
            setShowSuccess(true);
            setTimeout(() => {
                onSuccess(response.registrationNumber || '');
            }, 2000);
        } catch (error) {
            if (error instanceof PasswordSetupError) {
                const errorMessage = error.getErrorMessage();
                if (error.status === 400) {
                    setServerError(errorMessage);
                } else if (error.status === 410) {
                    setServerError(labels.errors.tokenExpired);
                } else if (error.status === 404) {
                    setServerError(labels.errors.tokenInvalid);
                } else {
                    setServerError(errorMessage);
                }
            } else {
                setServerError(labels.errors.unexpectedError);
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    const updateField = (field: keyof FormData, value: string) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
        // Clear error for this field when user starts typing
        if (errors[field]) {
            setErrors((prev) => ({ ...prev, [field]: undefined }));
        }

        // Update requirements when password changes
        if (field === 'password') {
            const newRequirements: PasswordRequirement[] = [
                { id: 'length', label: 'Minimálně 12 znaků', met: value.length >= 12 },
                { id: 'uppercase', label: 'Alespoň 1 velké písmeno', met: /[A-Z]/.test(value) },
                { id: 'lowercase', label: 'Alespoň 1 malé písmeno', met: /[a-z]/.test(value) },
                { id: 'digit', label: 'Alespoň 1 číslo', met: /\d/.test(value) },
                { id: 'special', label: 'Alespoň 1 speciální znak', met: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value) },
            ];
            setRequirements(newRequirements);
        }
    };

    if (showSuccess) {
        return (
            <Card className="p-8 text-center">
                <div className="mb-4 flex justify-center">
                    <div className="w-16 h-16 bg-success/20 rounded-full flex items-center justify-center">
                        <svg
                            className="w-8 h-8 text-success"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M5 13l4 4L19 7"
                            />
                        </svg>
                    </div>
                </div>
                <h2 className="text-2xl font-semibold text-text-primary mb-2">
                    {labels.ui.passwordSetSuccess}
                </h2>
                <p className="text-text-secondary">
                    {labels.ui.passwordSetSuccessDescription}
                </p>
            </Card>
        );
    }

    const allRequirementsMet = requirements.every((req) => req.met);
    const passwordsMatch = formData.password === formData.passwordConfirmation && formData.passwordConfirmation.length > 0;

    return (
        <Card className="p-8 max-w-md mx-auto">
            <div className="mb-6">
                <h2 className="text-2xl font-semibold text-text-primary mb-2">
                    Nastavení hesla
                </h2>
                <p className="text-text-secondary">
                    Zadejte své nové heslo pro aktivaci účtu.
                </p>
            </div>

            {serverError && (
                <Alert severity="error" className="mb-6" onClose={() => setServerError(null)}>
                    {serverError}
                </Alert>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
                <PasswordField
                    name="password"
                    label="Nové heslo"
                    value={formData.password}
                    onChange={(value) => updateField('password', value)}
                    error={errors.password}
                    required
                    placeholder="Zadejte nové heslo"
                    autoComplete="new-password"
                />

                {formData.password && (
                    <PasswordStrengthIndicator requirements={requirements} />
                )}

                <PasswordField
                    name="passwordConfirmation"
                    label="Potvrzení hesla"
                    value={formData.passwordConfirmation}
                    onChange={(value) => updateField('passwordConfirmation', value)}
                    error={errors.passwordConfirmation}
                    required
                    placeholder="Zadejte heslo znovu"
                    autoComplete="new-password"
                />

                {passwordsMatch && allRequirementsMet && (
                    <Alert severity="success">
                        ✓ Hesla jsou shodná a splňují všechny požadavky
                    </Alert>
                )}

                <Button
                    type="submit"
                    fullWidth
                    loading={isSubmitting}
                    disabled={!allRequirementsMet || !passwordsMatch}
                >
                    {labels.buttons.setPassword}
                </Button>
            </form>
        </Card>
    );
};

PasswordSetupForm.displayName = 'PasswordSetupForm';
