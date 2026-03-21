import * as Yup from "yup";
import {Form, Formik, getIn, useFormikContext} from "formik";
import React, {type ReactElement, type ReactNode, useContext, useMemo} from "react";
import {type HalFormsProperty, type HalFormsTemplate} from "../../../api";
import {type HalFormFieldFactory, type HalFormsInputProps, type RenderMode, type SubElementConfiguration} from "./types.ts";
import {halFormsFieldsFactory} from "./HalFormsFieldFactory.tsx";
import {sanitizeFormValues} from "./utils.ts";
import {Alert, Button, Spinner} from "../../UI";
import {Box} from "../../UI/layout";
import {FieldWrapper} from "../../UI/forms";
import {UI_MESSAGES, VALIDATION_MESSAGES} from "../../../constants/messages.ts";

type FormData = Record<string, unknown>;

// --- Helpers ---

function getInitialValues(
    template: HalFormsTemplate,
    data: FormData
): Record<string, unknown> {
    const initialValues: Record<string, unknown> = {};
    template.properties.forEach((prop) => {
        if (prop.multiple) {
            initialValues[prop.name] = Array.isArray(data[prop.name]) ? data[prop.name] : [];
        } else {
            initialValues[prop.name] = data[prop.name] !== undefined ? data[prop.name] : prop.value || "";
        }

    });
    return initialValues;
}

// Type helper for Yup schemas
type YupSchemaMap = Record<string, Yup.AnySchema>;

// TODO: do we want "frontend validation"? There may be validations which can't be done on frontend...
function createValidationSchema(template: HalFormsTemplate): Yup.ObjectSchema<Record<string, any>> {
    const shape: YupSchemaMap = {};
    template.properties.forEach((prop) => {
        let validator: Yup.AnySchema;

        if (prop.type === "number") {
            validator = Yup.number().typeError(VALIDATION_MESSAGES.MUST_BE_NUMBER);
        } else if (prop.type === "email") {
            validator = Yup.string().email(VALIDATION_MESSAGES.INVALID_EMAIL);
        } else if (prop.type === "text") {
            validator = Yup.string();
        } else {
            validator = Yup.mixed();
        }

        if (prop.multiple) {
            validator = Yup.array();
        }

        if (prop.required) {
            validator = validator.required(VALIDATION_MESSAGES.REQUIRED_FIELD);
        } else {
            validator = validator.nullable();
        }

        if (prop.regex) {
            // Properly narrow type before calling .matches()
            if (validator.type === "string") {
                validator = (validator as Yup.StringSchema).matches(
                    new RegExp(prop.regex),
                    VALIDATION_MESSAGES.INVALID_FORMAT
                );
            } else if (validator.type === "mixed") {
                // For non-string types, convert to string first
                validator = Yup.string().matches(
                    new RegExp(prop.regex),
                    VALIDATION_MESSAGES.INVALID_FORMAT
                );
            }
        }

        shape[prop.name] = validator;
    });
    return Yup.object().shape(shape) as Yup.ObjectSchema<Record<string, unknown>>;
}

function subElementInputProps(attrName: string, parentProps: HalFormsInputProps, conf?: SubElementConfiguration): HalFormsInputProps {
    function subElementProp(parentProp: HalFormsProperty, attr: string, label: string = attr): HalFormsProperty {
        return {
            ...parentProp,
            name: parentProp.name + "." + attr,
            prompt: label,
            regex: undefined,
            type: conf?.type || 'text',
            options: undefined,
            multiple: false,
            value: parentProp.value
        };
    }

    return {
        prop: subElementProp(parentProps.prop, attrName, conf?.prompt),
        errorText: undefined,
        renderMode: parentProps.renderMode,
        subElementProps: parentProps.subElementProps
    };
}


// --- Read-only field display ---
const ReadOnlyField: React.FC<{ prop: HalFormsProperty }> = ({prop}) => {
    const {values} = useFormikContext<Record<string, unknown>>();
    const rawValue = getIn(values, prop.name);

    let displayValue: string;
    if (rawValue == null || rawValue === '') {
        displayValue = '\u2014';
    } else if (Array.isArray(rawValue)) {
        displayValue = rawValue.join(', ');
    } else if (typeof rawValue === 'object') {
        displayValue = JSON.stringify(rawValue);
    } else {
        displayValue = String(rawValue);
    }

    return (
        <FieldWrapper label={prop.prompt || prop.name}>
            <span className="py-2.5 text-text-primary">{displayValue}</span>
        </FieldWrapper>
    );
};

