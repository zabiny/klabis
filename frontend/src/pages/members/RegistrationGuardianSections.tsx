import {type ReactElement, type ReactNode, useEffect} from 'react';
import {useFormikContext} from 'formik';
import {DetailRow} from '../../components/UI';
import {authorizedFetch} from '../../api/authorizedFetch';
import type {components} from '../../api/klabisApi';
import {labels} from '../../localization';
import {Section} from './MemberSection.tsx';
import {isMinorOnDate} from './registrationAge.ts';

type LegalGuardianProfile = components['schemas']['LegalGuardianResponse'];

interface RegistrationSectionProps {
    renderInput: (name: string) => ReactNode;
    hasField: (name: string) => boolean;
}

const useIsMinor = (): boolean | undefined => {
    const {values} = useFormikContext<Record<string, unknown>>();
    return isMinorOnDate(values.dateOfBirth);
};

/** Backend requires own email and phone only for adults, so the required mark follows the age. */
export const RegistrationContactSection = ({renderInput, hasField}: RegistrationSectionProps): ReactElement => {
    const isMinor = useIsMinor();
    const mark = (label: string) => isMinor === false ? `${label} *` : label;
    return (
        <Section title={labels.sections.contact}>
            {hasField('email') && <DetailRow label={mark(labels.fields.email)}>{renderInput('email')}</DetailRow>}
            {hasField('phone') && <DetailRow label={mark(labels.fields.phone)}>{renderInput('phone')}</DetailRow>}
            {isMinor === true && (
                <p className="text-xs text-text-secondary pt-2">{labels.ui.registrationMinorContactHint}</p>
            )}
        </Section>
    );
};

export const RegistrationGuardianSections = ({renderInput, hasField}: RegistrationSectionProps): ReactElement | null => {
    const {values, setFieldValue} = useFormikContext<Record<string, unknown>>();
    const isMinor = useIsMinor();
    const takeoverId = typeof values.legalGuardianUserId === 'string' ? values.legalGuardianUserId : '';
    const hasGuardianValue = Array.isArray(values.legalGuardians) && values.legalGuardians.length > 0;

    useEffect(() => {
        if (isMinor === true && takeoverId) void setFieldValue('legalGuardianUserId', '');
        if (isMinor === false && hasGuardianValue) void setFieldValue('legalGuardians', []);
    }, [isMinor, takeoverId, hasGuardianValue, setFieldValue]);

    useEffect(() => {
        if (isMinor !== false || !takeoverId) return;
        let cancelled = false;
        authorizedFetch(`/api/legal-guardians/${takeoverId}`)
            .then(response => response.json() as Promise<LegalGuardianProfile>)
            .then(profile => {
                if (cancelled) return;
                void setFieldValue('firstName', profile.firstName);
                void setFieldValue('lastName', profile.lastName);
                void setFieldValue('email', profile.email);
                void setFieldValue('phone', profile.phone);
            })
            .catch(() => undefined);
        return () => {
            cancelled = true;
        };
    }, [isMinor, takeoverId, setFieldValue]);

    if (isMinor === true && hasField('legalGuardians')) {
        return (
            <Section title={labels.sections.legalGuardians}>
                <p className="text-xs text-text-secondary pb-2">{labels.ui.registrationGuardiansRequiredHint}</p>
                {renderInput('legalGuardians')}
            </Section>
        );
    }
    if (isMinor === false && hasField('legalGuardianUserId')) {
        return (
            <Section title={labels.sections.legalGuardianTakeover}>
                <p className="text-xs text-text-secondary pb-2">{labels.ui.registrationTakeoverHint}</p>
                <DetailRow label={labels.fields.legalGuardianUserId}>{renderInput('legalGuardianUserId')}</DetailRow>
            </Section>
        );
    }
    return null;
};
