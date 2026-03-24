import {type ReactElement} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import type {HalResponse} from '../../api';
import {toHref} from '../../api/hateoas';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay';
import type {RenderFormCallback} from '../../components/HalNavigator2/halforms';
import {DetailRow} from '../../components/UI';
import {Section} from './MemberSection';
import {BirthNumberConditionalField} from './BirthNumberConditionalField';

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

export const MemberRegistrationPage = (): ReactElement => {
    const navigate = useNavigate();
    const {data: collectionData, isLoading, error} = useAuthorizedQuery<HalResponse>('/api/members');

    if (isLoading) {
        return <div>Načítání...</div>;
    }

    if (error) {
        return <div className="text-error">{(error as Error).message}</div>;
    }

    const template = collectionData?._templates?.registerMember ?? null;

    if (!template) {
        return (
            <div className="flex flex-col gap-4">
                <Link to="/members" className="text-sm text-primary hover:text-primary-light">
                    &larr; Zpět na seznam
                </Link>
                <div className="text-error">Registrace nového člena není k dispozici.</div>
            </div>
        );
    }

    const fieldNameSet = new Set(template.properties.map(p => p.name));
    const fieldTypeSet = new Set(template.properties.map(p => p.type));

    const hasField = (fieldName: string) => fieldNameSet.has(fieldName);
    const hasFields = (fieldNames: string[]) => fieldNames.some(f => fieldNameSet.has(f));
    const hasType = (type: string) => fieldTypeSet.has(type);

    const hasDocumentFields = hasFields(DOCUMENT_FIELDS) || DOCUMENT_TYPES.some(t => fieldTypeSet.has(t));

    const handleSubmitSuccess = (responseData?: unknown) => {
        const halData = responseData as HalResponse | undefined;
        const selfLink = halData?._links?.self;
        if (selfLink) {
            navigate(toHref(selfLink).replace(/^\/api/, ''));
        } else {
            navigate('/members');
        }
    };

    const renderForm: RenderFormCallback = ({renderInput, renderField}) => (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/members" className="text-sm text-primary hover:text-primary-light">
                    &larr; Zpět na seznam
                </Link>
            </div>

            <div>
                <h1 className="text-3xl font-bold text-text-primary">
                    Registrace nového člena
                </h1>
                <hr className="border-border mt-4"/>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                <div className="flex flex-col gap-6">
                    {hasFields(PERSONAL_FIELDS) && (
                        <Section title="OSOBNÍ ÚDAJE">
                            {hasField('firstName') && <DetailRow label="Jméno">{renderInput('firstName')}</DetailRow>}
                            {hasField('lastName') && <DetailRow label="Příjmení">{renderInput('lastName')}</DetailRow>}
                            {hasField('dateOfBirth') && <DetailRow label="Datum narození">{renderInput('dateOfBirth')}</DetailRow>}
                            {hasField('gender') && <DetailRow label="Pohlaví">{renderInput('gender')}</DetailRow>}
                            {hasField('nationality') && <DetailRow label="Státní příslušnost">{renderInput('nationality')}</DetailRow>}
                            {hasField('birthNumber') && <BirthNumberConditionalField renderInput={renderInput} hasBirthNumberField={true}/>}
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
                </div>

                <div className="flex flex-col gap-6">
                    {hasFields(SUPPLEMENTARY_FIELDS) && (
                        <Section title="DOPLŇKOVÉ INFORMACE">
                            {hasField('chipNumber') && <DetailRow label="Číslo člena">{renderInput('chipNumber')}</DetailRow>}
                            {hasField('bankAccountNumber') && (
                                <DetailRow label="Bankovní účet">
                                    {renderInput('bankAccountNumber')}
                                </DetailRow>
                            )}
                            {hasField('dietaryRestrictions') && <DetailRow label="Stravovací omezení">{renderInput('dietaryRestrictions')}</DetailRow>}
                        </Section>
                    )}

                    {hasType(GUARDIAN_TYPE) && (
                        <Section title="ZÁKONNÝ ZÁSTUPCE">
                            {renderInput('guardian')}
                        </Section>
                    )}
                </div>
            </div>

            {hasDocumentFields && (
                <Section title="DOKLADY A LICENCE">
                    {hasType(IDENTITY_CARD_TYPE) && renderInput('identityCard')}
                    {hasField('drivingLicenseGroup') && <DetailRow label="Řidičský průkaz">{renderInput('drivingLicenseGroup')}</DetailRow>}
                    {hasType(MEDICAL_COURSE_TYPE) && renderInput('medicalCourse')}
                    {hasType(TRAINER_LICENSE_TYPE) && renderInput('trainerLicense')}
                </Section>
            )}

            <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-border">
                <Link
                    to="/members"
                    className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                >
                    Zrušit
                </Link>
                {renderField('submit')}
            </div>
        </div>
    );

    return (
        <HalFormDisplay
            template={template}
            templateName="registerMember"
            resourceData={{}}
            pathname="/members"
            onClose={() => navigate('/members')}
            onSubmitSuccess={handleSubmitSuccess}
            successMessage="Člen úspěšně zaregistrován"
            customLayout={renderForm}
        />
    );
};
