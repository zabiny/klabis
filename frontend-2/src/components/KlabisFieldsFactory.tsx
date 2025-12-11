import {expandMuiFieldsFactory, type HalFormsInputProps} from "./HalFormsForm";
import React, {type ReactElement} from "react";
import {Button, FormGroup, FormLabel} from "@mui/material";
import {HalFormsInput} from "./HalFormsForm/MuiHalFormsFields";
import {FieldArray, type FieldArrayRenderProps, useFormikContext} from "formik";

const ContactDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {


    return <FormGroup sx={{border: "1px solid red", padding: 2}}>
        <FormLabel>{props.prop.prompt || props.prop.name}</FormLabel>
        <HalFormsInput {...props.subElementProps("email", {prompt: "Email"})}/>
        <HalFormsInput {...props.subElementProps("phone", {prompt: "Telefon"})}/>
        <HalFormsInput {...props.subElementProps("note", {prompt: "Poznamka"})}/>
    </FormGroup>;

}


const AddressDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {


    return <FormGroup sx={{border: "1px solid red", padding: 2}}>
        <FormLabel>{props.prop.prompt || props.prop.name}</FormLabel>
        <HalFormsInput {...props.subElementProps("streetAndNumber", {prompt: "Ulice"})}/>
        <HalFormsInput {...props.subElementProps("city", {prompt: "Mesto"})}/>
        <HalFormsInput {...props.subElementProps("postalCode", {prompt: "PSC"})}/>
        <HalFormsInput {...props.subElementProps("country", {prompt: "Stat"})}/>
    </FormGroup>;

}

const IdentityCardApiDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroup sx={{border: "1px solid red", padding: 2}}>
        <FormLabel>{props.prop.prompt || props.prop.name}</FormLabel>
        <HalFormsInput {...props.subElementProps("number", {prompt: "Cislo"})}/>
        <HalFormsInput {...props.subElementProps("expiryDate", {prompt: "Platnost do", type: "date"})}/>
    </FormGroup>;
}

const LegalGuardiansField: React.FC<HalFormsInputProps> = ({prop, subElementProps}): ReactElement => {
    const {getFieldMeta} = useFormikContext();

    /// https://formik.org/docs/examples/field-arrays
    const metaVal = getFieldMeta(prop.name).value;
    const fieldValue: object[] = Array.isArray(metaVal) ? metaVal : [];

    // WIP

    return <FormGroup sx={{border: "1px solid red", padding: 2}}>
        <FormLabel>{prop.prompt || prop.name}</FormLabel>
        <FieldArray name={prop.name} render={(arrayHelpers: FieldArrayRenderProps) => (
            <div>
                {fieldValue.map((_, index) => <FormGroup key={index}>
                    <HalFormsInput {...subElementProps("[" + index + "].firstName", {prompt: "Jmeno"})}/>
                    <HalFormsInput {...subElementProps("[" + index + "].lastName", {prompt: "Prijmeni"})}/>
                    <ContactDtoField {...subElementProps("[" + index + "].contact", {prompt: "Kontakt"})}/>
                    <Button onClick={() => arrayHelpers.remove(index)}>Odeber</Button>
                </FormGroup>)}
                <Button onClick={() => arrayHelpers.push({})}>Pridej</Button>
            </div>
        )}/>
    </FormGroup>;
}


export const klabisFieldsFactory = expandMuiFieldsFactory((fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
    switch (fieldType) {
        case "AddressApiDto":
            return <AddressDtoField {...conf}/>;
        case "ContactApiDto":
            return <ContactDtoField {...conf}/>;
        case "LegalGuardiansApiDto":
            return <LegalGuardiansField {...conf}/>;
        case "IdentityCardApiDto":
            return <IdentityCardApiDtoField {...conf}/>;
        default:
            return null;
    }
});

