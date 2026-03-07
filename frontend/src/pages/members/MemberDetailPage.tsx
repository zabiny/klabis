import {type ReactElement, useState} from "react";
import {Link} from "react-router-dom";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {Skeleton} from "../../components/UI";
import {Badge} from "../../components/UI/Badge";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {formatDate} from "../../utils/dateUtils.ts";
import type {components} from "../../api/klabisApi";
import type {HalResponse} from "../../api";

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

    const member = resourceData;
    const address = member.address;
    const guardian = member.guardian;
    const identityCard = member.identityCard;
    const medicalCourse = member.medicalCourse;
    const trainerLicense = member.trainerLicense;

    const hasSupplementaryInfo = member.chipNumber || member.bankAccountNumber || member.dietaryRestrictions;
    const hasDocuments = identityCard?.cardNumber || member.drivingLicenseGroup || medicalCourse?.completionDate || trainerLicense?.licenseNumber;
    const showDeactivation = member.active === false && member.deactivationReason;

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
                <HalFormButton name="default" modal={false}/>
                <HalFormButton name="terminate" modal={true}/>
                <HalFormButton name="reactivate" modal={true}/>
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
                <DetailRow label="Jméno">{member.firstName} {member.lastName}</DetailRow>
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
                    <DetailRow label="Rodné číslo">
                        <MaskedBirthNumber value={member.birthNumber}/>
                    </DetailRow>
                )}
            </Section>

            <Section title="KONTAKT">
                {member.email && <DetailRow label="E-mail">{member.email}</DetailRow>}
                {member.phone && <DetailRow label="Telefon">{member.phone}</DetailRow>}
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
                    {member.chipNumber && <DetailRow label="Číslo čipu">{member.chipNumber}</DetailRow>}
                    {member.bankAccountNumber && <DetailRow label="Číslo bankovního účtu">{member.bankAccountNumber}</DetailRow>}
                    {member.dietaryRestrictions && <DetailRow label="Stravovací omezení">{member.dietaryRestrictions}</DetailRow>}
                </Section>
            )}

            {hasDocuments && (
                <Section title="DOKLADY A LICENCE">
                    {identityCard?.cardNumber && (
                        <DetailRow label="Občanský průkaz">
                            {identityCard.cardNumber}
                            {identityCard.validityDate && ` (platnost do ${formatDate(identityCard.validityDate)})`}
                        </DetailRow>
                    )}
                    {member.drivingLicenseGroup && (
                        <DetailRow label="Řidičský průkaz">{member.drivingLicenseGroup}</DetailRow>
                    )}
                    {medicalCourse?.completionDate && (
                        <DetailRow label="Zdravotní kurz">
                            {formatDate(medicalCourse.completionDate)}
                            {medicalCourse.validityDate && ` – platnost do ${formatDate(medicalCourse.validityDate)}`}
                        </DetailRow>
                    )}
                    {trainerLicense?.licenseNumber && (
                        <DetailRow label="Trenérská licence">
                            {trainerLicense.licenseNumber}
                            {trainerLicense.validityDate && ` (platnost do ${formatDate(trainerLicense.validityDate)})`}
                        </DetailRow>
                    )}
                </Section>
            )}

            {guardian && (
                <Section title="ZÁKONNÝ ZÁSTUPCE">
                    <DetailRow label="Jméno">{guardian.firstName} {guardian.lastName}</DetailRow>
                    {guardian.relationship && <DetailRow label="Vztah">{guardian.relationship}</DetailRow>}
                    {guardian.email && <DetailRow label="E-mail">{guardian.email}</DetailRow>}
                    {guardian.phone && <DetailRow label="Telefon">{guardian.phone}</DetailRow>}
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
