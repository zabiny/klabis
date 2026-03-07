import {expandHalFormsFieldFactory, type HalFormsInputProps} from "./HalNavigator2/halforms";
import React, {type ReactElement} from "react";
import {HalFormsInput, HalFormsMemberId} from "./HalNavigator2/halforms/fields";
import {DetailRow} from "./UI";

const FormGroupWrapper: React.FC<{ label: string; children: ReactElement | ReactElement[] }> = ({label, children}) => (
    <div className="rounded p-4 mb-4">
        <label className="block text-sm font-semibold mb-2">{label}</label>
        <div className="space-y-3">{children}</div>
    </div>
);

interface SubField {
    key: string;
    attr: string;
    prompt: string;
    type?: string;
}

const renderCompositeField = (props: HalFormsInputProps, subFields: SubField[]): ReactElement => {
    if (props.renderMode === 'input') {
        return <>
            {subFields.map(sf => (
                <DetailRow key={sf.key} label={sf.prompt}>
                    <HalFormsInput {...props.subElementProps(sf.attr, {prompt: sf.prompt, type: sf.type})} />
                </DetailRow>
            ))}
        </>;
    }
    return <FormGroupWrapper label={props.prop.prompt || props.prop.name}>
        {subFields.map(sf => (
            <HalFormsInput key={sf.key} {...props.subElementProps(sf.attr, {prompt: sf.prompt, type: sf.type})} />
        ))}
    </FormGroupWrapper>;
};

const ADDRESS_FIELDS: SubField[] = [
    {key: "street", attr: "street", prompt: "Ulice"},
    {key: "city", attr: "city", prompt: "Město"},
    {key: "postal", attr: "postalCode", prompt: "PSČ"},
    {key: "country", attr: "country", prompt: "Stát"},
];

const IDENTITY_CARD_FIELDS: SubField[] = [
    {key: "cardNumber", attr: "cardNumber", prompt: "Číslo OP"},
    {key: "validityDate", attr: "validityDate", prompt: "Platnost OP", type: "date"},
];

const GUARDIAN_FIELDS: SubField[] = [
    {key: "firstName", attr: "firstName", prompt: "Jméno"},
    {key: "lastName", attr: "lastName", prompt: "Příjmení"},
    {key: "relationship", attr: "relationship", prompt: "Vztah"},
    {key: "email", attr: "email", prompt: "E-mail", type: "email"},
    {key: "phone", attr: "phone", prompt: "Telefon", type: "tel"},
];

const MEDICAL_COURSE_FIELDS: SubField[] = [
    {key: "completionDate", attr: "completionDate", prompt: "Datum absolvování", type: "date"},
    {key: "validityDate", attr: "validityDate", prompt: "Platnost", type: "date"},
];

const TRAINER_LICENSE_FIELDS: SubField[] = [
    {key: "licenseNumber", attr: "licenseNumber", prompt: "Číslo licence"},
    {key: "validityDate", attr: "validityDate", prompt: "Platnost", type: "date"},
];

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
            return renderCompositeField(conf, ADDRESS_FIELDS);
        case "GuardianDTO":
            return renderCompositeField(conf, GUARDIAN_FIELDS);
        case "IdentityCardDto":
            return renderCompositeField(conf, IDENTITY_CARD_FIELDS);
        case "MedicalCourseDto":
            return renderCompositeField(conf, MEDICAL_COURSE_FIELDS);
        case "TrainerLicenseDto":
            return renderCompositeField(conf, TRAINER_LICENSE_FIELDS);
        default:
            return null;
    }
});
