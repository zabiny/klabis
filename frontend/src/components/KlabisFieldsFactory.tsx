import {expandHalFormsFieldFactory, type HalFormFieldFactory, type HalFormsInputProps} from "./HalNavigator2/halforms";
import {isMultipleProperty} from "./HalNavigator2/halforms/utils";
import {type ReactElement} from "react";
import {HalFormsCheckboxGroup, HalFormsInput, HalFormsMemberId, HalFormsSelect} from "./HalNavigator2/halforms/fields";
import {DetailRow} from "./UI";
import {FormGroupWrapper} from "./FormGroupWrapper";
import {getFieldLabel} from "../localization";
import {useEventTypes} from "../hooks/useEventTypes";

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
    const baseName = props.prop.name.replace(/\.\d+$/, '');
    return <FormGroupWrapper label={props.prop.prompt || getFieldLabel(baseName)}>
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

const RULE_TYPE_OPTIONS = [
    {value: "PERCENTAGE", prompt: "Procentuální (% ze startovného)"},
    {value: "FIXED_AMOUNT", prompt: "Fixní částka (Kč)"},
];

// eslint-disable-next-line react-refresh/only-export-components
const PaymentRuleFormFields = (conf: HalFormsInputProps): ReactElement => {
    const {eventTypes} = useEventTypes();
    const eventTypeOptions = eventTypes.map(et => ({value: et.id, prompt: et.name}));

    const eventTypeSubProps = conf.subElementProps("eventTypeId", {prompt: "Typ akce"});
    const eventTypePropWithOptions = {...eventTypeSubProps.prop, options: {inline: eventTypeOptions}};

    const ruleTypeSubProps = conf.subElementProps("ruleType", {prompt: "Typ pravidla"});
    const ruleTypePropWithOptions = {...ruleTypeSubProps.prop, options: {inline: RULE_TYPE_OPTIONS}};

    const rankingSubProps = conf.subElementProps("rankingShortName", {prompt: "Zkratka žebříčku"});
    const percentSubProps = conf.subElementProps("percent", {prompt: "Procento (%)", type: "number"});
    const fixedAmountSubProps = conf.subElementProps("fixedAmount", {prompt: "Fixní částka", type: "number"});
    const fixedCurrencySubProps = conf.subElementProps("fixedCurrency", {prompt: "Měna fixní částky"});

    if (conf.renderMode === 'input') {
        return <>
            <DetailRow label="Typ akce">
                <HalFormsSelect {...eventTypeSubProps} prop={eventTypePropWithOptions}/>
            </DetailRow>
            <DetailRow label="Zkratka žebříčku">
                <HalFormsInput {...rankingSubProps}/>
            </DetailRow>
            <DetailRow label="Typ pravidla">
                <HalFormsSelect {...ruleTypeSubProps} prop={ruleTypePropWithOptions}/>
            </DetailRow>
            <DetailRow label="Procento (%)">
                <HalFormsInput {...percentSubProps}/>
            </DetailRow>
            <DetailRow label="Fixní částka">
                <HalFormsInput {...fixedAmountSubProps}/>
            </DetailRow>
            <DetailRow label="Měna fixní částky">
                <HalFormsInput {...fixedCurrencySubProps}/>
            </DetailRow>
        </>;
    }

    const baseName = conf.prop.name.replace(/\.\d+$/, '');
    return <FormGroupWrapper label={conf.prop.prompt || getFieldLabel(baseName)}>
        <HalFormsSelect {...eventTypeSubProps} prop={eventTypePropWithOptions}/>
        <HalFormsInput {...rankingSubProps}/>
        <HalFormsSelect {...ruleTypeSubProps} prop={ruleTypePropWithOptions}/>
        <HalFormsInput {...percentSubProps}/>
        <HalFormsInput {...fixedAmountSubProps}/>
        <HalFormsInput {...fixedCurrencySubProps}/>
    </FormGroupWrapper>;
};

const changeTypeOfProperty = (prop: HalFormsInputProps, newType: string): HalFormsInputProps => {
    return {
        ...prop,
        prop: {...prop.prop, type: newType}
    } as HalFormsInputProps;
}

const memberIdFieldRenderer = (conf: HalFormsInputProps, extraProps?: {excludeIds?: string[]; includeIds?: string[]}): ReactElement => {
    // If backend already provides inline options, respect them instead of defaulting to members list
    if (conf.prop.options?.inline) {
        if (isMultipleProperty(conf.prop)) {
            return <HalFormsCheckboxGroup {...conf} />;
        }
        return <HalFormsSelect {...conf} />;
    }
    const propWithMemberOptions = {
        ...conf.prop,
        options: {
            link: {
                href: "/members/options"
            }
        }
    };
    if (isMultipleProperty(conf.prop)) {
        return <HalFormsCheckboxGroup {...conf} prop={propWithMemberOptions}/>;
    }
    return <HalFormsMemberId {...conf} prop={propWithMemberOptions} {...extraProps}/>;
};

export const klabisFieldsFactory = expandHalFormsFieldFactory((fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
    switch (fieldType) {
        case "range": return <HalFormsInput {...changeTypeOfProperty(conf, 'text')}/>;
        case "List": {
            const propWithMemberOptions = {
                ...conf.prop,
                options: {
                    link: {
                        href: "/members/options"
                    }
                }
            };
            return <HalFormsCheckboxGroup {...conf} prop={propWithMemberOptions}/>;
        }
        case "MemberId":
        case "UUID": {
            return memberIdFieldRenderer(conf);
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
        case "DeactivationReason": {
            const propWithDeactivationOptions = {
                ...conf.prop,
                options: {
                    inline: [
                        {value: "ODHLASKA", prompt: "Odhlášení"},
                        {value: "PRESTUP", prompt: "Přestup"},
                        {value: "OTHER", prompt: "Jiný důvod"},
                    ]
                }
            };
            return <HalFormsSelect {...conf} prop={propWithDeactivationOptions}/>;
        }
        case "RankingRequest":
            return renderCompositeField(conf, [
                {key: "levelId", attr: "levelId", prompt: "ID žebříčku", type: "number"},
                {key: "shortName", attr: "shortName", prompt: "Zkratka"},
                {key: "name", attr: "name", prompt: "Název"},
            ]);
        case "PaymentRuleRequest":
            // For multi/collection: return null so HalFormsCollectionField handles iteration.
            // For a single item (inside the collection): render sub-fields with custom select renderers.
            if (isMultipleProperty(conf.prop)) return null;
            return <PaymentRuleFormFields {...conf}/>;
        case "EntryFeeRequest":
            return renderCompositeField(conf, [
                {key: "amount", attr: "amount", prompt: "Částka", type: "number"},
                {key: "currency", attr: "currency", prompt: "Měna"},
            ]);
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

/**
 * Creates a variant of klabisFieldsFactory that applies member-ID filtering.
 * Use when the caller already holds the group's current member/owner list and wants
 * to prevent the user from picking someone already in the group.
 *
 * @param excludeIds - IDs to hide from the picker (already-in-group members)
 * @param includeIds - When set, only these IDs are shown (whitelist for promote-to-owner)
 */
export const createMemberFilteredFactory = (
    excludeIds?: string[],
    includeIds?: string[]
): HalFormFieldFactory => {
    const hasFilter = (excludeIds && excludeIds.length > 0) || includeIds !== undefined;
    if (!hasFilter) return klabisFieldsFactory;

    return expandHalFormsFieldFactory((fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
        if (fieldType === 'MemberId' || fieldType === 'UUID') {
            return memberIdFieldRenderer(conf, {excludeIds, includeIds});
        }
        return null;
    });
};
