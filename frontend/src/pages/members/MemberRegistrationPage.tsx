import {type ReactElement, type ReactNode} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {toHref} from '../../api/hateoas';
import {useAuthorizedQuery, useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {useToast} from '../../contexts/ToastContext';
import {HalFormsForm} from '../../components/HalNavigator2/halforms';
import {klabisFieldsFactory} from '../../components/KlabisFieldsFactory';
import {DetailRow} from '../../components/UI';

interface SectionProps {
    title: string;
    children: ReactNode;
}

const Section = ({title, children}: SectionProps) => (
    <div className="bg-surface-raised rounded-md border border-border p-6">
        <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">{title}</h3>
        <dl>{children}</dl>
    </div>
);

const PERSONAL_FIELDS = ['firstName', 'lastName', 'dateOfBirth', 'gender', 'nationality', 'birthNumber'];
const CONTACT_FIELDS = ['email', 'phone'];
const ADDRESS_TYPE = 'AddressRequest';
const SUPPLEMENTARY_FIELDS = ['chipNumber', 'bankAccountNumber', 'dietaryRestrictions'];
const IDENTITY_CARD_TYPE = 'IdentityCardDto';
const MEDICAL_COURSE_TYPE = 'MedicalCourseDto';
const TRAINER_LICENSE_TYPE = 'TrainerLicenseDto';
const DOCUMENT_FIELDS = ['drivingLicenseGroup'];
const DOCUMENT_TYPES = [IDENTITY_CARD_TYPE, MEDICAL_COURSE_TYPE, TRAINER_LICENSE_TYPE];
const GUARDIAN_TYPE = 'GuardianDTO';

interface RegistrationFormProps {
    template: HalFormsTemplate;
}

const RegistrationForm = ({template}: RegistrationFormProps) => {
    const navigate = useNavigate();
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const {addToast} = useToast();

    const {mutate, isPending} = useAuthorizedMutation({
        method: template.method || 'POST',
        onSuccess: (data: unknown) => {
            invalidateAllCaches();
            addToast('Člen úspěšně zaregistrován', 'success');
            const halData = data as HalResponse;
            const selfLink = halData?._links?.self;
            if (selfLink) {
                navigate(toHref(selfLink).replace(/^\/api/, ''));
            } else {
                navigate('/members');
            }
        },
    });

    const handleSubmit = async (values: Record<string, unknown>) => {
        const url = template.target || '/api/members';
        mutate({url, data: values});
    };

    const hasField = (fieldName: string) =>
        template.properties.some(p => p.name === fieldName);

    const hasFields = (fieldNames: string[]) =>
        template.properties.some(p => fieldNames.includes(p.name));

    const hasType = (type: string) =>
        template.properties.some(p => p.type === type);

    const hasDocumentFields = hasFields(DOCUMENT_FIELDS) ||
        template.properties.some(p => DOCUMENT_TYPES.includes(p.type));

    return (
        <HalFormsForm
            data={{}}
            template={template}
            onSubmit={handleSubmit}
            fieldsFactory={klabisFieldsFactory}
            submitButtonLabel="Registrovat"
            isSubmitting={isPending}
            renderForm={({renderInput, renderField}) => (
                <div className="flex flex-col gap-8">
                    <div>
                        <Link to="/members" className="text-sm text-primary hover:text-primary-light">
                            &larr; Zpět na seznam
                        </Link>
                    </div>

                    <h1 className="text-3xl font-bold text-text-primary">
                        Registrace nového člena
                    </h1>

                    <div className="flex flex-wrap gap-3">
                        {renderField('submit')}
                        <Link
                            to="/members"
                            className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                        >
                            Zrušit
                        </Link>
                    </div>

                    {hasFields(PERSONAL_FIELDS) && (
                        <Section title="OSOBNÍ ÚDAJE">
                            {hasField('firstName') && <DetailRow label="Jméno">{renderInput('firstName')}</DetailRow>}
                            {hasField('lastName') && <DetailRow label="Příjmení">{renderInput('lastName')}</DetailRow>}
                            {hasField('dateOfBirth') && <DetailRow label="Datum narození">{renderInput('dateOfBirth')}</DetailRow>}
                            {hasField('gender') && <DetailRow label="Pohlaví">{renderInput('gender')}</DetailRow>}
                            {hasField('nationality') && <DetailRow label="Státní příslušnost">{renderInput('nationality')}</DetailRow>}
                            {hasField('birthNumber') && <DetailRow label="Rodné číslo">{renderInput('birthNumber')}</DetailRow>}
                        </Section>
                    )}

                    {hasFields(CONTACT_FIELDS) && (
                        <Section title="KONTAKT">
                            {hasField('email') && <DetailRow label="E-mail">{renderInput('email')}</DetailRow>}
                            {hasField('phone') && <DetailRow label="Telefon">{renderInput('phone')}</DetailRow>}
                        </Section>
                    )}

                    {hasType(ADDRESS_TYPE) && (
                        <Section title="ADRESA">
                            {renderInput('address')}
                        </Section>
                    )}

                    {hasFields(SUPPLEMENTARY_FIELDS) && (
                        <Section title="DOPLŇKOVÉ INFORMACE">
                            {hasField('chipNumber') && <DetailRow label="Číslo čipu">{renderInput('chipNumber')}</DetailRow>}
                            {hasField('bankAccountNumber') && (
                                <DetailRow label="Číslo bankovního účtu (nepovinné)">
                                    {renderInput('bankAccountNumber')}
                                    <p className="mt-1 text-sm text-text-tertiary">Pro proplácení cestovních nákladů a dalších výdajů spojených s klubem</p>
                                </DetailRow>
                            )}
                            {hasField('dietaryRestrictions') && <DetailRow label="Stravovací omezení">{renderInput('dietaryRestrictions')}</DetailRow>}
                        </Section>
                    )}

                    {hasDocumentFields && (
                        <Section title="DOKLADY A LICENCE">
                            {hasType(IDENTITY_CARD_TYPE) && renderInput('identityCard')}
                            {hasField('drivingLicenseGroup') && <DetailRow label="Řidičský průkaz">{renderInput('drivingLicenseGroup')}</DetailRow>}
                            {hasType(MEDICAL_COURSE_TYPE) && renderInput('medicalCourse')}
                            {hasType(TRAINER_LICENSE_TYPE) && renderInput('trainerLicense')}
                        </Section>
                    )}

                    {hasType(GUARDIAN_TYPE) && (
                        <Section title="ZÁKONNÝ ZÁSTUPCE">
                            {renderInput('guardian')}
                        </Section>
                    )}
                </div>
            )}
        />
    );
};

export const MemberRegistrationPage = (): ReactElement => {
    const {data: collectionData, isLoading, error} = useAuthorizedQuery<HalResponse>('/api/members');

    if (isLoading) {
        return <div>Načítání...</div>;
    }

    if (error) {
        return <div className="text-feedback-error">{(error as Error).message}</div>;
    }

    const template = collectionData?._templates?.default ?? null;

    if (!template) {
        return (
            <div className="flex flex-col gap-4">
                <Link to="/members" className="text-sm text-primary hover:text-primary-light">
                    &larr; Zpět na seznam
                </Link>
                <div className="text-feedback-error">Registrace nového člena není k dispozici.</div>
            </div>
        );
    }

    return <RegistrationForm template={template}/>;
};
