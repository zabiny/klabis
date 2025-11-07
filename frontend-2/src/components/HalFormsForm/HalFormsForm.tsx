import * as Yup from "yup";
import {
    Alert,
    Button,
    Checkbox,
    FormControl,
    FormControlLabel,
    FormGroup,
    FormHelperText,
    FormLabel,
    MenuItem,
    Radio,
    RadioGroup,
    Select,
    TextField
} from "@mui/material";
import {Field, Form, Formik} from "formik";
import React, {type ReactElement, type ReactNode, useCallback, useEffect, useState} from "react";
import {
    type HalFormsOption,
    type HalFormsOptionType,
    type HalFormsProperty,
    type HalFormsResponse,
    type HalFormsTemplate,
    type Link,
    type OptionItem
} from "../../api";
import {fetchHalFormsData, submitHalFormsData} from "../../api/hateoas";
import {isHalFormsResponse} from "./utils";
import {klabisAuthUserManager} from "../../api/klabisUserManager";

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

function getValidationSchema(template: HalFormsTemplate): Yup.ObjectSchema<any> {
    const shape: Record<string, any> = {};
    template.properties.forEach((prop) => {
        let validator: any = Yup.string();

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

        shape[prop.name] = validator;
    });
    return Yup.object().shape(shape);
}

// --- Render funkce pro pole ---
function renderField(
    prop: HalFormsProperty,
    values: Record<string, any>,
    setFieldValue: (field: string, value: any) => void,
    errors: Record<string, any>,
    touched: Record<string, any>
): ReactNode {
    const errorText = touched[prop.name] && errors[prop.name] ? errors[prop.name] : "";

// OPTIONS s multiple = Checkboxy
    if (prop.options && prop.multiple) {
        return <HalFormsCheckbox prop={prop} value={values[prop.name]} onValueChanged={setFieldValue}/>;
    }

// OPTIONS + type radio
    if (prop.options && prop.type === "radio") {
        return <HalFormsRadio prop={prop} value={values[prop.value]} onValueChanged={setFieldValue}/>;
    }

    // OPTIONS single = Select
    if (prop.options) {
        return <HalFormsSelect prop={prop} value={values[prop.value]} onValueChanged={setFieldValue}/>;
    }

    // TEXTAREA
    if (prop.type === "textarea") {
        return (
            <Field
                as={TextField}
                id={prop.name}
                name={prop.name}
                label={prop.prompt || prop.name}
                fullWidth
                multiline
                rows={4}
                error={!!errorText}
                helperText={errorText}
            />
        );
    }

    // Default: TextField
    return (
        <Field
            as={TextField}
            id={prop.name}
            name={prop.name}
            type={prop.type || "text"}
            label={prop.prompt || prop.name}
            fullWidth
            error={!!errorText}
            helperText={errorText}
        />
    );
}

const useHalFormsController = (
    api: Link
): {
    isLoading: boolean,
    submit: (formData: Record<string, any>) => Promise<void>,
    error?: string,
    submitError?: string,
    formData?: HalFormsResponse,
} => {
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>();
    const [formData, setFormData] = useState<HalFormsResponse>();
    const [submitError, setSubmitError] = useState<string>();

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            setError(undefined);

            try {
                const data = await fetchHalFormsData(api);
                if (isHalFormsResponse(data)) {
                    setFormData(data);
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

        fetchData();
    }, [api]);

    const submit = useCallback(
        async (data: Record<string, any>) => {
            const defaultTemplate = formData?._templates?.default;
            const method = defaultTemplate?.method || "POST";

            try {
                await submitHalFormsData(method, api, data);
            } catch (submitError) {
                setSubmitError(
                    submitError instanceof Error ? submitError.message : "Error submitting form data"
                );
                throw submitError; // Re-throw error in case caller needs to handle it
            }
        },
        [formData, api] // No dependencies as `formData` and methods come from the function context
    );

    return {isLoading, submit, error, formData, submitError};
};

const HalFormsFormController = ({api}: { api: Link }): ReactElement => {
    const {isLoading, submit, error, formData, submitError} = useHalFormsController(api);

    //console.log(`Loading=${isLoading}, error=${error}, formData=${JSON.stringify(formData)}`);

    if (isLoading) {
        return <span>Loading form data (${api.href})</span>;
    }

    if (error) {
        return <Alert severity={"error"}>{error}</Alert>;
    } else if (!formData?._templates?.default) {
        return <Alert severity={"error"}>Response doesn't contain form template 'default', can't render HalForms
            form</Alert>
    }

    return <div>
        <HalFormsForm data={formData} template={formData._templates.default} onSubmit={submit}/>
        {submitError && <Alert severity={"error"}>{submitError}</Alert>}
    </div>;
}

interface HalFormsFormProps {
    data: Record<string, any>;
    template: HalFormsTemplate;
    onSubmit?: (values: Record<string, any>) => void;
    submitButtonLabel?: string
}

