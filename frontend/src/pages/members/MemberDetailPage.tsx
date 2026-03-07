import {type ReactElement, type ReactNode, useState} from "react";
import {Link} from "react-router-dom";
import {Formik, Form} from "formik";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {Skeleton} from "../../components/UI";
import {Badge} from "../../components/UI/Badge";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {formatDate} from "../../utils/dateUtils.ts";
import type {components} from "../../api/klabisApi";
import type {HalResponse} from "../../api";
import {EditableDetailRow} from "./EditableDetailRow";
import {useMemberEditForm} from "./useMemberEditForm";

type MemberDetail = components['schemas']['EntityModelMemberDetailsResponse'] & HalResponse;

const DEACTIVATION_REASON_LABELS: Record<string, string> = {
    ODHLASKA: 'Odhlášení',
    PRESTUP: 'Přestup',
    OTHER: 'Jiný důvod',
};

const GENDER_LABELS: Record<string, string> = {
    MALE: 'Muž',
    FEMALE: 'Žena',
};

interface DetailRowProps {
    label: string;
    children: React.ReactNode;
}

const DetailRow = ({label, children}: DetailRowProps) => (
    <div className="flex flex-col sm:flex-row sm:gap-4 py-2 border-b border-border last:border-b-0">
        <dt className="text-sm text-text-secondary sm:w-48 shrink-0">{label}</dt>
        <dd className="text-sm text-text-primary">{children}</dd>
    </div>
);

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

const MaskedBirthNumber = ({value}: { value: string }) => {
    const [revealed, setRevealed] = useState(false);
    const formatted = value.length >= 7
        ? `${value.substring(0, 6)}/${value.substring(6)}`
        : value;
    return (
        <span className="inline-flex items-center gap-2">
            {revealed ? formatted : '••••••/••••'}
            <button
                type="button"
                className="text-xs text-primary hover:text-primary-light underline"
                onClick={() => setRevealed(!revealed)}
            >
                {revealed ? 'Skrýt' : 'Zobrazit'}
            </button>
        </span>
    );
};

interface ERowProps {
    label: string;
    fieldName: string;
    isEditing: boolean;
    template: ReturnType<typeof useMemberEditForm>['template'];
    children: ReactNode;
}

const ERow = ({label, fieldName, isEditing, template, children}: ERowProps) => {
    if (!isEditing || !template) {
        return <DetailRow label={label}>{children}</DetailRow>;
    }
    return (
        <EditableDetailRow
            label={label}
            fieldName={fieldName}
            template={template}
            isEditing={isEditing}
        >
            {children}
        </EditableDetailRow>
    );
};

export const MemberDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error, hasLink, route} = useHalPageData<MemberDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <div className="text-feedback-error">{error.message}</div>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <MemberDetailContent resourceData={resourceData} hasLink={hasLink} route={route}/>;
};

interface MemberDetailContentProps {
    resourceData: MemberDetail;
    hasLink: (name: string) => boolean;
    route: ReturnType<typeof useHalPageData>['route'];
}

