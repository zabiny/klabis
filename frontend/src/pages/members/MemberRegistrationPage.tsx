import {type ReactElement, useMemo} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {Formik, Form} from 'formik';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {toHref} from '../../api/hateoas';
import {useAuthorizedQuery, useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {useToast} from '../../contexts/ToastContext';
import {EditableDetailRow} from './EditableDetailRow';
import {buildEmptyInitialValues, buildValidationSchema} from './formUtils';
import {COMPOSITE_SUBFIELDS} from './compositeTypes';

interface SectionProps {
    title: string;
    children: React.ReactNode;
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
const IDENTITY_CARD_TYPE = 'IdentityCardRequest';
const MEDICAL_COURSE_TYPE = 'MedicalCourseRequest';
const TRAINER_LICENSE_TYPE = 'TrainerLicenseRequest';
const DOCUMENT_FIELDS = ['drivingLicenseGroup'];
const DOCUMENT_TYPES = [IDENTITY_CARD_TYPE, MEDICAL_COURSE_TYPE, TRAINER_LICENSE_TYPE];
const GUARDIAN_TYPE = 'GuardianRequest';

const FIELD_LABELS: Record<string, string> = {
    firstName: 'Jméno',
    lastName: 'Příjmení',
    dateOfBirth: 'Datum narození',
    gender: 'Pohlaví',
    nationality: 'Státní příslušnost',
    birthNumber: 'Rodné číslo',
    email: 'E-mail',
    phone: 'Telefon',
    'address.street': 'Ulice',
    'address.city': 'Město',
    'address.postalCode': 'PSČ',
    'address.country': 'Stát',
    chipNumber: 'Číslo čipu',
    bankAccountNumber: 'Číslo bankovního účtu',
    dietaryRestrictions: 'Stravovací omezení',
    'identityCard.cardNumber': 'Číslo OP',
    'identityCard.validityDate': 'Platnost OP',
    drivingLicenseGroup: 'Řidičský průkaz',
    'medicalCourse.completionDate': 'Zdravotní kurz',
    'medicalCourse.validityDate': 'Platnost ZK',
    'trainerLicense.licenseNumber': 'Trenérská licence',
    'trainerLicense.validityDate': 'Platnost TL',
    'guardian.firstName': 'Jméno',
    'guardian.lastName': 'Příjmení',
    'guardian.relationship': 'Vztah',
    'guardian.email': 'E-mail',
    'guardian.phone': 'Telefon',
};

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

    const initialValues = useMemo(() => buildEmptyInitialValues(template), [template]);
    const validationSchema = useMemo(() => buildValidationSchema(template), [template]);

    const handleSubmit = (values: Record<string, unknown>) => {
        const url = template.target || '/api/members';
        mutate({url, data: values});
    };

    const personalProps = template.properties.filter(p => PERSONAL_FIELDS.includes(p.name));
    const contactProps = template.properties.filter(p => CONTACT_FIELDS.includes(p.name));
    const addressProp = template.properties.find(p => p.type === ADDRESS_TYPE);
    const supplementaryProps = template.properties.filter(p => SUPPLEMENTARY_FIELDS.includes(p.name));
    const documentProps = template.properties.filter(p => DOCUMENT_FIELDS.includes(p.name) || DOCUMENT_TYPES.includes(p.type));
    const guardianProp = template.properties.find(p => p.type === GUARDIAN_TYPE);

    const renderField = (fieldName: string, label?: string) => (
        <EditableDetailRow
            key={fieldName}
            label={label || FIELD_LABELS[fieldName] || fieldName}
            fieldName={fieldName}
            template={template}
            isEditing={true}
        >
            {''}
        </EditableDetailRow>
    );

    const renderCompositeFields = (propName: string, propType: string) => {
        const subfields = COMPOSITE_SUBFIELDS[propType] || [];
        return subfields.map(sf => renderField(`${propName}.${sf}`, FIELD_LABELS[`${propName}.${sf}`]));
    };

    return (
        <Formik
            initialValues={initialValues}
            validationSchema={validationSchema}
            onSubmit={handleSubmit}
        >
            <Form>
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
                        <button
                            type="submit"
                            disabled={isPending}
                            className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light"
                        >
                            Registrovat
                        </button>
                        <Link
                            to="/members"
                            className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                        >
                            Zrušit
                        </Link>
                    </div>

                    {personalProps.length > 0 && (
                        <Section title="OSOBNÍ ÚDAJE">
                            {personalProps.map(p => renderField(p.name))}
                        </Section>
                    )}

                    {contactProps.length > 0 && (
                        <Section title="KONTAKT">
                            {contactProps.map(p => renderField(p.name))}
                        </Section>
                    )}

                    {addressProp && (
                        <Section title="ADRESA">
                            {renderCompositeFields(addressProp.name, addressProp.type)}
                        </Section>
                    )}

                    {supplementaryProps.length > 0 && (
                        <Section title="DOPLŇKOVÉ INFORMACE">
                            {supplementaryProps.map(p => renderField(p.name))}
                        </Section>
                    )}

                    {documentProps.length > 0 && (
                        <Section title="DOKLADY A LICENCE">
                            {documentProps.map(p => {
                                if (DOCUMENT_TYPES.includes(p.type)) {
                                    return renderCompositeFields(p.name, p.type);
                                }
                                return renderField(p.name);
                            })}
                        </Section>
                    )}

                    {guardianProp && (
                        <Section title="ZÁKONNÝ ZÁSTUPCE">
                            {renderCompositeFields(guardianProp.name, guardianProp.type)}
                        </Section>
                    )}
                </div>
            </Form>
        </Formik>
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