const ReadOnlyValue: React.FC<{ prop: HalFormsProperty }> = ({prop}) => {
    const {values} = useFormikContext<Record<string, unknown>>();
    const rawValue = getIn(values, prop.name);

    let displayValue: string;
    if (rawValue == null || rawValue === '') {
        displayValue = '\u2014';
    } else if (Array.isArray(rawValue)) {
        displayValue = rawValue.join(', ');
    } else if (typeof rawValue === 'object') {
        displayValue = JSON.stringify(rawValue);
    } else {
        displayValue = String(rawValue);
    }

    return <span className="text-text-primary">{displayValue}</span>;
};

const SIMPLE_FIELD_TYPES = new Set([
    'text', 'email', 'number', 'date', 'url', 'tel',
    'textarea', 'select', 'radioGroup', 'checkbox', 'checkboxGroup',
    'boolean', 'datetime'
]);

// --- Render funkce pro pole ---
function renderFieldInternal(
    prop: HalFormsProperty,
    errors: Record<string, unknown>,
    touched: Record<string, unknown>,
    fieldFactory?: HalFormFieldFactory,
    renderMode: RenderMode = 'field'
): ReactNode {
    const error = touched[prop.name] && errors[prop.name];
    const errorText: string = typeof error === 'string' ? error : '';

    if (prop.readOnly === true && SIMPLE_FIELD_TYPES.has(prop.type)) {
        if (renderMode === 'input') {
            return <ReadOnlyValue prop={prop} />;
        }
        return <ReadOnlyField prop={prop} />;
    }

    const fieldProps: HalFormsInputProps = {
        prop: prop,
        errorText: errorText || undefined,
        renderMode,
        subElementProps: (attrName, conf) => {
            return subElementInputProps(attrName, fieldProps, conf);
        }
    };

    const result = fieldFactory && fieldFactory(prop.type, fieldProps);

    if (result) {
        return result;
    }

    return (<Box className={errorText ? 'border-2 border-red-500 rounded p-4' : ''}>
            <Alert severity="warning">{prop.prompt || prop.name}: neznamy typ HAL+FORMS property: '{prop.type}'</Alert>
            {errorText && <Alert severity="error" className="mt-2">{errorText}</Alert>}
        </Box>
    );
}

export interface FormRenderHelpers {
    renderField: (fieldName: string) => ReactElement;
    renderInput: (fieldName: string) => ReactElement;
    renderLabel: (fieldName: string) => string | undefined;
}

export type RenderFormCallback = (helpers: FormRenderHelpers) => ReactElement;

interface HalFormsFormProps {
    data: Record<string, unknown>,
    template: HalFormsTemplate,
    onSubmit?: (values: Record<string, unknown>) => Promise<void>,
    onCancel?: () => void,
    submitButtonLabel?: string,
    submitIcon?: ReactNode,
    fieldsFactory?: HalFormFieldFactory,
    isSubmitting?: boolean,
    renderForm?: RenderFormCallback,
    serverValidationErrors?: Record<string, string>
}

// TODO: upravit HAL+FORMS: zobrazit "item" vzdy jako read only. Pokud je defalt template pro aktivni metodu, tak zobrazit tlacitko EDIT ktere prepne do editacniho rezimu. Pokud je default template pro GET metodu, tak jen pouzit policka s readonly pro lepsi zobrazeni. Na backendu pridat "default" affordanci pokud pro selflink zadna neexistuje (pouze pro ITEM).

const HalFormsFormContext = React.createContext<HalFormsFormContextType>({
    renderField: (name: string) => <>{name}</> as ReactElement,
    renderInput: (name: string) => <>{name}</> as ReactElement,
    renderLabel: () => undefined,
});

