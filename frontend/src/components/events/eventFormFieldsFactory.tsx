import {type ReactElement} from 'react';
import {useField} from 'formik';
import {expandHalFormsFieldFactory, type CustomFieldFactory, type HalFormFieldFactory, type HalFormsInputProps} from '../HalNavigator2/halforms';
import {klabisCustomFieldFactory} from '../KlabisFieldsFactory.tsx';
import {CategoryPresetPickerButton} from './CategoryPresetPickerButton.tsx';
import {EventTypeSelectField} from './EventTypeSelectField.tsx';
import {TextField} from '../UI/forms';
import {labels} from '../../localization';

const DEFAULT_CURRENCY = 'CZK';

interface MoneyAmount {
    amount: number | string;
    currency: string;
}

interface CategoryRowValue {
    id?: string;
    name?: string;
    fee?: MoneyAmount | null;
}

/**
 * Renders one row of the "categories" collection field: name + optional fee.
 * The category's `id` is carried in Formik state but never surfaced as an input —
 * it must round-trip unchanged so the backend keeps updating the same category
 * (matching by id) instead of deleting and recreating it, which would unlink registrations.
 */
// eslint-disable-next-line react-refresh/only-export-components
const CategoryRowField = ({prop}: HalFormsInputProps): ReactElement => {
    const [field, , helpers] = useField<CategoryRowValue>(prop.name);
    const value = field.value ?? {};

    const handleNameChange = (name: string) => {
        helpers.setValue({...value, name});
    };

    const handleFeeAmountChange = (amount: string) => {
        if (amount === '') {
            helpers.setValue({...value, fee: null});
            return;
        }
        helpers.setValue({...value, fee: {amount, currency: value.fee?.currency || DEFAULT_CURRENCY}});
    };

    return (
        <div className="flex items-start gap-2">
            <div className="flex-1">
                <TextField
                    name={`${prop.name}.name`}
                    value={value.name ?? ''}
                    onChange={(e) => handleNameChange(e.target.value)}
                    onBlur={field.onBlur}
                    label={labels.fields.categories}
                    required
                    className="w-full"
                />
            </div>
            <div className="flex-1">
                <TextField
                    name={`${prop.name}.fee.amount`}
                    type="number"
                    value={value.fee?.amount ?? ''}
                    onChange={(e) => handleFeeAmountChange(e.target.value)}
                    onBlur={field.onBlur}
                    label={labels.fields.categoryFee}
                    className="w-full"
                />
            </div>
        </div>
    );
};

/**
 * Custom field logic for event create/edit forms, composed with klabisCustomFieldFactory
 * via expandHalFormsFieldFactory: event type dropdown, and category row rendering
 * (name + optional fee, hidden id preserved). The `multi` branch in halFormsFieldsFactory
 * always routes the "categories" array to HalFormsCollectionField before this runs, so
 * this only ever sees a single row (multiple:false) for CategoryRequest — no guard needed.
 */
const eventCustomFieldFactory: CustomFieldFactory = (fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
    if (conf.prop.name === 'eventTypeId') {
        return <EventTypeSelectField {...conf}/>;
    }

    if (fieldType === 'CategoryRequest') {
        return <CategoryRowField {...conf}/>;
    }

    return klabisCustomFieldFactory(fieldType, conf);
};

const eventFieldsFactory = expandHalFormsFieldFactory(eventCustomFieldFactory);

/**
 * Field factory for event create/edit forms.
 * Extends klabisFieldsFactory (which handles range, MemberId, etc.) by adding:
 * - "Select from templates" button next to the categories field
 * - Event type dropdown loaded from the /api/event-types catalog
 * - Row rendering for each category item (name + optional fee, hidden id preserved)
 */
export const eventFormFieldsFactory: HalFormFieldFactory = (
    fieldType: string,
    conf: HalFormsInputProps
): ReactElement | null => {
    const defaultField = eventFieldsFactory(fieldType, conf);

    if (conf.prop.name === 'categories' && defaultField !== null) {
        return (
            <div>
                <div className="flex items-center justify-end mb-2">
                    <CategoryPresetPickerButton/>
                </div>
                {defaultField}
            </div>
        );
    }

    return defaultField;
};
