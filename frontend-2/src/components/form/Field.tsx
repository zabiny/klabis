import React from 'react';
import {FieldProps} from './types';

export const Field: React.FC<FieldProps> = ({
                                                name,
                                                value,
                                                onChange,
                                                validate,
                                                errorMessage,
                                                children
                                            }) => {
    const handleChange = (newValue: any) => {
        if (onChange) {
            onChange(newValue);
        }
    };

    const hasError = !!errorMessage;

    return (
        <div className="field-container">
            {children({
                value,
                onChange: handleChange,
                hasError,
                errorMessage
            })}
            {hasError && (
                <div className="error-message text-red-500 text-sm mt-1">
                    {errorMessage}
                </div>
            )}
        </div>
    );
};
