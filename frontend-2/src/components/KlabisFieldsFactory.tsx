import type {AddressApiDto, HalFormsProperty} from "../api";
import {expandMuiFieldsFactory, type HalFormsInputProps} from "./HalFormsForm";
import React, {type ReactElement} from "react";
import {FormGroup, FormLabel} from "@mui/material";
import {HalFormsInput} from "./HalFormsForm/MuiHalFormsFields";

type OnChangeEvent = (attrName: string, value: any) => void;


function subElementInputProps(attrName: string, parentValue: any, parentProps: HalFormsInputProps<any>, label?: string): HalFormsInputProps<any> {
    function subElementValue(value: Record<string, any>, attrName: string): any {
        return value[attrName];
    }

    function subElementProp(parentProp: HalFormsProperty, attr: string, label: string = attr): HalFormsProperty {
        return {
            ...parentProp,
            name: parentProp.name + "." + attr,
            prompt: label,
            regex: undefined,
            type: 'text',
            options: undefined,
            multiple: false,
            value: parentProp.value
        };
    }

    function subElementOnChange(parentAttrName: string, onParentChange: OnChangeEvent): OnChangeEvent {
        return (attrName, value) => {
            console.error(`TBD: how to do subelement change? Better to have attrName as dotNotation and have change handled by form - react-hook-forms? ${attrName}=>${value}`)
        };
    }

    return {
        prop: subElementProp(parentProps.prop, attrName, label),
        value: subElementValue(parentValue, attrName),
        onValueChanged: subElementOnChange(attrName, parentProps.onValueChanged),
        errorText: undefined
    };
}


const ContactDtoField: React.FC<HalFormsInputProps<AddressApiDto>> = (props): ReactElement => {


    return <FormGroup>
        <FormLabel>{props.prop.prompt || props.prop.name}</FormLabel>
        <HalFormsInput {...subElementInputProps("email", props.value, props, "Email")}/>
        <HalFormsInput {...subElementInputProps("phone", props.value, props, "Telefon")}/>
        <HalFormsInput {...subElementInputProps("note", props.value, props, "Poznamka")}/>
    </FormGroup>;

}


const AddressDtoField: React.FC<HalFormsInputProps<AddressApiDto>> = (props): ReactElement => {


    return <FormGroup>
        <FormLabel>{props.prop.prompt || props.prop.name}</FormLabel>
        <HalFormsInput {...subElementInputProps("streetAndNumber", props.value, props, "Ulice")}/>
        <HalFormsInput {...subElementInputProps("city", props.value, props, "Mesto")}/>
        <HalFormsInput {...subElementInputProps("postalCode", props.value, props, "PSC")}/>
        <HalFormsInput {...subElementInputProps("country", props.value, props, "Stat")}/>
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

