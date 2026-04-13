import {type ReactElement} from 'react';
import {halFormsFieldsFactory, type HalFormFieldFactory, type HalFormsInputProps} from '../HalNavigator2/halforms';
import {CategoryPresetPickerButton} from './CategoryPresetPickerButton.tsx';

/**
 * Field factory for event create/edit forms.
 * Extends the default factory by adding a "Select from templates" button
 * next to the categories field.
 */
export const eventFormFieldsFactory: HalFormFieldFactory = (
    fieldType: string,
    conf: HalFormsInputProps
): ReactElement | null => {
    const defaultField = halFormsFieldsFactory(fieldType, conf);

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
