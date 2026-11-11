import {DetailRow} from '../../components/UI';
import type {HalFormPanelRenderHelpers} from '../../components/HalNavigator2/HalFormPanel.tsx';
import {labels} from '../../localization';
import {Section} from './MemberSection.tsx';
import {RegistrationContactSection, RegistrationGuardianSections} from './RegistrationGuardianSections.tsx';
import {BirthNumberConditionalField} from './BirthNumberConditionalField.tsx';

const PERSONAL_FIELDS = ['firstName', 'lastName', 'dateOfBirth', 'gender', 'nationality', 'birthNumber'];
const CONTACT_FIELDS = ['email', 'phone'];
const ADDRESS_TYPE = 'AddressRequest';
const SUPPLEMENTARY_FIELDS = ['chipNumber', 'bankAccountNumber', 'dietaryRestrictions'];
const IDENTITY_CARD_TYPE = 'IdentityCardDto';
const MEDICAL_COURSE_TYPE = 'MedicalCourseDto';
const TRAINER_LICENSE_TYPE = 'TrainerLicenseDto';
const DOCUMENT_FIELDS = ['drivingLicenseGroup'];
const DOCUMENT_TYPES = [IDENTITY_CARD_TYPE, MEDICAL_COURSE_TYPE, TRAINER_LICENSE_TYPE];

export const renderRegistrationForm = (takeOverId?: string) => ({renderInput, renderField, hasField, hasType}: HalFormPanelRenderHelpers) => {
    const hasFields = (fieldNames: string[]) => fieldNames.some(f => hasField(f));
    const hasDocumentFields = hasFields(DOCUMENT_FIELDS) || DOCUMENT_TYPES.some(t => hasType(t));
    return (
        <div className="flex flex-col gap-8">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                <div className="flex flex-col gap-6">
                    {hasFields(PERSONAL_FIELDS) && (
                        <Section title={labels.sections.personalInfo}>
                            {hasField('firstName') && <DetailRow label={labels.fields.firstName}>{renderInput('firstName')}</DetailRow>}
                            {hasField('lastName') && <DetailRow label={labels.fields.lastName}>{renderInput('lastName')}</DetailRow>}
                            {hasField('dateOfBirth') && <DetailRow label={labels.fields.dateOfBirth}>{renderInput('dateOfBirth')}</DetailRow>}
                            {hasField('gender') && <DetailRow label={labels.fields.gender}>{renderInput('gender')}</DetailRow>}
                            {hasField('nationality') && <DetailRow label={labels.fields.nationality}>{renderInput('nationality')}</DetailRow>}
                            {hasField('birthNumber') && <BirthNumberConditionalField renderInput={renderInput}/>}
                        </Section>
                    )}
                    {hasFields(CONTACT_FIELDS) && (
                        <RegistrationContactSection renderInput={renderInput} hasField={hasField}/>
                    )}
                    <RegistrationGuardianSections renderInput={renderInput} hasField={hasField} initialTakeOverId={takeOverId}/>
                    {hasType(ADDRESS_TYPE) && (
                        <Section title={labels.sections.address}>
                            {renderInput('address')}
                        </Section>
                    )}
                </div>
                <div className="flex flex-col gap-6">
                    {hasFields(SUPPLEMENTARY_FIELDS) && (
                        <Section title={labels.sections.supplementary}>
                            {hasField('chipNumber') && <DetailRow label={labels.fields.chipNumber}>{renderInput('chipNumber')}</DetailRow>}
                            {hasField('bankAccountNumber') && (
                                <DetailRow label={labels.fields.bankAccountNumber}>
                                    {renderInput('bankAccountNumber')}
                                </DetailRow>
                            )}
                            {hasField('dietaryRestrictions') && <DetailRow label={labels.fields.dietaryRestrictions}>{renderInput('dietaryRestrictions')}</DetailRow>}
                        </Section>
                    )}
                </div>
            </div>
            {hasDocumentFields && (
                <Section title={labels.sections.documentsAndLicenses}>
                    {hasType(IDENTITY_CARD_TYPE) && renderInput('identityCard')}
                    {hasField('drivingLicenseGroup') && <DetailRow label={labels.fields.drivingLicenseGroup}>{renderInput('drivingLicenseGroup')}</DetailRow>}
                    {hasType(MEDICAL_COURSE_TYPE) && renderInput('medicalCourse')}
                    {hasType(TRAINER_LICENSE_TYPE) && renderInput('trainerLicense')}
                </Section>
            )}
            <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-border">
                {renderField('cancel')}
                {renderField('submit')}
            </div>
        </div>
    );
};
