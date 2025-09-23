import React from 'react';
import {Field, Form} from '../index.ts';
import {type FieldRenderProps} from "../types";

interface PersonFormData {
    firstName: string;
    lastName: string;
    address: {
        streetAndNumber: string;
        city: string;
        postalCode: string;
        country: string;
    };
    dateOfBirth: string;
    contact?: {
        email?: string;
        phone?: string;
        note?: string;
    };
}

export const ExamplePersonForm: React.FC = () => {
    const initialValue: PersonFormData = {
        firstName: '',
        lastName: '',
        address: {
            streetAndNumber: '',
            city: '',
            postalCode: '',
            country: ''
        },
        dateOfBirth: '',
        contact: {
            email: '',
            phone: '',
            note: ''
        }
    };

    const validateForm = (data: PersonFormData) => {
        const errors: Record<string, string> = {};

        if (!data.firstName) {
            errors['firstName'] = 'Jméno je povinné';
        }

        if (!data.lastName) {
            errors['lastName'] = 'Příjmení je povinné';
        }

        if (!data.address.city) {
            errors['address.city'] = 'Město je povinné';
        }

        if (data.contact?.email && !data.contact.email.includes('@')) {
            errors['contact.email'] = 'Neplatný email';
        }

        return Object.keys(errors).length > 0 ? errors : {};
    };

    const handleSubmit = (data: PersonFormData) => {
        console.log('Formulář odeslán:', data);
        // Zde by byla logika pro odeslání dat
    };

    return (
        <div className="max-w-md mx-auto p-6">
            <h2 className="text-2xl font-bold mb-6">Osobní údaje</h2>

            <Form
                value={initialValue}
                onSubmit={handleSubmit}
                validate={validateForm}
            >
                <Field name="firstName">
                    {({value, onChange, hasError}: FieldRenderProps<string>) => (
                        <div className="mb-4">
                            <label className="block text-sm font-medium mb-2">Jméno</label>
                            <input
                                type="text"
                                value={value || ''}
                                onChange={(e) => onChange(e.target.value)}
                                className={`w-full px-3 py-2 border rounded-md ${
                                    hasError ? 'border-red-500' : 'border-gray-300'
                                }`}
                            />
                        </div>
                    )}
                </Field>

                <Field name="lastName">
                    {({value, onChange, hasError}: FieldRenderProps<string>) => (
                        <div className="mb-4">
                            <label className="block text-sm font-medium mb-2">Příjmení</label>
                            <input
                                type="text"
                                value={value || ''}
                                onChange={(e) => onChange(e.target.value)}
                                className={`w-full px-3 py-2 border rounded-md ${
                                    hasError ? 'border-red-500' : 'border-gray-300'
                                }`}
                            />
                        </div>
                    )}
                </Field>

                <Field name="address.city">
                    {({value, onChange, hasError}: FieldRenderProps<string>) => (
                        <div className="mb-4">
                            <label className="block text-sm font-medium mb-2">Město</label>
                            <input
                                type="text"
                                value={value || ''}
                                onChange={(e) => onChange(e.target.value)}
                                className={`w-full px-3 py-2 border rounded-md ${
                                    hasError ? 'border-red-500' : 'border-gray-300'
                                }`}
                            />
                        </div>
                    )}
                </Field>

                <Field
                    name="contact.email"
                    validate={(value: string) => {
                        if (value && !value.includes('@')) {
                            return 'Neplatný formát emailu';
                        }
                        return null;
                    }}
                >
                    {({value, onChange, hasError}: FieldRenderProps<string>) => (
                        <div className="mb-4">
                            <label className="block text-sm font-medium mb-2">Email</label>
                            <input
                                type="email"
                                value={value || ''}
                                onChange={(e) => onChange(e.target.value)}
                                className={`w-full px-3 py-2 border rounded-md ${
                                    hasError ? 'border-red-500' : 'border-gray-300'
                                }`}
                            />
                        </div>
                    )}
                </Field>
            </Form>
        </div>
    );
};
