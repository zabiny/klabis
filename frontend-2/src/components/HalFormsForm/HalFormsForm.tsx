import * as Yup from "yup";
import {Alert, Box, Button} from "@mui/material";
import {Form, Formik} from "formik";
import React, {type ReactElement, type ReactNode, useCallback, useEffect, useState} from "react";
import {type HalFormsProperty, type HalFormsResponse, type HalFormsTemplate, type TemplateTarget} from "../../api";
import {fetchHalFormsData, submitHalFormsData} from "../../api/hateoas";
import {isHalFormsResponse} from "./utils";
import {type HalFormFieldFactory, type HalFormsInputProps, SubElementConfiguration} from "./types";
import {muiHalFormsFieldsFactory} from "./MuiHalFormsFieldsFactory";

type FormData = Record<string, any>;

// --- Helpers ---

function getInitialValues(
    template: HalFormsTemplate,
    data: FormData
): Record<string, any> {
    const initialValues: Record<string, any> = {};
    template.properties.forEach((prop) => {
        if (prop.multiple) {
            initialValues[prop.name] = Array.isArray(data[prop.name]) ? data[prop.name] : [];
            data[prop.name] = data[prop.name] === null ? [] : data[prop.name];
        } else {
            initialValues[prop.name] = data[prop.name] !== undefined ? data[prop.name] : prop.value || "";
            data[prop.name] = data[prop.name] === null ? "" : data[prop.name];
        }

    });
    return initialValues;
}

function createValidationSchema(template: HalFormsTemplate): Yup.ObjectSchema<any> {
    const shape: Record<string, any> = {};
    template.properties.forEach((prop) => {
        let validator: any = Yup.mixed().nullable();

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
            validator = Yup.string().required().matches(new RegExp(prop.regex), "Nespravny format");
        }

        shape[prop.name] = validator;
    });
    return Yup.object().shape(shape);
}

function subElementInputProps(attrName: string, parentProps: HalFormsInputProps, conf: SubElementConfiguration): HalFormsInputProps {
    function subElementProp(parentProp: HalFormsProperty, attr: string, label: string = attr): HalFormsProperty {
        return {
            ...parentProp,
            name: parentProp.name + "." + attr,
            prompt: label,
            regex: undefined,
            type: conf.type || 'text',
            options: undefined,
            multiple: false,
            value: parentProp.value
        };
    }

    return {
        prop: subElementProp(parentProps.prop, attrName, conf.prompt),
        errorText: undefined,
        subElementProps: parentProps.subElementProps
    };
}


// --- Render funkce pro pole ---
function renderField(
    prop: HalFormsProperty,
    values: Record<string, any>,
    setFieldValue: (field: string, value: any) => void,
    errors: Record<string, any>,
    touched: Record<string, any>,
    fieldFactory?: HalFormFieldFactory
): ReactNode {
    const errorText = touched[prop.name] && errors[prop.name] ? errors[prop.name] : "";

    const fieldProps = {
        prop: prop,
        errorText,
        onValueChanged: setFieldValue,
        value: values[prop.name],
        subElementProps: (attrName, conf) => {
            return subElementInputProps(attrName, fieldProps, conf);
        }
    }

    const result = fieldFactory && fieldFactory(prop.type, fieldProps);

    if (result) {
        return result;
    }

    return (<Box sx={errorText ? {border: '1px solid red'} : {}}>
            <Alert severity={"warning"}>{prop.prompt || prop.name}: neznamy typ HAL+FORMS property:
                '{prop.type}'</Alert>
            {errorText && <Alert severity={"error"}>{errorText}</Alert>}
        </Box>
    );
}

