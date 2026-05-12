import {type ReactElement} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {Button, DetailRow} from '../../components/UI';
import {Section} from './MemberSection';
import {BirthNumberConditionalField} from './BirthNumberConditionalField';
import {labels} from '../../localization';
import {HalFormPanel} from '../../components/HalNavigator2/HalFormPanel';
import type {HalFormPanelRenderHelpers} from '../../components/HalNavigator2/HalFormPanel';

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

    return (
        <HalFormPanel
            collectionUrl="/api/members"
            templateName="registerMember"
            successMessage="Člen úspěšně zaregistrován"
            onSuccess={() => navigate('/members')}
            templateMissingMessage={labels.errors.memberRegistrationUnavailable}
        >
            {({renderInput, renderField, hasField, hasType}: HalFormPanelRenderHelpers) => {
                const hasFields = (fieldNames: string[]) => fieldNames.some(f => hasField(f));
                const hasDocumentFields = hasFields(DOCUMENT_FIELDS) || DOCUMENT_TYPES.some(t => hasType(t));
                return (
                    <div className="flex flex-col gap-8">
                        <div>
                            <Link to="/members" className="text-sm text-primary hover:text-primary-light">
                                {labels.ui.backToList}
                            </Link>
                        </div>

                        <div>
                            <h1 className="text-3xl font-bold text-text-primary">
                                {labels.sections.newMemberRegistration}
                            </h1>
                            <hr className="border-border mt-4"/>
                        </div>

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
                                    <Section title={labels.sections.contact}>
                                        {hasField('email') && <DetailRow label={labels.fields.email}>{renderInput('email')}</DetailRow>}
                                        {hasField('phone') && <DetailRow label={labels.fields.phone}>{renderInput('phone')}</DetailRow>}
                                    </Section>
                                )}

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

                                {hasType(GUARDIAN_TYPE) && (
                                    <Section title={labels.sections.guardian}>
                                        {renderInput('guardian')}
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
                            <Link to="/members">
                                <Button variant="secondary">
                                    {labels.buttons.cancel}
                                </Button>
                            </Link>
                            {renderField('submit')}
                        </div>
                    </div>
                );
            }}
        </HalFormPanel>
    );
};
