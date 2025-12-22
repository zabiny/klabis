import * as Yup from "yup";
import {Form, Formik} from "formik";
import React, {type ReactElement, type ReactNode, useCallback, useContext, useEffect, useMemo, useState} from "react";
import {
    type HalFormsProperty,
    type HalFormsResponse,
    type HalFormsTemplate,
    isFormTarget,
    type TemplateTarget
} from "../../api";
import {fetchHalFormsData, isFormValidationError, submitHalFormsData} from "../../api/hateoas";
import {getDefaultTemplate, isHalFormsResponse} from "./utils";
import {type HalFormFieldFactory, type HalFormsInputProps, type SubElementConfiguration} from "./types";
import {halFormsFieldsFactory} from "./HalFormsFieldFactory";
import {Alert, Button, Spinner} from "../UI";
import {Box} from "../Layout";

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

// TODO: do we want "frontend validation"? There may be validations which can't be done on frontend...
function createValidationSchema(template: HalFormsTemplate): Yup.ObjectSchema<unknown> {
    const shape: Record<string, unknown> = {};
    template.properties.forEach((prop) => {
        let validator: Yup.AnySchema = Yup.mixed().nullable();

        if (prop.type === "number") {
            validator = Yup.number().typeError("Musí být číslo");
        } else if (prop.type === "email") {
            validator = Yup.string().email("Neplatný email");
        }

        if (prop.multiple) {
            validator = Yup.array();
        }

        if (prop.required) {
            validator = validator.required("Povinné pole");
        }

        if (prop.regex) {
            console.log(validator);
            if (validator.type === "mixed") {
                validator = Yup.string();
            }
            validator = validator.matches(new RegExp(prop.regex), "Nespravny format");
        }

        shape[prop.name] = validator;
    });
    return Yup.object().shape(shape);
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
    const errorText = touched[prop.name] && errors[prop.name] ? errors[prop.name] : "";

    const fieldProps: HalFormsInputProps = {
        prop: prop,
        errorText,
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

const useHalFormsController = (
    api: TemplateTarget,
    inputTemplate?: HalFormsTemplate
): {
    isLoading: boolean,
    submit: (formData: Record<string, unknown>) => Promise<void>,
    error?: string,
    submitError?: Error,
    template?: HalFormsTemplate,
    formData?: HalFormsResponse,
} => {
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>();
    const [formData, setFormData] = useState<HalFormsResponse>();
    const [submitError, setSubmitError] = useState<Error>();
    const [actualTemplate, setActualTemplate] = useState<HalFormsTemplate>();

    useEffect(() => {
        let cancelled = false;

        const fetchData = async () => {
            setIsLoading(true);
            setError(undefined);

            try {
                const data = await fetchHalFormsData(api);
                if (!cancelled) {
                    if (isHalFormsResponse(data)) {
                        setFormData(data);
                        setActualTemplate(getDefaultTemplate(data));
                    } else {
                        setError("Returned data doesn't have HAL FORMS format");
                        console.warn("Returned data doesn't have HAL FORMS format");
                        console.warn(JSON.stringify(data, null, 2));
                    }
                }
            } catch (fetchError) {
                if (!cancelled) {
                    setError(
                        fetchError instanceof Error ? fetchError.message : "Error fetching form data"
                    );
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        };
        if (inputTemplate) {
            setFormData({_templates: {formTemplate: inputTemplate}});
            setActualTemplate(inputTemplate);
        } else {
            fetchData();
        }

        return () => {
            cancelled = true;
        };
    }, [api, inputTemplate]);

    const submit = useCallback(
        async (data: Record<string, unknown>) => {
            // use target+method from template if is it present
            const submitTarget: TemplateTarget = isFormTarget(actualTemplate) && actualTemplate || api;
            try {
                await submitHalFormsData(submitTarget, data as Record<string, any>);
            } catch (submitError) {
                setSubmitError(
                    submitError instanceof Error ? submitError : new Error("Error submitting form data")
                )
                throw submitError; // Re-throw error in case caller needs to handle it
            }
        }, [api, actualTemplate])

    return {isLoading, submit, error, formData, template: actualTemplate, submitError};
}


const HalFormsFormController = ({api, inputTemplate}: {
    api: TemplateTarget,
    inputTemplate?: HalFormsTemplate
}): ReactElement => {
    const {isLoading, submit, error, formData, template, submitError} = useHalFormsController(api, inputTemplate);

    //console.log(`Loading=${isLoading}, error=${error}, formData=${JSON.stringify(formData)}`);

    if (isLoading) {
        return <span>Loading form data (`${api.target}`)</span>;
    }

    if (error) {
        return <Alert severity="error">{error}</Alert>;
    } else if (!template) {
        return <Alert severity="error">Response doesn't contain form template, can't render HalForms form</Alert>
    }

    return <div className="space-y-4">
        <HalFormsForm data={formData || {}} template={template} onSubmit={submit}/>
        {submitError && <Alert severity="error">{submitError.message}</Alert>}
        {isFormValidationError(submitError) && Object.entries(submitError.validationErrors).map((entry) =>
            <Alert key={entry[0]} severity="error">{entry[0]}:&nbsp;{entry[1]}</Alert>)}
    </div>;
}

type FieldRenderFunc = (fieldName: string) => ReactNode;

type RenderFormCallback = (renderField: (fieldName: string) => ReactElement) => ReactElement;

interface HalFormsFormProps {
    data: Record<string, unknown>,
    template: HalFormsTemplate,
    onSubmit?: (values: Record<string, unknown>) => void,
    onCancel?: () => void,
    submitButtonLabel?: string,
    fieldsFactory?: HalFormFieldFactory,
    isSubmitting?: boolean,
    renderForm?: RenderFormCallback
}

// TODO: zjednodusit - udelat kontext ktery bude drzet template a udelat hook ktery bude z toho kontextu tahat definici policka pro konkretni nazev
// TODO: udelat hook/kompomentu (ala Formik Field) ktera zkombinuje HalForms context s Formik a s pomoci Fields factory z jedineho parametru - field name udela kompletni ReactElement daneho fieldu. Takovy hook pak bude mozne pouzit pro libovolny layout formulare stejne jako to umi Formik.
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
            onSubmit={(values, {setSubmitting}) => {
                try {
                    if (onSubmit) {
                        onSubmit(values);
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
                                {isFormProcessing ? "Odesílám..." : submitButtonLabel}
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
                                Zpět
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

export {HalFormsForm, HalFormsFormController};