import type {AddressApiDto} from "../api";
import {expandMuiFieldsFactory, type HalFormsInputProps} from "./HalFormsForm";
import React, {type ReactElement} from "react";
import {FormGroup, FormLabel} from "@mui/material";
import {HalFormsInput} from "./HalFormsForm/MuiHalFormsFields";

const ContactDtoField: React.FC<HalFormsInputProps<AddressApiDto>> = (props): ReactElement => {


    return <FormGroup>
        <FormLabel>{props.prop.prompt || props.prop.name}</FormLabel>
        <HalFormsInput {...props.subElementProps("email", {prompt: "Email"})}/>
        <HalFormsInput {...props.subElementProps("phone", {prompt: "Telefon"})}/>
        <HalFormsInput {...props.subElementProps("note", {prompt: "Poznamka"})}/>
    </FormGroup>;

}


const AddressDtoField: React.FC<HalFormsInputProps<AddressApiDto>> = (props): ReactElement => {


    return <FormGroup>
        <FormLabel>{props.prop.prompt || props.prop.name}</FormLabel>
        <HalFormsInput {...props.subElementProps("streetAndNumber", {prompt: "Ulice"})}/>
        <HalFormsInput {...props.subElementProps("city", {prompt: "Mesto"})}/>
        <HalFormsInput {...props.subElementProps("postalCode", {prompt: "PSC"})}/>
        <HalFormsInput {...props.subElementProps("country", {prompt: "Stat"})}/>
    </FormGroup>;

}


export const klabisFieldsFactory = expandMuiFieldsFactory((fieldType: string, conf: HalFormsInputProps<any>): ReactElement | null => {
    switch (fieldType) {
        case "AddressApiDto":
            return <AddressDtoField {...conf}/>;
        case "ContactApiDto":
            return <ContactDtoField {...conf}/>;
        case "LegalGuardians":
            return <AddressDtoField {...conf}/>;
        default:
            return null;
    }
});

