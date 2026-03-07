import {expandHalFormsFieldFactory, type HalFormsInputProps} from "./HalNavigator2/halforms";
import React, {type ReactElement} from "react";
import {HalFormsInput, HalFormsMemberId} from "./HalNavigator2/halforms/fields";

const FormGroupWrapper: React.FC<{ label: string; children: ReactElement | ReactElement[] }> = ({label, children}) => (
    <div className="border-2 border-red-500 rounded p-4 mb-4">
        <label className="block text-sm font-semibold mb-2">{label}</label>
        <div className="space-y-3">{children}</div>
    </div>
);

const AddressDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="street" {...props.subElementProps("street", {prompt: "Ulice"})} />,
            <HalFormsInput key="city" {...props.subElementProps("city", {prompt: "Město"})} />,
            <HalFormsInput key="postal" {...props.subElementProps("postalCode", {prompt: "PSČ"})} />,
            <HalFormsInput key="country" {...props.subElementProps("country", {prompt: "Stát"})} />,
        ]}
    </FormGroupWrapper>;
}

const IdentityCardDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="cardNumber" {...props.subElementProps("cardNumber", {prompt: "Číslo OP"})} />,
            <HalFormsInput key="validityDate" {...props.subElementProps("validityDate", {
                prompt: "Platnost OP",
                type: "date"
            })} />,
        ]}
    </FormGroupWrapper>;
}

const GuardianDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="firstName" {...props.subElementProps("firstName", {prompt: "Jméno"})} />,
            <HalFormsInput key="lastName" {...props.subElementProps("lastName", {prompt: "Příjmení"})} />,
            <HalFormsInput key="relationship" {...props.subElementProps("relationship", {prompt: "Vztah"})} />,
            <HalFormsInput key="email" {...props.subElementProps("email", {prompt: "E-mail", type: "email"})} />,
            <HalFormsInput key="phone" {...props.subElementProps("phone", {prompt: "Telefon", type: "tel"})} />,
        ]}
    </FormGroupWrapper>;
}

const MedicalCourseDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="completionDate" {...props.subElementProps("completionDate", {prompt: "Datum absolvování", type: "date"})} />,
            <HalFormsInput key="validityDate" {...props.subElementProps("validityDate", {prompt: "Platnost", type: "date"})} />,
        ]}
    </FormGroupWrapper>;
}

const TrainerLicenseDtoField: React.FC<HalFormsInputProps> = (props): ReactElement => {
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {[
            <HalFormsInput key="licenseNumber" {...props.subElementProps("licenseNumber", {prompt: "Číslo licence"})} />,
            <HalFormsInput key="validityDate" {...props.subElementProps("validityDate", {prompt: "Platnost", type: "date"})} />,
        ]}
    </FormGroupWrapper>;
}

const changeTypeOfProperty = (prop: HalFormsInputProps, newType: string): HalFormsInputProps => {
    return {
        ...prop,
        prop: {...prop.prop, type: newType}
    } as HalFormsInputProps;
}

export const klabisFieldsFactory = expandHalFormsFieldFactory((fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
    switch (fieldType) {
        case "range": return <HalFormsInput {...changeTypeOfProperty(conf, 'text')}/>;
        case "MemberId": {
            const propWithOptions = {
                ...conf.prop,
                options: {
                    link: {
                        href: "/members/options"
                    }
                }
            };
            return <HalFormsMemberId {...conf} prop={propWithOptions}/>;
        }
        case "AddressRequest":
            return <AddressDtoField {...conf}/>;
        case "GuardianDTO":
            return <GuardianDtoField {...conf}/>;
        case "IdentityCardDto":
            return <IdentityCardDtoField {...conf}/>;
        case "MedicalCourseDto":
            return <MedicalCourseDtoField {...conf}/>;
        case "TrainerLicenseDto":
            return <TrainerLicenseDtoField {...conf}/>;
        default:
            return null;
    }
});