const MemberDetailContent = ({resourceData, hasLink, route}: MemberDetailContentProps) => {
    const editForm = useMemberEditForm(resourceData);
    const {isEditing, startEditing, cancelEditing, handleSubmit, template, initialValues, validationSchema} = editForm;

    const member = resourceData;
    const address = member.address;
    const guardian = member.guardian;
    const identityCard = member.identityCard;
    const medicalCourse = member.medicalCourse;
    const trainerLicense = member.trainerLicense;

    const hasSupplementaryInfo = member.chipNumber || member.bankAccountNumber || member.dietaryRestrictions;
    const hasDocuments = identityCard?.cardNumber || member.drivingLicenseGroup || medicalCourse?.completionDate || trainerLicense?.licenseNumber;
    const showDeactivation = member.active === false && member.deactivationReason;

    const pageContent = (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/members" className="text-sm text-primary hover:text-primary-light">
                    &larr; Zpět na seznam
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-4">
                    <h1 className="text-3xl font-bold text-text-primary">
                        {member.firstName} {member.lastName}
                    </h1>
                    <Badge variant={member.active ? 'success' : 'default'} size="sm">
                        {member.active ? 'Aktivní' : 'Neaktivní'}
                    </Badge>
                </div>
                <span className="text-sm text-text-secondary">{member.registrationNumber}</span>
            </div>

            <div className="flex flex-wrap gap-3">
                {isEditing ? (
                    <>
                        <button
                            type="submit"
                            className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light"
                        >
                            Uložit
                        </button>
                        <button
                            type="button"
                            onClick={cancelEditing}
                            className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                        >
                            Zrušit
                        </button>
                    </>
                ) : (
                    <>
                        {editForm.hasTemplate && (
                            <button
                                type="button"
                                onClick={startEditing}
                                className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light"
                            >
                                Upravit
                            </button>
                        )}
                        <HalFormButton name="terminate" modal={true} label="Ukončit členství"/>
                        <HalFormButton name="reactivate" modal={true} label="Reaktivovat"/>
                        {hasLink('permissions') && (
                            <Link
                                to={route.getResourceLink('permissions')?.href ?? '#'}
                                className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                            >
                                Správa oprávnění
                            </Link>
                        )}
                    </>
                )}
            </div>

            <Section title="OSOBNÍ ÚDAJE">
                <ERow label="Jméno" fieldName="firstName" isEditing={isEditing} template={template}>
                    {member.firstName}
                </ERow>
                <ERow label="Příjmení" fieldName="lastName" isEditing={isEditing} template={template}>
                    {member.lastName}
                </ERow>
                {member.dateOfBirth && (
                    <ERow label="Datum narození" fieldName="dateOfBirth" isEditing={isEditing} template={template}>
                        {formatDate(member.dateOfBirth)}
                    </ERow>
                )}
                {member.gender && (
                    <ERow label="Pohlaví" fieldName="gender" isEditing={isEditing} template={template}>
                        {GENDER_LABELS[member.gender] ?? member.gender}
                    </ERow>
                )}
                {member.nationality && (
                    <ERow label="Státní příslušnost" fieldName="nationality" isEditing={isEditing} template={template}>
                        {member.nationality}
                    </ERow>
                )}
                {member.nationality === 'CZ' && member.birthNumber && (
                    <ERow label="Rodné číslo" fieldName="birthNumber" isEditing={isEditing} template={template}>
                        {isEditing ? member.birthNumber : <MaskedBirthNumber value={member.birthNumber}/>}
                    </ERow>
                )}
                {isEditing && (
                    <ERow label="Registrační číslo" fieldName="registrationNumber" isEditing={isEditing} template={template}>
                        {member.registrationNumber}
                    </ERow>
                )}
            </Section>

            <Section title="KONTAKT">
                {member.email && (
                    <ERow label="E-mail" fieldName="email" isEditing={isEditing} template={template}>
                        {member.email}
                    </ERow>
                )}
                {member.phone && (
                    <ERow label="Telefon" fieldName="phone" isEditing={isEditing} template={template}>
                        {member.phone}
                    </ERow>
                )}
            </Section>

            {(address || isEditing) && (
                <Section title="ADRESA">
                    {(address?.street || isEditing) && (
                        <ERow label="Ulice" fieldName="address.street" isEditing={isEditing} template={template}>
                            {address?.street}
                        </ERow>
                    )}
                    {(address?.city || isEditing) && (
                        <ERow label="Město" fieldName="address.city" isEditing={isEditing} template={template}>
                            {address?.city}
                        </ERow>
                    )}
                    {(address?.postalCode || isEditing) && (
                        <ERow label="PSČ" fieldName="address.postalCode" isEditing={isEditing} template={template}>
                            {address?.postalCode}
                        </ERow>
                    )}
                    {(address?.country || isEditing) && (
                        <ERow label="Stát" fieldName="address.country" isEditing={isEditing} template={template}>
                            {address?.country}
                        </ERow>
                    )}
                </Section>
            )}

            {hasSupplementaryInfo && (
                <Section title="DOPLŇKOVÉ INFORMACE">
                    {member.chipNumber && (
                        <ERow label="Číslo čipu" fieldName="chipNumber" isEditing={isEditing} template={template}>
                            {member.chipNumber}
                        </ERow>
                    )}
                    {member.bankAccountNumber && (
                        <ERow label="Číslo bankovního účtu" fieldName="bankAccountNumber" isEditing={isEditing} template={template}>
                            {member.bankAccountNumber}
                        </ERow>
                    )}
                    {member.dietaryRestrictions && (
                        <ERow label="Stravovací omezení" fieldName="dietaryRestrictions" isEditing={isEditing} template={template}>
                            {member.dietaryRestrictions}
                        </ERow>
                    )}
                </Section>
            )}

            {hasDocuments && (
                <Section title="DOKLADY A LICENCE">
                    {identityCard?.cardNumber && (
                        <>
                            <ERow label="Číslo OP" fieldName="identityCard.cardNumber" isEditing={isEditing} template={template}>
                                {identityCard.cardNumber}
                            </ERow>
                            {identityCard.validityDate && (
                                <ERow label="Platnost OP" fieldName="identityCard.validityDate" isEditing={isEditing} template={template}>
                                    {formatDate(identityCard.validityDate)}
                                </ERow>
                            )}
                        </>
                    )}
                    {member.drivingLicenseGroup && (
                        <ERow label="Řidičský průkaz" fieldName="drivingLicenseGroup" isEditing={isEditing} template={template}>
                            {member.drivingLicenseGroup}
                        </ERow>
                    )}
                    {medicalCourse?.completionDate && (
                        <>
                            <ERow label="Zdravotní kurz" fieldName="medicalCourse.completionDate" isEditing={isEditing} template={template}>
                                {formatDate(medicalCourse.completionDate)}
                            </ERow>
                            {medicalCourse.validityDate && (
                                <ERow label="Platnost ZK" fieldName="medicalCourse.validityDate" isEditing={isEditing} template={template}>
                                    {formatDate(medicalCourse.validityDate)}
                                </ERow>
                            )}
                        </>
                    )}
                    {trainerLicense?.licenseNumber && (
                        <>
                            <ERow label="Trenérská licence" fieldName="trainerLicense.licenseNumber" isEditing={isEditing} template={template}>
                                {trainerLicense.licenseNumber}
                            </ERow>
                            {trainerLicense.validityDate && (
                                <ERow label="Platnost TL" fieldName="trainerLicense.validityDate" isEditing={isEditing} template={template}>
                                    {formatDate(trainerLicense.validityDate)}
                                </ERow>
                            )}
                        </>
                    )}
                </Section>
            )}

            {guardian && (
                <Section title="ZÁKONNÝ ZÁSTUPCE">
                    <ERow label="Jméno" fieldName="guardian.firstName" isEditing={isEditing} template={template}>
                        {guardian.firstName}
                    </ERow>
                    <ERow label="Příjmení" fieldName="guardian.lastName" isEditing={isEditing} template={template}>
                        {guardian.lastName}
                    </ERow>
                    {guardian.relationship && (
                        <ERow label="Vztah" fieldName="guardian.relationship" isEditing={isEditing} template={template}>
                            {guardian.relationship}
                        </ERow>
                    )}
                    {guardian.email && (
                        <ERow label="E-mail" fieldName="guardian.email" isEditing={isEditing} template={template}>
                            {guardian.email}
                        </ERow>
                    )}
                    {guardian.phone && (
                        <ERow label="Telefon" fieldName="guardian.phone" isEditing={isEditing} template={template}>
                            {guardian.phone}
                        </ERow>
                    )}
                </Section>
            )}

            {showDeactivation && (
                <Section title="DEAKTIVACE">
                    <DetailRow label="Důvod">
                        {member.deactivationReason && (DEACTIVATION_REASON_LABELS[member.deactivationReason] ?? member.deactivationReason)}
                    </DetailRow>
                    {member.deactivatedAt && (
                        <DetailRow label="Datum">{formatDate(member.deactivatedAt)}</DetailRow>
                    )}
                    {member.deactivationNote && (
                        <DetailRow label="Poznámka">{member.deactivationNote}</DetailRow>
                    )}
                </Section>
            )}
        </div>
    );

    if (isEditing) {
        return (
            <Formik
                initialValues={initialValues}
                validationSchema={validationSchema}
                enableReinitialize
                onSubmit={(values) => handleSubmit(values)}
            >
                <Form>
                    {pageContent}
                </Form>
            </Formik>
        );
    }

    return pageContent;
};
