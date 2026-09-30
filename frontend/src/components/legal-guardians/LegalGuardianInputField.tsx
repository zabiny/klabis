import {type ReactElement, useEffect, useState} from 'react';
import {useField} from 'formik';
import {HalFormsInput, HalFormsMemberId} from '../HalNavigator2/halforms/fields';
import {PillGroup} from '../UI/PillGroup.tsx';
import type {HalFormsInputProps} from '../HalNavigator2/halforms';
import {labels} from '../../localization';

type GuardianValue = Record<string, unknown> | undefined | null;

const NEW_GUARDIAN_FIELDS = ['firstName', 'lastName', 'email', 'phone'] as const;

const hasNewGuardianData = (value: GuardianValue): boolean =>
    NEW_GUARDIAN_FIELDS.some(key => typeof value?.[key] === 'string' && value[key] !== '');

type Mode = 'existing' | 'new';

/**
 * One row of the `legalGuardians` collection: either picks an existing candidate (`{userId}`)
 * or defines a new non-member guardian (`{firstName, lastName, email, phone}`). Options link
 * belongs to the collection property, so the picker takes it from the row's prop, not from sub-elements.
 */
export const LegalGuardianInputField = (conf: HalFormsInputProps): ReactElement => {
    const [field, , helpers] = useField<GuardianValue>(conf.prop.name);
    const [mode, setMode] = useState<Mode>(() => hasNewGuardianData(field.value) ? 'new' : 'existing');

    useEffect(() => {
        if (field.value && field.value.userId === '') {
            const rest = {...field.value};
            delete rest.userId;
            void helpers.setValue(rest);
        }
    }, [field.value, helpers]);

    const switchMode = (next: Mode) => {
        if (next === mode) return;
        setMode(next);
        void helpers.setValue({});
    };

    const userIdProps = conf.subElementProps('userId', {prompt: labels.ui.legalGuardianPickExisting});
    const rowInput = (attr: string, prompt: string, type: string) =>
        <HalFormsInput key={attr} {...conf.subElementProps(attr, {prompt, type})} />;

    return (
        <div className="space-y-3">
            <PillGroup<Mode>
                ariaLabel={conf.prop.prompt || labels.fields.legalGuardianUserId}
                selectedValue={mode}
                onChange={switchMode}
                options={[
                    {value: 'existing', label: labels.ui.legalGuardianPickExisting},
                    {value: 'new', label: labels.ui.legalGuardianCreateNew},
                ]}
            />
            {mode === 'existing' ? (
                <HalFormsMemberId {...userIdProps} renderMode="input"
                                  prop={{...userIdProps.prop, options: conf.prop.options}}/>
            ) : (
                <>
                    {rowInput('firstName', labels.fields.firstName, 'text')}
                    {rowInput('lastName', labels.fields.lastName, 'text')}
                    {rowInput('email', labels.fields.email, 'email')}
                    {rowInput('phone', labels.fields.phone, 'tel')}
                </>
            )}
        </div>
    );
};
