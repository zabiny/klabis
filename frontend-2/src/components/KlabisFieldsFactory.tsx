import {expandHalFormsFieldFactory, type HalFormsInputProps} from "./HalFormsForm";
import React, {type ReactElement} from "react";
import {HalFormsInput} from "./HalFormsForm/fields";
import {FieldArray, type FieldArrayRenderProps, useFormikContext} from "formik";
import {Button} from "./UI";

const FormGroupWrapper: React.FC<{ label: string; children: ReactElement | ReactElement[] }> = ({label, children}) => (
    <div className="border-2 border-red-500 rounded p-4 mb-4">
        <label className="block text-sm font-semibold mb-2">{label}</label>
        <div className="space-y-3">{children}</div>
    </div>
);

const ContactDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="email" {...props.subElementProps("email", {prompt: "Email"})} />,
            <HalFormsInput key="phone" {...props.subElementProps("phone", {prompt: "Telefon"})} />,
            <HalFormsInput key="note" {...props.subElementProps("note", {prompt: "Poznamka"})} />,
        ]}
    </FormGroupWrapper>;
}


const AddressDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="street" {...props.subElementProps("streetAndNumber", {prompt: "Ulice"})} />,
            <HalFormsInput key="city" {...props.subElementProps("city", {prompt: "Mesto"})} />,
            <HalFormsInput key="postal" {...props.subElementProps("postalCode", {prompt: "PSC"})} />,
            <HalFormsInput key="country" {...props.subElementProps("country", {prompt: "Stat"})} />,
        ]}
    </FormGroupWrapper>;
}

const IdentityCardApiDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="number" {...props.subElementProps("number", {prompt: "Cislo"})} />,
            <HalFormsInput key="expiry" {...props.subElementProps("expiryDate", {
                prompt: "Platnost do",
                type: "date"
            })} />,
        ]}
    </FormGroupWrapper>;
}

const LegalGuardiansField: React.FC<HalFormsInputProps> = ({prop, subElementProps}): ReactElement => {
    const {getFieldMeta} = useFormikContext();

    /// https://formik.org/docs/examples/field-arrays
    const metaVal = getFieldMeta(prop.name).value;
    const fieldValue: object[] = Array.isArray(metaVal) ? metaVal : [];

    // WIP

    return <FormGroupWrapper label={prop.prompt || prop.name}>
        <FieldArray name={prop.name} render={(arrayHelpers: FieldArrayRenderProps) => (
            <div className="space-y-3">
                {fieldValue.map((_, index) => (
                    <div key={index} className="border border-gray-300 rounded p-3 space-y-3">
                        <HalFormsInput {...subElementProps("[" + index + "].firstName", {prompt: "Jmeno"})} />
                        <HalFormsInput {...subElementProps("[" + index + "].lastName", {prompt: "Prijmeni"})} />
                        <ContactDtoField {...subElementProps("[" + index + "].contact", {prompt: "Kontakt"})} />
                        <Button variant="danger" size="sm" onClick={() => arrayHelpers.remove(index)}>Odeber</Button>
                    </div>
                ))}
                <Button variant="primary" size="sm" onClick={() => arrayHelpers.push({})}>Pridej</Button>
            </div>
        )}/>
    </FormGroupWrapper>;
}


export const klabisFieldsFactory = expandHalFormsFieldFactory((fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
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

