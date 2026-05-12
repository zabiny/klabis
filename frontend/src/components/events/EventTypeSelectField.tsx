import type {ReactElement} from 'react';
import {Field} from 'formik';
import type {FieldProps} from 'formik';
import {SelectField} from '../UI/forms';
import type {HalFormsInputProps} from '../HalNavigator2/halforms/types.ts';
import {useEventTypes} from '../../hooks/useEventTypes.ts';
import {labels} from '../../localization/labels.ts';

/**
 * A Formik-integrated dropdown for the eventTypeId field in the event create/update form.
 * Loads options from the cached /api/event-types catalog; first option is "—" (not set).
 */
export function EventTypeSelectField({prop, renderMode = 'field'}: HalFormsInputProps): ReactElement {
    const {eventTypes, isLoading} = useEventTypes();

    const options = isLoading
        ? []
        : [{value: '', label: '—'}, ...eventTypes.map((t) => ({value: t.id, label: t.name}))];

    return (
        <Field name={prop.name} validate={() => undefined}>
            {({field}: FieldProps<unknown>) => {
                const fieldValue = (field.value as string | null | undefined) ?? '';
                return (
                    <SelectField
                        {...field}
                        value={fieldValue}
                        label={renderMode === 'field' ? (prop.prompt || labels.fields.eventTypeId) : undefined}
                        placeholder={isLoading ? labels.ui.loading : undefined}
                        disabled={prop.readOnly || isLoading || false}
                        required={prop.required}
                        options={options}
                        className="w-full"
                    />
                );
            }}
        </Field>
    );
}
