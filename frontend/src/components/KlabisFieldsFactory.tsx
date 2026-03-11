import {expandHalFormsFieldFactory, type HalFormsInputProps} from "./HalNavigator2/halforms";
import React, {type ReactElement} from "react";
import {HalFormsInput, HalFormsMemberId, HalFormsSelect} from "./HalNavigator2/halforms/fields";
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
    {key: "completionDate", attr: "completionDate", prompt: "Datum absolvování kurzu", type: "date"},
    {key: "validityDate", attr: "validityDate", prompt: "Platnost", type: "date"},
];

const TRAINER_LEVEL_OPTIONS = [
    {value: "T1", prompt: "T1"},
    {value: "T2", prompt: "T2"},
    {value: "T3", prompt: "T3"},
];

const REFEREE_LEVEL_OPTIONS = [
    {value: "R1", prompt: "R1"},
    {value: "R2", prompt: "R2"},
    {value: "R3", prompt: "R3"},
];

const renderLicenseField = (conf: HalFormsInputProps, levelOptions: {value: string; prompt: string}[], title: string): ReactElement => {
    const levelSubProps = conf.subElementProps("level", {prompt: "Stupeň"});
    const levelPropWithOptions = {...levelSubProps.prop, options: {inline: levelOptions}};
    const validitySubProps = conf.subElementProps("validityDate", {prompt: "Platnost", type: "date"});

    if (conf.renderMode === 'input') {
        return <>
            <DetailRow key="level" label="Stupeň">
                <HalFormsSelect {...levelSubProps} prop={levelPropWithOptions}/>
            </DetailRow>
            <DetailRow key="validityDate" label="Platnost">
                <HalFormsInput {...validitySubProps}/>
            </DetailRow>
        </>;
    }
    return <FormGroupWrapper label={title}>
        <HalFormsSelect key="level" {...levelSubProps} prop={levelPropWithOptions}/>
        <HalFormsInput key="validityDate" {...validitySubProps}/>
    </FormGroupWrapper>;
};

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
        case "Gender": {
            const propWithGenderOptions = {
                ...conf.prop,
                options: {
                    inline: [
                        {value: "MALE", prompt: "Muž"},
                        {value: "FEMALE", prompt: "Žena"},
                    ]
                }
            };
            return <HalFormsSelect {...conf} prop={propWithGenderOptions}/>;
        }
        case "DrivingLicenseGroup": {
            const propWithDrivingOptions = {
                ...conf.prop,
                options: {
                    inline: [
                        {value: "AM", prompt: "AM"},
                        {value: "A1", prompt: "A1"},
                        {value: "A2", prompt: "A2"},
                        {value: "A", prompt: "A"},
                        {value: "B", prompt: "B"},
                        {value: "BE", prompt: "BE"},
                        {value: "C", prompt: "C"},
                        {value: "C1", prompt: "C1"},
                        {value: "D", prompt: "D"},
                        {value: "D1", prompt: "D1"},
                        {value: "T", prompt: "T"},
                    ]
                }
            };
            return <HalFormsSelect {...conf} prop={propWithDrivingOptions}/>;
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
            return renderLicenseField(conf, TRAINER_LEVEL_OPTIONS, conf.prop.prompt || "Trenérská licence");
        case "RefereeLicenseDto":
            return renderLicenseField(conf, REFEREE_LEVEL_OPTIONS, conf.prop.prompt || "Rozhodcovská licence");
        default:
            return null;
    }
});
