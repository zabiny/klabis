import {type ReactElement, useState} from 'react';
import {Field, type FieldProps} from 'formik';
import {SelectField, TextField} from '../UI/forms';
import {useHalFormOptions} from '../../hooks/useHalFormOptions.ts';
import type {HalFormsInputProps} from '../HalNavigator2/halforms';
import {ClearSelectionButton} from '../HalNavigator2/halforms/fields/ClearSelectionButton.tsx';
import {getFieldLabel, labels} from '../../localization';

interface CandidateOption {
    value: string;
    prompt?: string;
    kind?: 'MEMBER' | 'LEGAL_GUARDIAN';
    registrationNumber?: string;
    email?: string;
}

const LEGAL_GUARDIAN_OPTIONS_PATH = '/legal-guardian-options';

export const isLegalGuardianOptionsLink = (href: string | undefined): boolean =>
    !!href && href.split(/[?{#]/)[0].endsWith(LEGAL_GUARDIAN_OPTIONS_PATH);

const describe = (candidate: CandidateOption): string => {
    const name = candidate.prompt ?? candidate.value;
    const detail = candidate.kind === 'MEMBER' ? candidate.registrationNumber : candidate.email;
    return detail ? `${name} (${detail})` : name;
};

const normalize = (text: string): string =>
    text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

/** Every typed word has to appear in the candidate's name or detail. */
const matches = (label: string, query: string): boolean => {
    const haystack = normalize(label);
    return normalize(query).split(/\s+/).filter(Boolean).every(word => haystack.includes(word));
};

/**
 * Searchable picker of legal guardian candidates. Each candidate shows its registration number
 * (members) or e-mail (non-members) so namesakes can be told apart.
 */
export const LegalGuardianCandidatePicker = ({prop, errorText, renderMode = 'field'}: HalFormsInputProps): ReactElement => {
    const [search, setSearch] = useState('');
    const {options: candidates, isLoading} = useHalFormOptions(
        prop.options,
        undefined,
        item => describe(item as unknown as CandidateOption),
    );

    return (
        <Field name={prop.name} validate={() => undefined}>
            {({field, form}: FieldProps<unknown>) => {
                const selected = field.value === undefined || field.value === null ? '' : String(field.value);
                const options = candidates.filter(option =>
                    option.value === selected || matches(option.label, search));
                return (
                    <div className="space-y-2">
                        <TextField
                            type="text"
                            aria-label={labels.ui.legalGuardianSearch}
                            placeholder={labels.ui.legalGuardianSearch}
                            value={search}
                            onChange={event => setSearch(event.target.value)}
                        />
                        <div className="relative">
                            <SelectField
                                {...field}
                                id={`field-${prop.name}`}
                                value={selected}
                                label={renderMode === 'field' ? (prop.prompt || getFieldLabel(prop.name)) : undefined}
                                placeholder={isLoading ? labels.ui.loading : labels.ui.selectOption}
                                disabled={isLoading}
                                required={prop.required}
                                error={errorText}
                                options={options}
                                className="w-full"
                            />
                            {selected !== '' && (
                                <ClearSelectionButton
                                    className="right-10"
                                    onClick={() => form.setFieldValue(prop.name, '')}
                                />
                            )}
                        </div>
                    </div>
                );
            }}
        </Field>
    );
};
