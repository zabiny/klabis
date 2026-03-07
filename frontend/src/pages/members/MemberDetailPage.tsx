import {type ReactElement, useState} from "react";
import {Link} from "react-router-dom";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {DetailRow, Skeleton} from "../../components/UI";
import {Badge} from "../../components/UI/Badge";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {HalFormsForm} from "../../components/HalNavigator2/halforms";
import {klabisFieldsFactory} from "../../components/KlabisFieldsFactory";
import {formatDate} from "../../utils/dateUtils.ts";
import type {components} from "../../api/klabisApi";
import type {HalFormsProperty, HalFormsTemplate, HalResponse} from "../../api";
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

function enrichTemplateWithReadOnlyFields(
    template: HalFormsTemplate,
    resourceData: Record<string, unknown>
): HalFormsTemplate {
    const templateFieldNames = new Set(template.properties.map(p => p.name));

    const readOnlyProps: HalFormsProperty[] = Object.keys(resourceData)
        .filter(key => !templateFieldNames.has(key) && !key.startsWith('_'))
        .filter(key => {
            const value = resourceData[key];
            return value === null || value === undefined || typeof value !== 'object';
        })
        .map(key => ({
            name: key,
            type: 'text',
            readOnly: true,
        }));

    return {
        ...template,
        properties: [...template.properties, ...readOnlyProps],
    };
}

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
    const {isEditing, startEditing, cancelEditing, handleSubmit, template} = editForm;

    const member = resourceData;
    const address = member.address;
    const guardian = member.guardian;
    const identityCard = member.identityCard;
    const medicalCourse = member.medicalCourse;
    const trainerLicense = member.trainerLicense;

    const hasSupplementaryInfo = member.chipNumber || member.bankAccountNumber || member.dietaryRestrictions;
    const hasDocuments = identityCard?.cardNumber || member.drivingLicenseGroup || medicalCourse?.completionDate || trainerLicense?.licenseNumber;
    const showDeactivation = member.active === false && member.deactivationReason;

    if (isEditing && template) {
        const enrichedTemplate = enrichTemplateWithReadOnlyFields(template, resourceData);
        const enrichedFieldNames = new Set(enrichedTemplate.properties.map(p => p.name));

        return (
            <HalFormsForm
                data={resourceData as Record<string, unknown>}
                template={enrichedTemplate}
                onSubmit={handleSubmit}
                fieldsFactory={klabisFieldsFactory}
                submitButtonLabel="Uložit"
                isSubmitting={editForm.isSubmitting}
                renderForm={({renderInput, renderField}) => {
                    const ri = (name: string) =>
                        enrichedFieldNames.has(name) ? renderInput(name) : null;
                    return (
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
                            {renderField('submit')}
                            <button
                                type="button"
                                onClick={cancelEditing}
                                className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                            >
                                Zrušit
                            </button>
                        </div>

                        <Section title="OSOBNÍ ÚDAJE">
                            <DetailRow label="Jméno">{ri('firstName')}</DetailRow>
                            <DetailRow label="Příjmení">{ri('lastName')}</DetailRow>
                            <DetailRow label="Datum narození">{ri('dateOfBirth')}</DetailRow>
                            <DetailRow label="Pohlaví">{ri('gender')}</DetailRow>
                            <DetailRow label="Státní příslušnost">{ri('nationality')}</DetailRow>
                            <DetailRow label="Rodné číslo">{ri('birthNumber')}</DetailRow>
                            <DetailRow label="Registrační číslo">{ri('registrationNumber')}</DetailRow>
                        </Section>

                        <Section title="KONTAKT">
                            <DetailRow label="E-mail">{ri('email')}</DetailRow>
                            <DetailRow label="Telefon">{ri('phone')}</DetailRow>
                        </Section>

                        <Section title="ADRESA">
                            {ri('address')}
                        </Section>

                        <Section title="DOPLŇKOVÉ INFORMACE">
                            <DetailRow label="Číslo čipu">{ri('chipNumber')}</DetailRow>
                            <DetailRow label="Číslo bankovního účtu">{ri('bankAccountNumber')}</DetailRow>
                            <DetailRow label="Stravovací omezení">{ri('dietaryRestrictions')}</DetailRow>
                        </Section>

                        <Section title="DOKLADY A LICENCE">
                            {ri('identityCard')}
                            <DetailRow label="Řidičský průkaz">{ri('drivingLicenseGroup')}</DetailRow>
                            {ri('medicalCourse')}
                            {ri('trainerLicense')}
                        </Section>

                        {(guardian || enrichedFieldNames.has('guardian')) && (
                            <Section title="ZÁKONNÝ ZÁSTUPCE">
                                {ri('guardian')}
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
                }}
            />
        );
    }

    return (
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
            </div>

            <Section title="OSOBNÍ ÚDAJE">
                <DetailRow label="Jméno">{member.firstName}</DetailRow>
                <DetailRow label="Příjmení">{member.lastName}</DetailRow>
                {member.dateOfBirth && (
                    <DetailRow label="Datum narození">{formatDate(member.dateOfBirth)}</DetailRow>
                )}
                {member.gender && (
                    <DetailRow label="Pohlaví">{GENDER_LABELS[member.gender] ?? member.gender}</DetailRow>
                )}
                {member.nationality && (
                    <DetailRow label="Státní příslušnost">{member.nationality}</DetailRow>
                )}
                {member.nationality === 'CZ' && member.birthNumber && (
                    <DetailRow label="Rodné číslo"><MaskedBirthNumber value={member.birthNumber}/></DetailRow>
                )}
            </Section>

            <Section title="KONTAKT">
                {member.email && (
                    <DetailRow label="E-mail">{member.email}</DetailRow>
                )}
                {member.phone && (
                    <DetailRow label="Telefon">{member.phone}</DetailRow>
                )}
            </Section>

            {address && (
                <Section title="ADRESA">
                    {address.street && <DetailRow label="Ulice">{address.street}</DetailRow>}
                    {address.city && <DetailRow label="Město">{address.city}</DetailRow>}
                    {address.postalCode && <DetailRow label="PSČ">{address.postalCode}</DetailRow>}
                    {address.country && <DetailRow label="Stát">{address.country}</DetailRow>}
                </Section>
            )}

            {hasSupplementaryInfo && (
                <Section title="DOPLŇKOVÉ INFORMACE">
                    {member.chipNumber && (
                        <DetailRow label="Číslo čipu">{member.chipNumber}</DetailRow>
                    )}
                    {member.bankAccountNumber && (
                        <DetailRow label="Číslo bankovního účtu">{member.bankAccountNumber}</DetailRow>
                    )}
                    {member.dietaryRestrictions && (
                        <DetailRow label="Stravovací omezení">{member.dietaryRestrictions}</DetailRow>
                    )}
                </Section>
            )}

            {hasDocuments && (
                <Section title="DOKLADY A LICENCE">
                    {identityCard?.cardNumber && (
                        <>
                            <DetailRow label="Číslo OP">{identityCard.cardNumber}</DetailRow>
                            {identityCard.validityDate && (
                                <DetailRow label="Platnost OP">{formatDate(identityCard.validityDate)}</DetailRow>
                            )}
                        </>
                    )}
                    {member.drivingLicenseGroup && (
                        <DetailRow label="Řidičský průkaz">{member.drivingLicenseGroup}</DetailRow>
                    )}
                    {medicalCourse?.completionDate && (
                        <>
                            <DetailRow label="Zdravotní kurz">{formatDate(medicalCourse.completionDate)}</DetailRow>
                            {medicalCourse.validityDate && (
                                <DetailRow label="Platnost ZK">{formatDate(medicalCourse.validityDate)}</DetailRow>
                            )}
                        </>
                    )}
                    {trainerLicense?.licenseNumber && (
                        <>
                            <DetailRow label="Trenérská licence">{trainerLicense.licenseNumber}</DetailRow>
                            {trainerLicense.validityDate && (
                                <DetailRow label="Platnost TL">{formatDate(trainerLicense.validityDate)}</DetailRow>
                            )}
                        </>
                    )}
                </Section>
            )}

            {guardian && (
                <Section title="ZÁKONNÝ ZÁSTUPCE">
                    <DetailRow label="Jméno">{guardian.firstName}</DetailRow>
                    <DetailRow label="Příjmení">{guardian.lastName}</DetailRow>
                    {guardian.relationship && (
                        <DetailRow label="Vztah">{guardian.relationship}</DetailRow>
                    )}
                    {guardian.email && (
                        <DetailRow label="E-mail">{guardian.email}</DetailRow>
                    )}
                    {guardian.phone && (
                        <DetailRow label="Telefon">{guardian.phone}</DetailRow>
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
};
