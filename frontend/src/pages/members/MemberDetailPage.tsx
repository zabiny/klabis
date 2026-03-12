import {type ReactElement, type ReactNode, useState} from "react";
import {PermissionsDialog} from "../../components/members/PermissionsDialog";
import {Link} from "react-router-dom";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {DetailRow, Skeleton} from "../../components/UI";
import {Badge} from "../../components/UI/Badge";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {type FormRenderHelpers, HalFormsForm} from "../../components/HalNavigator2/halforms";
import {klabisFieldsFactory} from "../../components/KlabisFieldsFactory";
import {formatDate} from "../../utils/dateUtils.ts";
import type {components} from "../../api/klabisApi";
import type {HalFormsProperty, HalFormsTemplate, HalResponse} from "../../api";
import {useMemberEditForm} from "./useMemberEditForm";
import {Banknote, Check, Pencil, Shield, ShieldCheck, UserX} from "lucide-react";
import {Section} from "./MemberSection";

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

const val = (value: ReactNode): ReactNode => value || '\u2014';

const MaskedBirthNumber = ({value}: { value: string }) => {
    const [revealed, setRevealed] = useState(false);
    const formatted = value.includes('/')
        ? value
        : (value.length >= 7 ? `${value.substring(0, 6)}/${value.substring(6)}` : value);
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

function isAdminTemplate(template: HalFormsTemplate): boolean {
    return template.properties.some(p => p.name === 'firstName' && !p.readOnly);
}

type ViewMode = 'other' | 'self' | 'admin';

function resolveViewMode(template: HalFormsTemplate | null): ViewMode {
    if (!template) return 'other';
    if (isAdminTemplate(template)) return 'admin';
    return 'self';
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
    const [isPermissionsDialogOpen, setIsPermissionsDialogOpen] = useState(false);

    const member = resourceData;
    const address = member.address;
    const guardian = member.guardian;
    const identityCard = member.identityCard;
    const medicalCourse = member.medicalCourse;
    const trainerLicense = member.trainerLicense;
    const refereeLicense = member.refereeLicense;
    const showDeactivation = member.active === false;

    const viewMode = resolveViewMode(template);
    const adminEdit = isEditing && viewMode === 'admin';
    const selfEdit = isEditing && viewMode === 'self';

    const enrichedTemplate = isEditing && template
        ? enrichTemplateWithReadOnlyFields(template, resourceData)
        : null;
    const enrichedFieldNames = enrichedTemplate
        ? new Set(enrichedTemplate.properties.map(p => p.name))
        : new Set<string>();

    const renderContent = (helpers?: FormRenderHelpers) => {
        const ri = (name: string): ReactNode =>
            isEditing && enrichedFieldNames.has(name) && helpers
                ? helpers.renderInput(name)
                : null;

        const leftColumn = (
            <div className="flex flex-col gap-8">
                {viewMode !== 'other' && (
                    <Section title="OSOBNÍ ÚDAJE">
                        <DetailRow label="Jméno">{ri('firstName') ?? val(member.firstName)}</DetailRow>
                        <DetailRow label="Příjmení">{ri('lastName') ?? val(member.lastName)}</DetailRow>
                        <DetailRow label="Datum narození">{ri('dateOfBirth') ?? val(member.dateOfBirth && formatDate(member.dateOfBirth))}</DetailRow>
                        <DetailRow label="Pohlaví">{ri('gender') ?? val(member.gender && (GENDER_LABELS[member.gender] ?? member.gender))}</DetailRow>
                        <DetailRow label="Státní příslušnost">{ri('nationality') ?? val(member.nationality)}</DetailRow>
                        {(isEditing || (member.nationality === 'CZ' && member.birthNumber)) && (
                            <DetailRow label="Rodné číslo">
                                {isEditing
                                    ? ri('birthNumber')
                                    : <MaskedBirthNumber value={member.birthNumber!}/>}
                            </DetailRow>
                        )}
                        {isEditing && (
                            <DetailRow label="Registrační číslo">{ri('registrationNumber')}</DetailRow>
                        )}
                    </Section>
                )}

                <Section title="KONTAKT">
                    <DetailRow label="E-mail">{ri('email') ?? val(member.email)}</DetailRow>
                    <DetailRow label="Telefon">{ri('phone') ?? val(member.phone)}</DetailRow>
                </Section>

                <Section title="ADRESA">
                    {isEditing ? ri('address') : (
                        address ? (
                            <>
                                <DetailRow label="Ulice">{val(address.street)}</DetailRow>
                                <DetailRow label="Město">{val(address.city)}</DetailRow>
                                <DetailRow label="PSČ">{val(address.postalCode)}</DetailRow>
                                <DetailRow label="Stát">{val(address.country)}</DetailRow>
                            </>
                        ) : <span className="text-sm text-text-tertiary">{'\u2014'}</span>
                    )}
                </Section>
            </div>
        );

        const rightColumn = viewMode !== 'other' ? (
            <div className="flex flex-col gap-8">
                <Section title="DOPLŇKOVÉ INFORMACE">
                    <DetailRow label="Číslo čipu">{ri('chipNumber') ?? val(member.chipNumber)}</DetailRow>
                    <DetailRow label={isEditing ? "Číslo bankovního účtu (nepovinné)" : "Číslo bankovního účtu"}>
                        {ri('bankAccountNumber') ?? val(member.bankAccountNumber)}
                        {isEditing && (
                            <p className="mt-1 text-sm text-text-tertiary">Pro proplácení cestovních nákladů a dalších výdajů spojených s klubem</p>
                        )}
                    </DetailRow>
                    <DetailRow label="Stravovací omezení">{ri('dietaryRestrictions') ?? val(member.dietaryRestrictions)}</DetailRow>
                </Section>

                <Section title="DOKLADY A LICENCE">
                    {isEditing ? ri('identityCard') : (
                        <DetailRow label="Občanský průkaz">
                            {identityCard?.cardNumber
                                ? <>{identityCard.cardNumber}{identityCard.validityDate && <span className="text-text-tertiary ml-2">(platný do {formatDate(identityCard.validityDate)})</span>}</>
                                : '\u2014'}
                        </DetailRow>
                    )}
                    <DetailRow label="Řidičský průkaz">{ri('drivingLicenseGroup') ?? val(member.drivingLicenseGroup)}</DetailRow>
                    {isEditing ? (
                        <div>
                            <p className="text-sm font-medium text-text-secondary mb-2">Zdravotní kurz</p>
                            {ri('medicalCourse')}
                        </div>
                    ) : (
                        <DetailRow label="Zdravotní kurz">
                            {medicalCourse?.completionDate ? (
                                <span className="inline-flex items-center gap-2">
                                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-50 text-green-700">Ano</span>
                                    {medicalCourse.validityDate && <span className="text-text-tertiary text-sm">platný do {formatDate(medicalCourse.validityDate)}</span>}
                                </span>
                            ) : (
                                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-surface-raised text-text-secondary">Ne</span>
                            )}
                        </DetailRow>
                    )}
                    {isEditing ? (
                        <div>
                            <p className="text-sm font-medium text-text-secondary mb-2">Trenér</p>
                            {ri('trainerLicense')}
                        </div>
                    ) : (
                        <DetailRow label="Trenér">
                            {trainerLicense?.level ? (
                                <span className="inline-flex items-center gap-2">
                                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-orange-50 text-orange-700">{trainerLicense.level}</span>
                                    {trainerLicense.validityDate && <span className="text-text-tertiary text-sm">platná do {formatDate(trainerLicense.validityDate)}</span>}
                                </span>
                            ) : '\u2014'}
                        </DetailRow>
                    )}
                    {isEditing ? (
                        <div>
                            <p className="text-sm font-medium text-text-secondary mb-2">Rozhodčí</p>
                            {ri('refereeLicense')}
                        </div>
                    ) : (
                        <DetailRow label="Rozhodčí">
                            {refereeLicense?.level ? (
                                <span className="inline-flex items-center gap-2">
                                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-50 text-blue-700">{refereeLicense.level}</span>
                                    {refereeLicense.validityDate && <span className="text-text-tertiary text-sm">platná do {formatDate(refereeLicense.validityDate)}</span>}
                                </span>
                            ) : '\u2014'}
                        </DetailRow>
                    )}
                </Section>
            </div>
        ) : null;

        return (
            <div className="flex flex-col gap-8">
                <div>
                    <Link to="/members" className="text-sm text-primary hover:text-primary-light">
                        &larr; Zpět na seznam
                    </Link>
                </div>

                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-4">
                            <h1 className="text-3xl font-bold text-text-primary">
                                {member.firstName} {member.lastName}
                            </h1>
                            {!isEditing && (
                                <Badge variant={member.active ? 'success' : 'default'} size="sm">
                                    {member.active ? 'Aktivní' : 'Neaktivní'}
                                </Badge>
                            )}
                            {adminEdit && (
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-md bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
                                    <ShieldCheck className="w-3.5 h-3.5"/>
                                    Admin — editace všech polí
                                </span>
                            )}
                            {selfEdit && (
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-md bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                                    Vlastní profil — omezená editace
                                </span>
                            )}
                        </div>
                        <span className="text-sm text-text-secondary">{member.registrationNumber}</span>
                    </div>

                    {!isEditing && (
                        <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                            {viewMode === 'admin' && (
                                <>
                                    <button
                                        type="button"
                                        onClick={startEditing}
                                        className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light"
                                    >
                                        <Pencil className="w-4 h-4"/>
                                        Upravit profil
                                    </button>
                                    <button
                                        type="button"
                                        className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                                    >
                                        <Banknote className="w-4 h-4 text-green-600"/>
                                        Vložit / Vybrat
                                    </button>
                                    {hasLink('permissions') && (
                                        <button
                                            type="button"
                                            onClick={() => setIsPermissionsDialogOpen(true)}
                                            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                                        >
                                            <Shield className="w-4 h-4"/>
                                            Oprávnění
                                        </button>
                                    )}
                                    <HalFormButton name="suspendMember" modal={true} label="Ukončit členství" variant="danger" icon={<UserX className="w-4 h-4"/>} dialogTitle="Ukončení členství"/>
                                    <HalFormButton name="resumeMember" modal={true} label="Reaktivovat"/>
                                </>
                            )}
                            {viewMode === 'self' && (
                                <>
                                    <button
                                        type="button"
                                        className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                                    >
                                        Členské příspěvky
                                    </button>
                                    <button
                                        type="button"
                                        onClick={startEditing}
                                        className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light"
                                    >
                                        <Pencil className="w-4 h-4"/>
                                        Upravit profil
                                    </button>
                                </>
                            )}
                        </div>
                    )}
                </div>

                <hr className="border-border"/>

                {viewMode !== 'other' ? (
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                        {leftColumn}
                        {rightColumn}
                    </div>
                ) : leftColumn}

                {(guardian || (isEditing && enrichedFieldNames.has('guardian'))) && (
                    <Section title="ZÁKONNÝ ZÁSTUPCE">
                        {isEditing ? ri('guardian') : (
                            <>
                                <DetailRow label="Jméno">{val(guardian?.firstName)}</DetailRow>
                                <DetailRow label="Příjmení">{val(guardian?.lastName)}</DetailRow>
                                <DetailRow label="Vztah">{val(guardian?.relationship)}</DetailRow>
                                <DetailRow label="E-mail">{val(guardian?.email)}</DetailRow>
                                <DetailRow label="Telefon">{val(guardian?.phone)}</DetailRow>
                            </>
                        )}
                    </Section>
                )}

                {showDeactivation && (
                    <Section title="DEAKTIVACE">
                        {member.deactivationReason && (
                            <DetailRow label="Důvod">
                                {DEACTIVATION_REASON_LABELS[member.deactivationReason] ?? member.deactivationReason}
                            </DetailRow>
                        )}
                        {member.deactivatedAt && (
                            <DetailRow label="Datum">{formatDate(member.deactivatedAt)}</DetailRow>
                        )}
                        {member.deactivationNote && (
                            <DetailRow label="Poznámka">{member.deactivationNote}</DetailRow>
                        )}
                    </Section>
                )}

                {isEditing && (
                    <div className="flex justify-end gap-3 pt-4 border-t border-border">
                        <button
                            type="button"
                            onClick={cancelEditing}
                            className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
                        >
                            Zrušit
                        </button>
                        {helpers?.renderField('submit')}
                    </div>
                )}
            </div>
        );
    };

    const permissionsUrl = route.getResourceLink('permissions')?.href ?? '';

    return (
        <>
            <PermissionsDialog
                isOpen={isPermissionsDialogOpen}
                onClose={() => setIsPermissionsDialogOpen(false)}
                permissionsUrl={permissionsUrl}
                memberName={`${member.firstName} ${member.lastName}`}
                memberRegistrationNumber={member.registrationNumber ?? undefined}
            />
            {isEditing && enrichedTemplate ? (
                <HalFormsForm
                    data={resourceData as Record<string, unknown>}
                    template={enrichedTemplate}
                    onSubmit={handleSubmit}
                    fieldsFactory={klabisFieldsFactory}
                    submitButtonLabel="Uložit změny"
                    submitIcon={<Check className="w-4 h-4"/>}
                    isSubmitting={editForm.isSubmitting}
                    renderForm={(helpers) => renderContent(helpers) as ReactElement}
                />
            ) : renderContent() as ReactElement}
        </>
    );
};
