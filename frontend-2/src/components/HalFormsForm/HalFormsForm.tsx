import * as Yup from "yup";
import {Form, Formik} from "formik";
import React, {type ReactElement, type ReactNode, useContext, useMemo} from "react";
import {type HalFormsProperty, type HalFormsTemplate} from "../../api";
import {type HalFormFieldFactory, type HalFormsInputProps, type SubElementConfiguration} from "./types";
import {halFormsFieldsFactory} from "./HalFormsFieldFactory";
import {Alert, Button, Spinner} from "../UI";
import {Box} from "../Layout";
import {UI_MESSAGES, VALIDATION_MESSAGES} from "../../constants/messages";

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
        let validator: Yup.AnySchema = Yup.mixed().nullable();

        if (prop.type === "number") {
            validator = Yup.number().typeError(VALIDATION_MESSAGES.MUST_BE_NUMBER).nullable();
        } else if (prop.type === "email") {
            validator = Yup.string().email(VALIDATION_MESSAGES.INVALID_EMAIL).nullable();
        } else if (prop.type === "text") {
            validator = Yup.string().nullable();
        }

        if (prop.multiple) {
            validator = Yup.array();
        }

        if (prop.required) {
            validator = validator.required(VALIDATION_MESSAGES.REQUIRED_FIELD);
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
        subElementProps: parentProps.subElementProps
    };
}


// --- Render funkce pro pole ---
function renderField(
    prop: HalFormsProperty,
    errors: Record<string, unknown>,
    touched: Record<string, unknown>,
    fieldFactory?: HalFormFieldFactory
): ReactNode {
    // Properly narrow error type to string
    const error = touched[prop.name] && errors[prop.name];
    const errorText: string = typeof error === 'string' ? error : '';

    const fieldProps: HalFormsInputProps = {
        prop: prop,
        errorText: errorText || undefined,
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

type FieldRenderFunc = (fieldName: string) => ReactNode;

export type RenderFormCallback = (renderField: (fieldName: string) => ReactElement) => ReactElement;

interface HalFormsFormProps {
    data: Record<string, unknown>,
    template: HalFormsTemplate,
    onSubmit?: (values: Record<string, unknown>) => Promise<void>,
    onCancel?: () => void,
    submitButtonLabel?: string,
    fieldsFactory?: HalFormFieldFactory,
    isSubmitting?: boolean,
    renderForm?: RenderFormCallback
}

// TODO: upravit HAL+FORMS: zobrazit "item" vzdy jako read only. Pokud je defalt template pro aktivni metodu, tak zobrazit tlacitko EDIT ktere prepne do editacniho rezimu. Pokud je default template pro GET metodu, tak jen pouzit policka s readonly pro lepsi zobrazeni. Na backendu pridat "default" affordanci pokud pro selflink zadna neexistuje (pouze pro ITEM).

const HalFormsFormContext = React.createContext<HalFormsFormContextType>({renderField: (name) => `${name}`});

// --- Hlavní komponenta ---
const HalFormsForm: React.FC<React.PropsWithChildren<HalFormsFormProps>> = ({
                                                                                data,
                                                                                template,
                                                                                onSubmit,
                                                                                onCancel,
                                                                                fieldsFactory = halFormsFieldsFactory,
                                                                                submitButtonLabel = "Odeslat",
                                                                                isSubmitting: externalIsSubmitting = false,
                                                                                renderForm,
                                                                                children
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
            validationSchema={validationSchema}
            validateOnChange={false}
            validateOnBlur={true}
            onSubmit={async (values, {setSubmitting}) => {
                try {
                    if (onSubmit) {
                        await onSubmit(values);
                    }
                } finally {
                    setSubmitting(false)
                }
            }}>
            {({isSubmitting: formikIsSubmitting, errors, touched}) => {
                const isFormProcessing = formikIsSubmitting || externalIsSubmitting;

                const createRenderFieldCallback = (): ((fieldName: string) => ReactElement) => {
                    return (fieldName: string) => {
                        if (fieldName === "submit") {
                            return <Button
                                type="submit"
                                disabled={isFormProcessing}
                                variant="primary"
                                size="md"
                                loading={isFormProcessing}
                                startIcon={isFormProcessing ? <Spinner size="sm"/> : undefined}
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

                        const prop = template.properties.find(p => p.name === fieldName);
                        if (!prop) {
                            return <Alert severity="error">Field '{fieldName}' not found in template</Alert>;
                        }
                        return renderField(prop, errors, touched, fieldsFactory) as ReactElement;
                    };
                };

                let formContent: ReactElement;
                if (children) {
                    const contextValue: HalFormsFormContextType = useMemo(
                        () => ({
                            renderField: createRenderFieldCallback()
                        }),
                        [template, errors, touched, fieldsFactory]
                    );
                    formContent = <HalFormsFormContext value={contextValue}>
                        {children}
                    </HalFormsFormContext>;
                } else if (renderForm) {
                    formContent = renderForm(createRenderFieldCallback());
                } else {
                    formContent = <>
                        {(template.title ? <h2 className="mb-6">{template.title}</h2> : <></>)}

                        {template.properties.map((prop) => (
                            <div key={prop.name}
                                 className={`transition-all duration-200 ${isFormProcessing ? 'opacity-60 pointer-events-none' : ''}`}>
                                {renderField(prop, errors, touched, fieldsFactory)}
                            </div>
                        ))}

                        <div className="flex gap-3 mt-6">
                            {createRenderFieldCallback()('submit')}
                            {onCancel && createRenderFieldCallback()('cancel')}
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

interface HalFormsFormContextType {
    renderField: FieldRenderFunc
}

export {HalFormsForm};