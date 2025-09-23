import {type FieldProps} from './types';
import {useState} from "react";

interface InputFieldProps extends FieldProps<string> {
    inputType?: "number" | "text" | "email" | "date"
}

const validateInternal = <T, >(required: boolean, validate: ((value: T) => string | null)): (value: T | undefined | null) => string | null => {

    return (value: T | null | undefined) => {
        if (value) {
            return validate(value);
        } else if (required) {
            return 'Chybi hodnota';
        } else {
            return null;
        }
    }

}

export const Field = ({
                          name, value, onChange = () => {
    }, validate = () => null, inputType = "text", required = false, initialErrorMessage
                      }: InputFieldProps) => {

    const [errorMessage, setErrorMessage] = useState(initialErrorMessage);
    const hasError = !!errorMessage;

    const onChangeInternal = (updatedValue: string) => {
        const updatedMessage = validateInternal(required, validate)(updatedValue);
        setErrorMessage(updatedMessage || undefined);
        if (!updatedMessage) {
            onChange(updatedValue);
        }
    }

    return (<div className="field-container">
        <label>
            {name}
            <input
                data-testid="field-input"
                value={value || ''}
                type={inputType}
                onChange={(e) => onChangeInternal(e.target.value)}
                className={hasError ? 'error' : ''}
            />
            {hasError && <span data-testid="error-message" className="error-message">{errorMessage}</span>}
        </label>
    </div>);
}
