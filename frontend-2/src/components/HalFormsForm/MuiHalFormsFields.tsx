import type {HalFormsOption, HalFormsOptionType, HalFormsOptionValue, OptionItem} from "../../api";
import {type ReactElement, useEffect, useState} from "react";
import {klabisAuthUserManager} from "../../api/klabisUserManager";
import {
    Alert,
    FormControl,
    FormControlLabel,
    FormGroup,
    FormHelperText,
    FormLabel,
    MenuItem,
    Radio,
    RadioGroup,
    Select,
    Switch,
    TextField
} from "@mui/material";
import {Checkbox} from "formik-mui";
import {Field} from "formik";
import {type HalFormsInputProps} from "./types";


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

export const HalFormsRadio: React.FC<HalFormsInputProps<string>> = ({prop, errorText, value, onValueChanged}) => {

    const {options} = useOptionItems(prop.options);

    function renderRadioOption(opt: HalFormsOptionType, idx: number): ReactElement {
        const val = getValue(opt);
        const label = getLabel(opt);

        return <FormControlLabel key={idx} value={val} control={<Radio/>} label={label}/>;
    }

    return (
        <FormControl component="fieldset" error={!!errorText} sx={errorText ? {border: '1px solid red', p: 1} : {p: 1}}>
            <FormLabel>{prop.prompt || prop.name}</FormLabel>
            <RadioGroup
                aria-errormessage={errorText}
                name={prop.name}
                value={value}
                onChange={(e) => onValueChanged(prop.name, e.target.value)}
            >
                {renderOptions(options, renderRadioOption)}
            </RadioGroup>
            <FormHelperText>{errorText}</FormHelperText>
        </FormControl>
    );

}

export const HalFormsSelect: React.FC<HalFormsInputProps<string>> = ({
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
                {renderOptions(options, renderSelectBoxOption)}
            </Select>
            <FormHelperText>{errorText}</FormHelperText>
        </FormControl>
    );
}

function isOptionItem(item: any): item is OptionItem {
    return item !== undefined && item !== null && item.value !== undefined;
}

function isNumber(item: any): item is number {
    return typeof item === 'number';
}

function optionValueToString(value: HalFormsOptionValue): string {
    if (isNumber(value)) {
        return `${value}`;
    } else {
        return value;
    }
}

function getValue(item: HalFormsOptionType): string {
    if (isOptionItem(item)) {
        return getValue(item.value);
    } else {
        return optionValueToString(item);
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

function renderOptions(options: HalFormsOptionType[], optionRender: (opt: HalFormsOptionType, key: number) => ReactElement): ReactElement {
    if (!options) {
        return <Alert severity={"warning"}>No options available</Alert>;
    }

    return <>{options.map(optionRender)}</>;
}

export const HalFormsCheckbox: React.FC<HalFormsInputProps<boolean>> = ({
                                                                            prop,
                                                                            errorText
                                                                        }): ReactElement => {
    return (
        <FormControlLabel
            key={prop.name}
            value={prop.value}
            name={prop.name}
            control={
                <Field type={"checkbox"} component={Checkbox} error={errorText}/>
            }
            label={prop.prompt || prop.name}
        />
    );

}


export const HalFormsCheckboxGroup: React.FC<HalFormsInputProps<string[]>> = ({
                                                                             prop,
                                                                                  errorText
                                                                         }): ReactElement => {
    const {options} = useOptionItems(prop.options);

    function renderCheckbox(opt: HalFormsOptionType, idx: number): ReactElement {
        const val = getValue(opt);
        const label = getLabel(opt);
        return (
            <FormControlLabel
                key={idx}
                value={val}
                name={prop.name}
                control={
                    <Field type={"checkbox"} component={Checkbox}/>
                }
                label={label}
            />
        );
    }

    return (
        <FormControl component="fieldset" error={!!errorText}>
            <FormLabel>{prop.prompt || prop.name}</FormLabel>
            <FormGroup>{renderOptions(options, renderCheckbox)}</FormGroup>
            <FormHelperText>{errorText}</FormHelperText>
        </FormControl>
    );

}

export const HalFormsBoolean: React.FC<HalFormsInputProps<boolean>> = ({
                                                                           prop,
                                                                           errorText,
                                                                           value,
                                                                           onValueChanged
                                                                       }): ReactElement => {

    return (

        <FormControlLabel
            key={prop.name}
            name={prop.name}
            value={true}
            label={prop.prompt || prop.name}
            control={
                <Field type={"radio"} as={Switch} error={errorText}/>
            }
        />


    );

}

export const HalFormsTextArea: React.FC<HalFormsInputProps<string>> = ({
                                                                           prop,
                                                                           errorText
                                                                       }: HalFormsInputProps<string>): ReactElement => {
    return (<Field
        as={TextField}
        id={prop.name}
        name={prop.name}
        label={prop.prompt || prop.name}
        disabled={prop.readOnly || false}
        fullWidth
        multiline
        rows={4}
        error={!!errorText}
        helperText={errorText}
    />);
}

export const HalFormsInput: React.FC<HalFormsInputProps<string>> = ({
                                                                        prop,
                                                                        errorText
                                                                    }: HalFormsInputProps<string>): ReactElement => {
    return (
        <Field
            as={TextField}
            id={prop.name}
            name={prop.name}
            type={prop.type || "text"}
            label={prop.prompt || prop.name}
            disabled={prop.readOnly || false}
            fullWidth
            error={!!errorText}
            helperText={errorText}
        />
    );
}


