import {type FormEvent, useMemo, useState} from 'react';
import {Alert, Button, Modal} from '../UI';
import {PasswordField} from './PasswordField';
import {PasswordStrengthIndicator} from './PasswordStrengthIndicator';
import {labels} from '../../localization';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import type {FetchError} from '../../api/authorizedFetch';
import {buildPasswordRequirements} from './passwordRequirements';

interface ChangePasswordDialogProps {
    isOpen: boolean;
    onClose: () => void;
}

interface FormData {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
}

interface ValidationErrors {
    currentPassword?: string;
    newPassword?: string;
    confirmPassword?: string;
}

const INCORRECT_CURRENT_PASSWORD_MARKER = 'Current password is incorrect';

function extractServerErrorMessage(error: Error): string {
    const responseBody = (error as FetchError).responseBody;
    if (responseBody) {
        try {
            const body = JSON.parse(responseBody);
            const detail: string = body?.detail ?? '';
            if (detail.includes(INCORRECT_CURRENT_PASSWORD_MARKER)) {
                return labels.changePassword.errorIncorrectCurrentPassword;
            }
            return detail || labels.errors.unexpectedError;
        } catch {
            // fall through
        }
    }
    return labels.errors.unexpectedError;
}

export const ChangePasswordDialog = ({isOpen, onClose}: ChangePasswordDialogProps) => {
    const [formData, setFormData] = useState<FormData>({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
    });
    const [errors, setErrors] = useState<ValidationErrors>({});
    const [serverError, setServerError] = useState<string | null>(null);

    const requirements = useMemo(() => buildPasswordRequirements(formData.newPassword), [formData.newPassword]);

    const {mutate, isPending} = useAuthorizedMutation({method: 'POST'});

    const handleClose = () => {
        setFormData({currentPassword: '', newPassword: '', confirmPassword: ''});
        setErrors({});
        setServerError(null);
        onClose();
    };

    const updateField = (field: keyof FormData, value: string) => {
        setFormData(prev => ({...prev, [field]: value}));
        if (errors[field]) {
            setErrors(prev => ({...prev, [field]: undefined}));
        }
        if (field === 'currentPassword') {
            setServerError(null);
        }
    };

    const validateForm = (): boolean => {
        const newErrors: ValidationErrors = {};

        if (!formData.currentPassword) {
            newErrors.currentPassword = labels.validation.requiredField;
        }

        const allMet = requirements.every(r => r.met);
        if (!allMet) {
            newErrors.newPassword = labels.validation.passwordRequirements;
        }

        if (formData.newPassword !== formData.confirmPassword) {
            newErrors.confirmPassword = labels.validation.passwordsMismatch;
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        if (!validateForm()) return;

        mutate(
            {
                url: '/api/me/password-change',
                data: {
                    currentPassword: formData.currentPassword,
                    newPassword: formData.newPassword,
                },
            },
            {
                onSuccess: () => handleClose(),
                onError: (error) => {
                    setServerError(extractServerErrorMessage(error));
                },
            },
        );
    };

    const l = labels.changePassword;

    return (
        <Modal
            isOpen={isOpen}
            onClose={handleClose}
            title={l.dialogTitle}
            size="md"
            footer={
                <>
                    <Button variant="secondary" onClick={handleClose} disabled={isPending}>
                        {labels.buttons.cancel}
                    </Button>
                    <Button
                        type="submit"
                        form="change-password-form"
                        variant="primary"
                        loading={isPending}
                        disabled={isPending}
                    >
                        {l.submitButton}
                    </Button>
                </>
            }
        >
            {serverError && (
                <Alert severity="error" className="mb-4" onClose={() => setServerError(null)}>
                    {serverError}
                </Alert>
            )}

            <form id="change-password-form" onSubmit={handleSubmit} className="space-y-4">
                <PasswordField
                    name="currentPassword"
                    label={l.currentPasswordLabel}
                    value={formData.currentPassword}
                    onChange={(value) => updateField('currentPassword', value)}
                    error={errors.currentPassword}
                    required
                    autoComplete="current-password"
                />

                <PasswordField
                    name="newPassword"
                    label={l.newPasswordLabel}
                    value={formData.newPassword}
                    onChange={(value) => updateField('newPassword', value)}
                    error={errors.newPassword}
                    required
                    autoComplete="new-password"
                />

                {formData.newPassword && (
                    <PasswordStrengthIndicator requirements={requirements}/>
                )}

                <PasswordField
                    name="confirmPassword"
                    label={l.confirmPasswordLabel}
                    value={formData.confirmPassword}
                    onChange={(value) => updateField('confirmPassword', value)}
                    error={errors.confirmPassword}
                    required
                    autoComplete="new-password"
                />
            </form>
        </Modal>
    );
};

ChangePasswordDialog.displayName = 'ChangePasswordDialog';
