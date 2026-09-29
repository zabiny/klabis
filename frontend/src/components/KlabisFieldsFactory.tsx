import {
    type CustomFieldFactory,
    expandHalFormsFieldFactory,
    type HalFormFieldFactory,
    type HalFormsInputProps
} from "./HalNavigator2/halforms";
import {isMultipleProperty} from "./HalNavigator2/halforms/utils";
import {type ReactElement} from "react";
import {HalFormsCheckboxGroup, HalFormsInput, HalFormsMemberId, HalFormsSelect} from "./HalNavigator2/halforms/fields";
import {DetailRow} from "./UI";
import {FormGroupWrapper} from "./FormGroupWrapper";
import {getFieldLabel} from "../localization";
import {useEventTypes} from "../hooks/useEventTypes";
import {useMembershipFeeTierOptions} from "../hooks/useMembershipFeeTierOptions";

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

const AGE_RANGE_FIELDS: SubField[] = [
    {key: "minAge", attr: "minAge", prompt: "Min. věk", type: "number"},
    {key: "maxAge", attr: "maxAge", prompt: "Max. věk", type: "number"},
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

// eslint-disable-next-line react-refresh/only-export-components
const MembershipFeeTierMultiSelectField = (conf: HalFormsInputProps): ReactElement => {
    const tierOptions = useMembershipFeeTierOptions();
    const propWithTierOptions = {...conf.prop, options: {inline: tierOptions}};
    return <HalFormsCheckboxGroup {...conf} prop={propWithTierOptions}/>;
};

const changeTypeOfProperty = (prop: HalFormsInputProps, newType: string): HalFormsInputProps => {
    return {
        ...prop,
        prop: {...prop.prop, type: newType}
    } as HalFormsInputProps;
}

/**
 * Field types whose value identifies a person and therefore renders as the member picker.
 * "MemberId" is a club member; "UserId" is any user of the system (a family group parent need not
 * have a member profile) — both get their options from listMemberOptions today, since user and
 * member identities share one UUID.
 */
const MEMBER_PICKER_FIELD_TYPES = ['MemberId', 'UserId'];

const isMemberPickerFieldType = (fieldType: string): boolean => MEMBER_PICKER_FIELD_TYPES.includes(fieldType);

/**
 * Renders a single member/user picker field — read/write dropdown or, when readOnly, the resolved
 * member name. Used both for a standalone field and as the per-row renderer when the
 * property is multi: the base factory's `multi` branch detects that this widget exists for
 * the field type and routes the array through HalFormsCollectionField (D7), which recurses back
 * here per row with multiple:false — so this never needs to special-case collections itself.
 *
 * Only the explicit "MemberId"/"UserId" field type hint triggers the picker (D7) — a plain "UUID"
 * field is NOT assumed to be a person reference (it renders by its options/basic type
 * instead), since that assumption is exactly what caused multi UUID fields with link options
 * (coordinators, disciplineIds) to silently render as a single select.
 */
const memberIdFieldRenderer = (conf: HalFormsInputProps, extraProps?: {excludeIds?: string[]; includeIds?: string[]}): ReactElement => {
    // If backend already provides inline options, respect them instead of the member picker
    if (conf.prop.options?.inline) {
        return <HalFormsSelect {...conf} />;
    }
    // Backend always provides options.link for member-picker fields; trust it as-is.
    return <HalFormsMemberId {...conf} {...extraProps}/>;
};

export const klabisCustomFieldFactory: CustomFieldFactory = (fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
    switch (fieldType) {
        case "range": return <HalFormsInput {...changeTypeOfProperty(conf, 'text')}/>;
        case "MemberId":
        case "UserId": {
            return memberIdFieldRenderer(conf);
        }
        case "RankingRequest":
            return renderCompositeField(conf, [
                {key: "levelId", attr: "levelId", prompt: "ID žebříčku", type: "number"},
                {key: "shortName", attr: "shortName", prompt: "Zkratka"},
                {key: "name", attr: "name", prompt: "Název"},
            ]);
        case "MembershipFeeTierMultiSelect":
            return <MembershipFeeTierMultiSelectField {...conf}/>;
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
        case "AgeRangeRequest":
            return renderCompositeField(conf, AGE_RANGE_FIELDS);
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
};

export const klabisFieldsFactory = expandHalFormsFieldFactory(klabisCustomFieldFactory);

/**
 * Creates a variant of klabisFieldsFactory that applies member-ID filtering.
 * Use when the caller already holds the group's current member/owner list and wants
 * to prevent the user from picking someone already in the group. Both picker field types
 * are filtered — a parent is identified by user id, a child by member id, and the two
 * share one UUID, so the same already-in-group list covers both.
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
        if (isMemberPickerFieldType(fieldType)) {
            return memberIdFieldRenderer(conf, {excludeIds, includeIds});
        }
        return null;
    });
};
