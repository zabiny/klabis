import React, {Children, cloneElement, isValidElement, useCallback, useMemo, useState} from 'react';
import {FieldProps, FormProps, ValidationErrors} from './types';
import {getNestedValue, setNestedValue} from './utils';
import {Field} from "./Field";

const getFieldProps = (element: unknown): FieldProps | null => {
    if (!isValidElement(element)) {
        return null;
    }
    const props = element.props as Partial<FieldProps>;

    // Ensure the required 'name' attribute exists and is a string
    if (typeof props.name !== 'string') {
        return null;
    }

    // Return the props typed as FieldProps (other attributes are optional)
    return props as FieldProps;
};

// Duplicate getFieldProps definition removed – functionality consolidated above.

export const Form: React.FC<FormProps> = ({
                                              value,
                                              onSubmit,
                                              validate,
                                              children, ariaLabel
                                          }) => {
    const [formValue, setFormValue] = useState(value);
    const [errors, setErrors] = useState<ValidationErrors>({});
    const [formLevelErrors, setFormLevelErrors] = useState<string[]>([]);

    const handleFieldChange = useCallback((fieldName: string, fieldValue: unknown) => {
        setFormValue(prev => setNestedValue(prev, fieldName, fieldValue));

        // Vymazat chybu pro toto pole při změně (vždy bezpečně)
        setErrors(prev => {
            const {[fieldName]: _, ...rest} = prev;
            return rest;
        });
    }, []);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        let validationErrors: ValidationErrors = {};

        if (validate) {
            const result = validate(formValue);
            if (result) {
                validationErrors = result;
            }
        }

        // Sesbírat field-level validační chyby
        const fieldErrors: ValidationErrors = {};
        const processFieldValidation = (element: React.ReactElement) => {
            if (element.type === Field || (element.type as any).name === 'Field') {
                const fieldProps = element.props as FieldProps;
                if (fieldProps.validate && fieldProps.name) {
                    const fieldValue = getNestedValue(formValue, fieldProps.name);
                    const fieldError = fieldProps.validate(fieldValue);
                    if (fieldError) {
                        fieldErrors[fieldProps.name] = fieldError;
                    }
                }
            }
        };

        Children.forEach(children, (child) => {
            if (isValidElement(child)) {
                processFieldValidation(child);
            }
        });

        // Sloučit všechny chyby
        const allErrors = {...validationErrors, ...fieldErrors};

        if (Object.keys(allErrors).length > 0) {
            setErrors(allErrors);

            // Najít chyby, které nemají odpovídající field
            const formErrors = {...allErrors};
            Children.forEach(children, (child) => {
                const props = getFieldProps(child);
                if (props != null) {
                    delete formErrors[props.name];
                }
            });
            setFormLevelErrors(Object.values(formErrors));
        } else {
            setErrors({});
            setFormLevelErrors([]);
            onSubmit(formValue);
        }
    };

    const enhancedChildren = useMemo(() => {
        return Children.map(children, (child) => {
            if (!isValidElement(child)) return child;

            // Pokud je to Field komponenta, předáme jí props
            if (child.type === Field || (child.type as any).name === 'Field') {
                const fieldProps = child.props as FieldProps;
                const fieldValue = getNestedValue(formValue, fieldProps.name);
                const fieldError = errors[fieldProps.name];

                // Přetypování na ReactElement<any>, aby TypeScript akceptoval rozšířené props
                return cloneElement(child as React.ReactElement<FieldProps>, {
                    ...fieldProps,
                    value: fieldValue,
                    onChange: (newValue: unknown) => handleFieldChange(fieldProps.name, newValue),
                    errorMessage: fieldError
                });
            }

            return child;
        });
    }, [children, formValue, errors, handleFieldChange]);

    return (
        <form onSubmit={handleSubmit} className="form-container" aria-label={ariaLabel || "Klabis form"}>
            {formLevelErrors.length > 0 && (
                <div className="form-errors mb-4">
                    {formLevelErrors.map((error, index) => (
                        <div key={index} className="error-message text-red-500 text-sm mb-2">
                            {error}
                        </div>
                    ))}
                </div>
            )}

            {enhancedChildren}

            <button
                type="submit"
                className="submit-button bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded mt-4"
            >
                Odeslat
            </button>
        </form>
    );
};