const useHalFormsController = (
    api: TemplateTarget,
    inputTemplate?: HalFormsTemplate
): {
    isLoading: boolean,
    submit: (formData: Record<string, any>) => Promise<void>,
    error?: string,
    submitError?: string,
    template?: HalFormsTemplate,
    formData?: HalFormsResponse,
} => {
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>();
    const [formData, setFormData] = useState<HalFormsResponse>();
    const [submitError, setSubmitError] = useState<string>();
    const [actualTemplate, setActualTemplate] = useState<HalFormsTemplate>();

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            setError(undefined);

            try {
                const data = await fetchHalFormsData(api);
                if (isHalFormsResponse(data)) {
                    setFormData(data);
                    setActualTemplate(data._templates?.default);
                } else {
                    setError("Returned data doesn't have HAL FORMS format");
                    console.warn("Returned data doesn't have HAL FORMS format");
                    console.warn(JSON.stringify(data, null, 2));
                }
            } catch (fetchError) {
                setError(
                    fetchError instanceof Error ? fetchError.message : "Error fetching form data"
                );
            } finally {
                setIsLoading(false);
            }
        };
        if (inputTemplate) {
            setFormData({_templates: {default: inputTemplate}});
            setActualTemplate(inputTemplate);
        } else {
            fetchData();
        }
    }, [api, inputTemplate]);

    const submit = useCallback(
        async (data: Record<string, any>) => {
            const defaultTemplate = formData?._templates?.default;
            const method = defaultTemplate?.method || "POST";
            console.log(`Submitting.... ${method} ${api}`);

            try {
                await submitHalFormsData(api, data);
            } catch (submitError) {
                setSubmitError(
                    submitError instanceof Error ? submitError.message : "Error submitting form data"
                );
                throw submitError; // Re-throw error in case caller needs to handle it
            }
        },
        [formData, api] // No dependencies as `formData` and methods come from the function context
    );

    return {isLoading, submit, error, formData, template: actualTemplate, submitError};
};

const HalFormsFormController = ({api, inputTemplate}: {
    api: TemplateTarget,
    inputTemplate?: HalFormsTemplate
}): ReactElement => {
    const {isLoading, submit, error, formData, template, submitError} = useHalFormsController(api, inputTemplate);

    //console.log(`Loading=${isLoading}, error=${error}, formData=${JSON.stringify(formData)}`);

    if (isLoading) {
        return <span>Loading form data (${api.target})</span>;
    }

    if (error) {
        return <Alert severity={"error"}>{error}</Alert>;
    } else if (!template) {
        return <Alert severity={"error"}>Response doesn't contain form template 'default', can't render HalForms
            form</Alert>
    }

    return <div>
        <HalFormsForm data={formData || {}} template={template} onSubmit={submit}/>
        {submitError && <Alert severity={"error"}>{submitError}</Alert>}
    </div>;
}

interface HalFormsFormProps {
    data: Record<string, any>;
    template: HalFormsTemplate;
    onSubmit?: (values: Record<string, any>) => void;
    submitButtonLabel?: string,
    fieldsFactory?: HalFormFieldFactory
}

// --- Hlavní komponenta ---
const HalFormsForm: React.FC<HalFormsFormProps> = ({
                                                       data,
                                                       template,
                                                       onSubmit,
                                                       fieldsFactory = muiHalFormsFieldsFactory,
                                                       submitButtonLabel = "Odeslat"
                                                   }) => {

    const initialValues = getInitialValues(template, data);
    const validationSchema = createValidationSchema(template);

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
            {({values, setFieldValue, isSubmitting, errors, touched}) => (
                <Form style={{display: "grid", gap: "1rem"}}>
                    {(template.title ? <h2>{template.title}</h2> : <></>)}

                    {template.properties.map((prop) => (
                        <div key={prop.name}>
                            {renderField(prop, values, setFieldValue, errors, touched, fieldsFactory)}
                        </div>
                    ))}

                    <Button type="submit" disabled={isSubmitting} variant="contained" color="primary">
                        {submitButtonLabel}
                    </Button>
                </Form>
            )}
        </Formik>
    );
};


export {HalFormsForm, HalFormsFormController};