// --- Hlavní komponenta ---
const HalFormsForm: React.FC<HalFormsFormProps> = ({data, template, onSubmit, submitButtonLabel = "Odeslat"}) => {
    const initialValues = getInitialValues(template, data);
    const validationSchema = getValidationSchema(template);

    return (
        <Formik
            initialValues={initialValues}
            validationSchema={validationSchema}
            onSubmit={(values, {setSubmitting}) => {
                if (onSubmit) onSubmit(values);
                setSubmitting(false);
            }}
        >
            {({values, setFieldValue, isSubmitting, errors, touched}) => (
                <Form style={{display: "grid", gap: "1rem"}}>
                    {template.properties.map((prop) => (
                        <div key={prop.name}>
                            {renderField(prop, values, setFieldValue, errors, touched)}
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

interface HalFormsInputProps<T> {
    prop: HalFormsProperty,
    errorText?: string,
    value: T,
    onValueChanged: (attrName: string, value: T) => void
}

const useOptionItems = (def: HalFormsOption | undefined): { isLoading: boolean, options: HalFormsOptionType[] } => {
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [options, setOptions] = useState<HalFormsOptionType[]>([]);

    const fetchData = async (url: string): Promise<void> => {
        setIsLoading(true);

        try {

            const user = await klabisAuthUserManager.getUser();
            const res = await fetch(url, {
                headers: {
                    Accept: "application/json",
                    "Authorization": `Bearer ${user?.access_token}`
                },
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();

            setOptions(data);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (def?.link && def.link.href) {
            fetchData(def.link.href);
        }
    }, [def]);

    if (def?.inline) {
        return {isLoading: false, options: def.inline};
    } else if (def?.link) {
        return {isLoading: isLoading, options: options};
    } else {
        return {isLoading: false, options: []}
    }

}

const HalFormsRadio: React.FC<HalFormsInputProps<string>> = ({prop, errorText, value, onValueChanged}) => {

    const {options} = useOptionItems(prop.options);

    function renderRadioOption(opt: HalFormsOptionType, idx: number): ReactElement {
        const val = typeof opt === "object" && "value" in opt ? opt.value : opt;
        const label = typeof opt === "object" && "prompt" in opt ? opt.prompt : String(opt);
        return <FormControlLabel key={idx} value={val} control={<Radio/>} label={label}/>;
    }

    return (
        <FormControl component="fieldset" error={!!errorText}>
            <FormLabel>{prop.prompt || prop.name}</FormLabel>
            <RadioGroup
                name={prop.name}
                value={value}
                onChange={(e) => onValueChanged(prop.name, e.target.value)}
            >
                {options.map((opt, idx) => renderRadioOption(opt, idx))}
            </RadioGroup>
            <FormHelperText>{errorText}</FormHelperText>
        </FormControl>
    );

}

const HalFormsSelect: React.FC<HalFormsInputProps<string>> = ({
                                                                  prop,
                                                                  errorText,
                                                                  value,
                                                                  onValueChanged
                                                              }): ReactElement => {
    const {options} = useOptionItems(prop.options);

    function renderSelectBoxOption(opt: HalFormsOptionType, idx: number): ReactElement {
        const val = getValue(opt);
        const label = getLabel(opt);
        return (
            <MenuItem key={idx} value={val}>
                {label}
            </MenuItem>
        );
    }

    return (
        <FormControl fullWidth error={!!errorText}>
            <FormLabel>{prop.prompt || prop.name}</FormLabel>
            <Select
                value={value}
                onChange={(e) => onValueChanged(prop.name, e.target.value)}
            >
                <MenuItem value="">
                    <em>-- vyber --</em>
                </MenuItem>
                {options.map((opt, idx) => renderSelectBoxOption(opt, idx))}
            </Select>
            <FormHelperText>{errorText}</FormHelperText>
        </FormControl>
    );
}

function isOptionItem(item: any): item is OptionItem {
    return item !== undefined && item !== null && item.value !== undefined;
}

function isNumber(item: any): item is Number {
    return typeof item === 'number';
}

function getValue(item: HalFormsOptionType): string {
    if (isOptionItem(item)) {
        return getValue(item.value);
    } else if (isNumber(item)) {
        return `${item}`;
    } else {
        return item;
    }
}

function getLabel(item: HalFormsOptionType): string {
    if (isOptionItem(item)) {
        return item.prompt || getLabel(item.value);
    } else if (isNumber(item)) {
        return `${item}`;
    } else {
        return item;
    }
}

const HalFormsCheckbox: React.FC<HalFormsInputProps<string[]>> = ({
                                                                      prop,
                                                                      errorText,
                                                                      value,
                                                                      onValueChanged
                                                                  }): ReactElement => {
    const {options} = useOptionItems(prop.options);

    function renderCheckbox(val: string, label: string, idx: number): ReactElement {
        return (
            <FormControlLabel
                key={idx}
                control={
                    <Checkbox
                        checked={value.includes(val)}
                        onChange={(e) => {
                            if (e.target.checked) {
                                onValueChanged(prop.name, [...value, val]);
                            } else {
                                onValueChanged(
                                    prop.name,
                                    value.filter((v: string) => v !== val)
                                );
                            }
                        }}
                    />
                }
                label={label}
            />
        );
    }

    return (
        <FormControl component="fieldset" error={!!errorText}>
            <FormLabel>{prop.prompt || prop.name}</FormLabel>
            <FormGroup>
                {options.map((opt, idx) => renderCheckbox(getValue(opt), getLabel(opt), idx))}
            </FormGroup>
            <FormHelperText>{errorText}</FormHelperText>
        </FormControl>
    );

}


export {HalFormsForm, HalFormsFormController};