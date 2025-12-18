import * as Yup from "yup";
import {Alert, Box, Button, CircularProgress} from "@mui/material";
import {Form, Formik} from "formik";
import React, {type ReactElement, type ReactNode, useCallback, useEffect, useState} from "react";
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
        } else {
            initialValues[prop.name] = data[prop.name] !== undefined ? data[prop.name] : prop.value || "";
        }

    });
    return initialValues;
}

// TODO: do we want "frontend validation"? There may be validations which can't be done on frontend...
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
    errors: Record<string, any>,
    touched: Record<string, any>,
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
        const fetchData = async () => {
            setIsLoading(true);
            setError(undefined);

            try {
                const data = await fetchHalFormsData(api);
                if (isHalFormsResponse(data)) {
                    setFormData(data);
                    setActualTemplate(getDefaultTemplate(data));
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
            setFormData({_templates: {formTemplate: inputTemplate}});
            setActualTemplate(inputTemplate);
        } else {
            fetchData();
        }
    }, [api, inputTemplate]);

    const submit = useCallback(
        async (data: Record<string, any>) => {
            // use target+method from template if is it present
            const submitTarget: TemplateTarget = isFormTarget(actualTemplate) && actualTemplate || api;
            try {
                await submitHalFormsData(submitTarget, data);
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
        return <Alert severity={"error"}>{error}</Alert>;
    } else if (!template) {
        return <Alert severity={"error"}>Response doesn't contain form template, can't render HalForms form</Alert>
    }

    return <div>
        <HalFormsForm data={formData || {}} template={template} onSubmit={submit}/>
        {submitError && <Alert severity={"error"}>{submitError.message}</Alert>}
        {isFormValidationError(submitError) && Object.entries(submitError.validationErrors).map((entry) =>
            <Alert severity={"error"}>{entry[0]}:&nbsp;{entry[1]}</Alert>)}
    </div>;
}

interface HalFormsFormProps {
    data: Record<string, any>,
    template: HalFormsTemplate,
    onSubmit?: (values: Record<string, any>) => void,
    onCancel?: () => void,
    submitButtonLabel?: string,
    fieldsFactory?: HalFormFieldFactory,
    isSubmitting?: boolean
}

// TODO: zjednodusit - udelat kontext ktery bude drzet template a udelat hook ktery bude z toho kontextu tahat definici policka pro konkretni nazev
// TODO: udelat hook/kompomentu (ala Formik Field) ktera zkombinuje HalForms context s Formik a s pomoci Fields factory z jedineho parametru - field name udela kompletni ReactElement daneho fieldu. Takovy hook pak bude mozne pouzit pro libovolny layout formulare stejne jako to umi Formik.
// TODO: upravit HAL+FORMS: zobrazit "item" vzdy jako read only. Pokud je defalt template pro aktivni metodu, tak zobrazit tlacitko EDIT ktere prepne do editacniho rezimu. Pokud je default template pro GET metodu, tak jen pouzit policka s readonly pro lepsi zobrazeni. Na backendu pridat "default" affordanci pokud pro selflink zadna neexistuje (pouze pro ITEM).

// --- Hlavní komponenta ---
const HalFormsForm: React.FC<HalFormsFormProps> = ({
                                                       data,
                                                       template,
                                                       onSubmit,
                                                       onCancel,
                                                       fieldsFactory = muiHalFormsFieldsFactory,
                                                       submitButtonLabel = "Odeslat",
                                                       isSubmitting: externalIsSubmitting = false
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
            {({isSubmitting: formikIsSubmitting, errors, touched}) => {
                const isFormProcessing = formikIsSubmitting || externalIsSubmitting;
                return (
                    <Form style={{display: "grid", gap: "1rem"}}>
                        {(template.title ? <h2>{template.title}</h2> : <></>)}

                        {template.properties.map((prop) => (
                            <div key={prop.name} style={{opacity: isFormProcessing ? 0.6 : 1, pointerEvents: isFormProcessing ? 'none' : 'auto'}}>
                                {renderField(prop, errors, touched, fieldsFactory)}
                            </div>
                        ))}

                        <Box sx={{display: 'flex', gap: 1}}>
                            <Button
                                type="submit"
                                disabled={isFormProcessing}
                                variant="contained"
                                color="primary"
                                sx={{minWidth: 120}}
                                startIcon={isFormProcessing ? <CircularProgress size={20}/> : undefined}
                            >
                                {isFormProcessing ? "Odesílám..." : submitButtonLabel}
                            </Button>
                            {onCancel && <Button
                                type="button"
                                disabled={isFormProcessing}
                                variant={"contained"}
                                color="secondary"
                                onClick={() => onCancel()}
                            >
                                Zpět
                            </Button>}
                        </Box>
                    </Form>
                );
            }}
        </Formik>
    );
};


export {HalFormsForm, HalFormsFormController};