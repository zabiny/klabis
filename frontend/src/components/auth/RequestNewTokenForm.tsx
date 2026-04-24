import {type ChangeEvent, type FormEvent, useState} from 'react';
import {Alert, Button, Card} from '../UI';
import {TextField} from '../UI/forms/TextField';
import {labels} from '../../localization';

interface RequestNewTokenFormProps {
    onSubmit: (data: { registrationNumber: string; email: string }) => void;
    isSubmitting: boolean;
    serverError: string | null;
    onClearServerError: () => void;
    showSuccess: boolean;
}

interface FormData {
    registrationNumber: string;
    email: string;
}

export const RequestNewTokenForm = ({
    onSubmit,
    isSubmitting,
    serverError,
    onClearServerError,
    showSuccess,
}: RequestNewTokenFormProps) => {
    const [formData, setFormData] = useState<FormData>({
        registrationNumber: '',
        email: '',
    });
    const [errors, setErrors] = useState<Partial<Record<keyof FormData, string>>>({});

    const validateForm = (): boolean => {
        const newErrors: Partial<Record<keyof FormData, string>> = {};

        if (!formData.registrationNumber.trim()) {
            newErrors.registrationNumber = labels.validation.registrationNumberRequired;
        }

        if (!formData.email.trim()) {
            newErrors.email = labels.validation.emailRequired;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
            newErrors.email = labels.validation.emailInvalidFormat;
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        onSubmit({
            registrationNumber: formData.registrationNumber.trim(),
            email: formData.email.trim(),
        });
    };

    const updateField = (field: keyof FormData, value: string) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
        if (errors[field]) {
            setErrors((prev) => ({ ...prev, [field]: undefined }));
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
                                d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                            />
                        </svg>
                    </div>
                </div>
                <h2 className="text-2xl font-semibold text-text-primary mb-2">
                    {labels.ui.emailSent}
                </h2>
                <p className="text-text-secondary">
                    {labels.ui.emailSentDescription}
                </p>
            </Card>
        );
    }

    return (
        <Card className="p-8 max-w-md mx-auto">
            <div className="mb-6">
                <h2 className="text-2xl font-semibold text-text-primary mb-2">
                    Požádat o nový token
                </h2>
                <p className="text-text-secondary">
                    Zadejte své registrační číslo a email. Pokud je účet stále čekající na aktivaci,
                    obdržíte nový odkaz pro nastavení hesla.
                </p>
            </div>

            {serverError && (
                <Alert severity="error" className="mb-6" onClose={onClearServerError}>
                    {serverError}
                </Alert>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
                <TextField
                    name="registrationNumber"
                    label={labels.fields.registrationNumber}
                    value={formData.registrationNumber}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => updateField('registrationNumber', e.target.value)}
                    error={errors.registrationNumber}
                    required
                    placeholder="např. 12345678"
                    autoComplete="username"
                />

                <TextField
                    name="email"
                    label={labels.fields.email}
                    type="email"
                    value={formData.email}
                    onChange={(e) => updateField('email', e.target.value)}
                    error={errors.email}
                    required
                    placeholder="vas@email.cz"
                    autoComplete="email"
                />

                <Button
                    type="submit"
                    fullWidth
                    loading={isSubmitting}
                >
                    {labels.buttons.sendRequest}
                </Button>
            </form>
        </Card>
    );
};

RequestNewTokenForm.displayName = 'RequestNewTokenForm';
