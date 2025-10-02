import * as Yup from "yup";
import {
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
import React, {ReactElement, type ReactNode} from "react";
import {type HalFormsFormProps} from "./index";
import {type HalFormsProperty, type HalFormsTemplate} from "../../api";

type FormData = Record<string, any>;

// --- Helpers ---

function getInitialValues(
    template: HalFormsTemplate,
    data: FormData
): Record<string, any> {
    const initialValues: Record<string, any> = {};
    template.properties.forEach((prop) => {
        if (prop.multiple) {
            initialValues[prop.name] = Array.isArray(data[prop.name])
                ? data[prop.name]
                : [];
            data[prop.name] = data[prop.name] === null ? [] : data[prop.name];
        } else {
            initialValues[prop.name] =
                data[prop.name] !== undefined ? data[prop.name] : prop.value || "";
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
        return (
            <FormControl component="fieldset" error={!!errorText}>
                <FormLabel>{prop.prompt || prop.name}</FormLabel>
                <FormGroup>
                    {prop.options.map((opt, idx) => {
                        const val = typeof opt === "object" && "value" in opt ? opt.value : opt;
                        const label = typeof opt === "object" && "prompt" in opt ? opt.prompt : String(opt);
                        return (
                            <FormControlLabel
                                key={idx}
                                control={
                                    <Checkbox
                                        checked={values[prop.name].includes(val)}
                                        onChange={(e) => {
                                            if (e.target.checked) {
                                                setFieldValue(prop.name, [...values[prop.name], val]);
                                            } else {
                                                setFieldValue(
                                                    prop.name,
                                                    values[prop.name].filter((v: any) => v !== val)
                                                );
                                            }
                                        }}
                                    />
                                }
                                label={label}
                            />
                        );
                    })}
                </FormGroup>
                <FormHelperText>{errorText}</FormHelperText>
            </FormControl>
        );
    }

    // OPTIONS + type radio
    if (prop.options && prop.type === "radio") {
        return (
            <FormControl component="fieldset" error={!!errorText}>
                <FormLabel>{prop.prompt || prop.name}</FormLabel>
                <RadioGroup
                    name={prop.name}
                    value={values[prop.name]}
                    onChange={(e) => setFieldValue(prop.name, e.target.value)}
                >
                    {prop.options.map((opt, idx) => {
                        const val = typeof opt === "object" && "value" in opt ? opt.value : opt;
                        const label = typeof opt === "object" && "prompt" in opt ? opt.prompt : String(opt);
                        return <FormControlLabel key={idx} value={val} control={<Radio/>} label={label}/>;
                    })}
                </RadioGroup>
                <FormHelperText>{errorText}</FormHelperText>
            </FormControl>
        );
    }

    // OPTIONS single = Select
    if (prop.options) {
        return (
            <FormControl fullWidth error={!!errorText}>
                <FormLabel>{prop.prompt || prop.name}</FormLabel>
                <Select
                    value={values[prop.name]}
                    onChange={(e) => setFieldValue(prop.name, e.target.value)}
                >
                    <MenuItem value="">
                        <em>-- vyber --</em>
                    </MenuItem>
                    {prop.options.map((opt, idx) => {
                        const val = typeof opt === "object" && "value" in opt ? opt.value : opt;
                        const label = typeof opt === "object" && "prompt" in opt ? opt.prompt : String(opt);
                        return (
                            <MenuItem key={idx} value={val}>
                                {label}
                            </MenuItem>
                        );
                    })}
                </Select>
                <FormHelperText>{errorText}</FormHelperText>
            </FormControl>
        );
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

// --- Hlavní komponenta ---
export const HalFormsForm: React.FC<HalFormsFormProps> = ({data, template, onSubmit}) => {
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

                    <Button
                        type="submit"
                        disabled={isSubmitting}
                        variant="contained"
                        color="primary"
                    >
                        Odeslat
                    </Button>
                </Form>
            )}
        </Formik>
    );
};