import {type ReactElement} from 'react';
import {type HalFormFieldFactory, type HalFormsInputProps} from '../HalNavigator2/halforms';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {CategoryPresetPickerButton} from './CategoryPresetPickerButton.tsx';

/**
 * Field factory for event create/edit forms.
 * Extends klabisFieldsFactory (which handles range, MemberId, etc.) by adding
 * a "Select from templates" button next to the categories field.
 */
export const eventFormFieldsFactory: HalFormFieldFactory = (
    fieldType: string,
    conf: HalFormsInputProps
): ReactElement | null => {
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
