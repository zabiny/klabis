import type {AddressApiDto, HalFormsProperty} from "../api";
import {expandMuiFieldsFactory, type HalFormsInputProps} from "./HalFormsForm";
import React, {type ReactElement} from "react";
import {FormGroup, FormLabel} from "@mui/material";
import {HalFormsInput} from "./HalFormsForm/MuiHalFormsFields";

const AddressDtoField: React.FC<HalFormsInputProps<AddressApiDto>> = (props): ReactElement => {

    function subElementValue(value: Record<string, any>, attrName: string): any {
        return value[attrName];
    }

    function subElementProp(parentProp: HalFormsProperty, attr: string): HalFormsProperty {
        return {
            ...parentProp,
            name: parentProp.name + "." + attr,
            prompt: attr,
            regex: undefined,
            type: 'subprop',
            options: undefined,
            multiple: false,
            value: parentProp.value
        };
    }

    type OnChangeEvent = (attrName: string, value: any) => void;


    function subElementOnChange(parentAttrName: string, onParentChange: OnChangeEvent): OnChangeEvent {
        return (attrName, value) => {
            console.error(`TBD: how to do subelement change? Better to have attrName as dotNotation and have change handled by form - react-hook-forms? ${attrName}=>${value}`)
        };
    }

    function subElementInputProps(attrName: string, parentValue: any, parentProps: HalFormsInputProps<any>): HalFormsInputProps<any> {
        return {
            prop: subElementProp(parentProps.prop, attrName),
            value: subElementValue(parentValue, attrName),
            onValueChanged: subElementOnChange(attrName, parentProps.onValueChanged),
            errorText: undefined
        };
    }


    return <FormGroup>
        <FormLabel>Adresa</FormLabel>
        <HalFormsInput {...subElementInputProps("streetAndNumber", props.value, props)}/>
        <HalFormsInput {...subElementInputProps("city", props.value, props)}/>
    </FormGroup>;

}


export const klabisFieldsFactory = expandMuiFieldsFactory((fieldType: string, conf: HalFormsInputProps<any>): ReactElement | null => {
    switch (fieldType) {
        case "AddressApiDto":
            return <AddressDtoField {...conf}/>;
        case "ContactApiDto":
            return <AddressDtoField {...conf}/>;
        case "LegalGuardians":
            return <AddressDtoField {...conf}/>;
        default:
            return null;
    }
});

