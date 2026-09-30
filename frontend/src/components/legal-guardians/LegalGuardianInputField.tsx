import {type ReactElement, useEffect, useState} from 'react';
import {useField} from 'formik';
import {HalFormsInput} from '../HalNavigator2/halforms/fields';
import {LegalGuardianCandidatePicker} from './LegalGuardianCandidatePicker.tsx';
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

    const modeButtonClass = (active: boolean) =>
        `px-3 py-1 text-sm rounded-md border ${active
            ? 'bg-primary text-white border-primary'
            : 'border-border text-text-secondary hover:text-text-primary'}`;

    return (
        <div className="space-y-3">
            <div className="flex gap-2" role="group">
                <button type="button" aria-pressed={mode === 'existing'}
                        className={modeButtonClass(mode === 'existing')}
                        onClick={() => switchMode('existing')}>
                    {labels.ui.legalGuardianPickExisting}
                </button>
                <button type="button" aria-pressed={mode === 'new'}
                        className={modeButtonClass(mode === 'new')}
                        onClick={() => switchMode('new')}>
                    {labels.ui.legalGuardianCreateNew}
                </button>
            </div>
            {mode === 'existing' ? (
                <LegalGuardianCandidatePicker {...userIdProps} renderMode="input"
                                              prop={{...userIdProps.prop, options: conf.prop.options}}/>
            ) : (
                <>
                    {rowInput('firstName', labels.ui.legalGuardianFirstName, 'text')}
                    {rowInput('lastName', labels.ui.legalGuardianLastName, 'text')}
                    {rowInput('email', labels.ui.legalGuardianEmail, 'email')}
                    {rowInput('phone', labels.ui.legalGuardianPhone, 'tel')}
                </>
            )}
        </div>
    );
};
