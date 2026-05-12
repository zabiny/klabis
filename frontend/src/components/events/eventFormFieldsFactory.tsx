import {type ReactElement} from 'react';
import {type HalFormFieldFactory, type HalFormsInputProps} from '../HalNavigator2/halforms';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {CategoryPresetPickerButton} from './CategoryPresetPickerButton.tsx';
import {EventTypeSelectField} from './EventTypeSelectField.tsx';

/**
 * Field factory for event create/edit forms.
 * Extends klabisFieldsFactory (which handles range, MemberId, etc.) by adding:
 * - "Select from templates" button next to the categories field
 * - Event type dropdown loaded from the /api/event-types catalog
 */
export const eventFormFieldsFactory: HalFormFieldFactory = (
    fieldType: string,
    conf: HalFormsInputProps
): ReactElement | null => {
    if (conf.prop.name === 'eventTypeId') {
        return <EventTypeSelectField {...conf}/>;
    }

    const defaultField = klabisFieldsFactory(fieldType, conf);

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