// --- Hlavní komponenta ---
const HalFormsForm: React.FC<React.PropsWithChildren<HalFormsFormProps>> = ({
                                                                                data,
                                                                                template,
                                                                                onSubmit,
                                                                                onCancel,
                                                                                fieldsFactory = halFormsFieldsFactory,
                                                                                submitButtonLabel = "Odeslat",
                                                                                submitIcon,
                                                                                isSubmitting: externalIsSubmitting = false,
                                                                                renderForm,
                                                                                children,
                                                                                serverValidationErrors
                                                                            }) => {

    const initialValues = useMemo(
        () => getInitialValues(template, data),
        [template, data]
    );
    const validationSchema = useMemo(
        () => createValidationSchema(template),
        [template]
    );

    return (
        <Formik
            initialValues={initialValues}
            initialErrors={serverValidationErrors}
            validationSchema={validationSchema}
            validateOnChange={false}
            validateOnBlur={true}
            onSubmit={async (values, {setSubmitting}) => {
                try {
                    if (onSubmit) {
                        await onSubmit(sanitizeFormValues(values));
                    }
                } finally {
                    setSubmitting(false)
                }
            }}>
            {({isSubmitting: formikIsSubmitting, errors, touched}) => {
                const isFormProcessing = formikIsSubmitting || externalIsSubmitting;

                const renderPseudoField = (fieldName: string): ReactElement | null => {
                    if (fieldName === "submit") {
                        return <Button
                            type="submit"
                            disabled={isFormProcessing}
                            variant="primary"
                            size="md"
                            loading={isFormProcessing}
                            startIcon={isFormProcessing ? <Spinner size="sm"/> : submitIcon}
                        >
                            {isFormProcessing ? UI_MESSAGES.SUBMITTING : submitButtonLabel}
                        </Button>;
                    }
                    if (fieldName === "cancel") {
                        return <Button
                            type="button"
                            disabled={isFormProcessing || !onCancel}
                            variant="secondary"
                            size="md"
                            onClick={() => onCancel && onCancel()}
                        >
                            {UI_MESSAGES.CLOSE}
                        </Button>;
                    }
                    return null;
                };

                const createFormHelpers = (): FormRenderHelpers => {
                    const renderFieldCallback = (fieldName: string): ReactElement => {
                        const pseudo = renderPseudoField(fieldName);
                        if (pseudo) return pseudo;
                        const prop = template.properties.find(p => p.name === fieldName);
                        if (!prop) {
                            return <Alert severity="error">Field '{fieldName}' not found in template</Alert>;
                        }
                        return renderFieldInternal(prop, errors, touched, fieldsFactory) as ReactElement;
                    };

                    const renderInputCallback = (fieldName: string): ReactElement => {
                        const pseudo = renderPseudoField(fieldName);
                        if (pseudo) return pseudo;
                        const prop = template.properties.find(p => p.name === fieldName);
                        if (!prop) {
                            return <Alert severity="error">Field '{fieldName}' not found in template</Alert>;
                        }
                        return renderFieldInternal(prop, errors, touched, fieldsFactory, 'input') as ReactElement;
                    };

                    const renderLabelCallback = (fieldName: string): string | undefined => {
                        const prop = template.properties.find(p => p.name === fieldName);
                        return prop?.prompt || prop?.name;
                    };

                    return {
                        renderField: renderFieldCallback,
                        renderInput: renderInputCallback,
                        renderLabel: renderLabelCallback,
                    };
                };

                let formContent: ReactElement;
                if (children) {
                    const contextValue: HalFormsFormContextType = useMemo(
                        () => createFormHelpers(),
                        [template, errors, touched, fieldsFactory, formikIsSubmitting, externalIsSubmitting]
                    );
                    formContent = <HalFormsFormContext value={contextValue}>
                        {children}
                    </HalFormsFormContext>;
                } else if (renderForm) {
                    formContent = renderForm(createFormHelpers());
                } else {
                    const helpers = createFormHelpers();
                    formContent = <>
                        {(template.title ? <h2 className="mb-6">{template.title}</h2> : <></>)}

                        {template.properties.map((prop) => (
                            <div key={prop.name}
                                 className={`transition-all duration-200 ${isFormProcessing ? 'opacity-60 pointer-events-none' : ''}`}>
                                {renderFieldInternal(prop, errors, touched, fieldsFactory)}
                            </div>
                        ))}

                        <div className="flex gap-3 mt-6">
                            {helpers.renderField('submit')}
                            {onCancel && helpers.renderField('cancel')}
                        </div>
                    </>;
                }

                return (
                    <Form className="space-y-4">
                        {formContent}
                    </Form>
                );
            }}
        </Formik>
    );
};

export const HalFormsFormField: React.FC<{ fieldName: string }> = ({fieldName}) => {
    const {renderField} = useContext(HalFormsFormContext);
    return renderField(fieldName);
}

export const HalFormsFormInput: React.FC<{ fieldName: string }> = ({fieldName}) => {
    const {renderInput} = useContext(HalFormsFormContext);
    return renderInput(fieldName);
}

type HalFormsFormContextType = FormRenderHelpers;

export type {HalFormsFormProps};
export {HalFormsForm, HalFormsFormContext};