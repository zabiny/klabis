import {type ReactElement, type ReactNode, useMemo, useState} from "react";
import {PermissionsDialog} from "../../components/members/PermissionsDialog";
import {Link, useLocation, useNavigate} from "react-router-dom";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {Alert, Badge, Button, DetailRow, Skeleton} from "../../components/UI";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {type FormRenderHelpers} from "../../components/HalNavigator2/halforms";
import {formatDate} from "../../utils/dateUtils.ts";
import type {components} from "../../api/klabisApi";
import type {HalFormsProperty, HalFormsTemplate, HalResponse} from "../../api";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {Banknote, Check, Pencil, Shield, UserX} from "lucide-react";
import {Section} from "./MemberSection";
import {BirthNumberConditionalField, isCzNationality} from "./BirthNumberConditionalField";
import {labels, getEnumLabel} from "../../localization";

type MemberDetail = components['schemas']['EntityModelMemberDetailsResponse'] & HalResponse;

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
                {revealed ? labels.ui.hide : labels.ui.reveal}
            </button>
        </span>
    );
};

const MEMBER_FIELD_TYPES: Record<string, string> = {
    gender: 'Gender',
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
            type: MEMBER_FIELD_TYPES[key] ?? 'text',
            readOnly: true,
        }));

    return {
        ...template,
        properties: [...template.properties, ...readOnlyProps],
    };
}

export const MemberDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error, hasLink, route} = useHalPageData<MemberDetail>();
    const location = useLocation();
    const initialEditing = !!(location.state as { editing?: boolean })?.editing;

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <MemberDetailContent resourceData={resourceData} hasLink={hasLink} route={route} initialEditing={initialEditing}/>;
};

interface MemberDetailContentProps {
    resourceData: MemberDetail;
    hasLink: (name: string) => boolean;
    route: ReturnType<typeof useHalPageData>['route'];
    initialEditing?: boolean;
}

const MemberDetailContent = ({resourceData, hasLink, route, initialEditing = false}: MemberDetailContentProps) => {
    const [isEditing, setIsEditing] = useState(initialEditing);
    const [isPermissionsDialogOpen, setIsPermissionsDialogOpen] = useState(false);
    const navigate = useNavigate();

    const member = resourceData;
    const address = member.address;
    const guardian = member.guardian;
    const identityCard = member.identityCard;
    const medicalCourse = member.medicalCourse;
    const trainerLicense = member.trainerLicense;
    const refereeLicense = member.refereeLicense;
    const showDeactivation = member.active === false;

    const template: HalFormsTemplate | null = resourceData?._templates?.updateMember ?? null;
    const hasEditTemplate = template !== null;

    const enrichedTemplate = useMemo(() => {
        if (!isEditing || !template) return null;
        return enrichTemplateWithReadOnlyFields(template, resourceData);
    }, [isEditing, template, resourceData]);

    const enrichedFieldNames = useMemo(() =>
            enrichedTemplate
                ? new Set(enrichedTemplate.properties.map(p => p.name))
                : new Set<string>(),
        [enrichedTemplate]);

    const startEditing = () => setIsEditing(true);
    const cancelEditing = () => {
        if (initialEditing) {
            navigate(-1);
        } else {
            setIsEditing(false);
        }
    };

    const originalEditableFieldNames = useMemo(() =>
            template ? new Set(template.properties.map(p => p.name)) : new Set<string>(),
        [template]);

    const postprocessPayload = (payload: Record<string, unknown>): Record<string, unknown> =>
        Object.fromEntries(
            Object.entries(payload).filter(([key]) => originalEditableFieldNames.has(key))
        );

    const renderContent = (helpers?: FormRenderHelpers) => {
        const ri = (name: string): ReactNode =>
            isEditing && enrichedFieldNames.has(name) && helpers
                ? helpers.renderInput(name)
                : null;

        const leftColumn = (
            <div className="flex flex-col gap-8">
                {hasEditTemplate && (
                    <Section title={labels.sections.personalInfo}>
                        <DetailRow label={labels.fields.firstName}>{ri('firstName') ?? val(member.firstName)}</DetailRow>
                        <DetailRow label={labels.fields.lastName}>{ri('lastName') ?? val(member.lastName)}</DetailRow>
                        <DetailRow label={labels.fields.dateOfBirth}>{ri('dateOfBirth') ?? val(member.dateOfBirth && formatDate(member.dateOfBirth))}</DetailRow>
                        <DetailRow label={labels.fields.gender}>{ri('gender') ?? val(member.gender && getEnumLabel('gender', member.gender))}</DetailRow>
                        <DetailRow label={labels.fields.nationality}>{ri('nationality') ?? val(member.nationality)}</DetailRow>
                        {isEditing
                            ? enrichedFieldNames.has('birthNumber') && <BirthNumberConditionalField renderInput={ri}/>
                            : (isCzNationality(member.nationality) && member.birthNumber && (
                                <DetailRow label={labels.fields.birthNumber}>
                                    <MaskedBirthNumber value={member.birthNumber}/>
                                </DetailRow>
                            ))
                        }
                        {isEditing && (
                            <DetailRow label={labels.fields.registrationNumber}>{ri('registrationNumber')}</DetailRow>
                        )}
                    </Section>
                )}

                <Section title={labels.sections.contact}>
                    <DetailRow label={labels.fields.email}>{ri('email') ?? val(member.email)}</DetailRow>
                    <DetailRow label={labels.fields.phone}>{ri('phone') ?? val(member.phone)}</DetailRow>
                </Section>

                <Section title={labels.sections.address}>
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

        const rightColumn = hasEditTemplate ? (
            <div className="flex flex-col gap-8">
                <Section title={labels.sections.supplementary}>
                    <DetailRow label={labels.fields.chipNumber}>{ri('chipNumber') ?? val(member.chipNumber)}</DetailRow>
                    <DetailRow label={isEditing ? `${labels.fields.bankAccountNumber} (nepovinné)` : labels.fields.bankAccountNumber}>
                        {ri('bankAccountNumber') ?? val(member.bankAccountNumber)}
                        {isEditing && (
                            <p className="mt-1 text-sm text-text-tertiary">Pro proplácení cestovních nákladů a dalších výdajů spojených s klubem</p>
                        )}
                    </DetailRow>
                    <DetailRow label={labels.fields.dietaryRestrictions}>{ri('dietaryRestrictions') ?? val(member.dietaryRestrictions)}</DetailRow>
                </Section>

                <Section title={labels.sections.documentsAndLicenses}>
                    {isEditing ? ri('identityCard') : (
                        <DetailRow label="Občanský průkaz">
                            {identityCard?.cardNumber
                                ? <>{identityCard.cardNumber}{identityCard.validityDate && <span className="text-text-tertiary ml-2">(platný do {formatDate(identityCard.validityDate)})</span>}</>
                                : '\u2014'}
                        </DetailRow>
                    )}
                    <DetailRow label={labels.fields.drivingLicenseGroup}>{ri('drivingLicenseGroup') ?? val(member.drivingLicenseGroup)}</DetailRow>
                    {isEditing ? (
                        <div>
                            <p className="text-sm font-medium text-text-secondary mb-2">Zdravotní kurz</p>
                            {ri('medicalCourse')}
                        </div>
                    ) : (
                        <DetailRow label="Zdravotní kurz">
                            {medicalCourse?.completionDate ? (
                                <span className="inline-flex items-center gap-2">
                                    <Badge variant="success" size="sm">Ano</Badge>
                                    {medicalCourse.validityDate && <span className="text-text-tertiary text-sm">platný do {formatDate(medicalCourse.validityDate)}</span>}
                                </span>
                            ) : (
                                <Badge variant="default" size="sm">Ne</Badge>
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
                                    <Badge variant="orange" size="sm">{trainerLicense.level}</Badge>
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
                                    <Badge variant="blue" size="sm">{refereeLicense.level}</Badge>
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
                        {labels.ui.backToList}
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
                                    {member.active ? labels.enums.memberStatus.active : labels.enums.memberStatus.inactive}
                                </Badge>
                            )}
                        </div>
                        <span className="text-sm text-text-secondary">{member.registrationNumber}</span>
                    </div>

                    {!isEditing && hasEditTemplate && (
                        <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                            <Button
                                variant="primary"
                                onClick={startEditing}
                                startIcon={<Pencil className="w-4 h-4"/>}
                            >
                                {labels.templates.updateMember}
                            </Button>
                            <Button
                                variant="secondary"
                                startIcon={<Banknote className="w-4 h-4 text-green-600"/>}
                            >
                                Vložit / Vybrat
                            </Button>
                            {hasLink('permissions') && (
                                <Button
                                    variant="secondary"
                                    onClick={() => setIsPermissionsDialogOpen(true)}
                                    startIcon={<Shield className="w-4 h-4"/>}
                                >
                                    {labels.permissions['MEMBERS:PERMISSIONS'].label}
                                </Button>
                            )}
                            <HalFormButton name="suspendMember" modal={true} label={labels.templates.suspendMember} variant="danger" icon={<UserX className="w-4 h-4"/>} dialogTitle={labels.dialogTitles.suspendMember}/>
                            <HalFormButton name="resumeMember" modal={true} label={labels.templates.resumeMember} dialogTitle={labels.dialogTitles.resumeMember}/>
                        </div>
                    )}
                </div>

                <hr className="border-border"/>

                {hasEditTemplate ? (
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                        {leftColumn}
                        {rightColumn}
                    </div>
                ) : leftColumn}

                {(guardian || (isEditing && enrichedFieldNames.has('guardian'))) && (
                    <Section title={labels.sections.guardian}>
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
                    <Section title={labels.sections.deactivation}>
                        {member.deactivationReason && (
                            <DetailRow label="Důvod">
                                {getEnumLabel('deactivationReason', member.deactivationReason)}
                            </DetailRow>
                        )}
                        {member.deactivatedAt && (
                            <DetailRow label="Datum">{formatDate(member.deactivatedAt)}</DetailRow>
                        )}
                        {member.deactivationNote && (
                            <DetailRow label="Poznámka">{member.deactivationNote}</DetailRow>
                        )}
                        {member.deactivatedBy && (
                            <DetailRow label="Deaktivoval/a">{member.deactivatedBy}</DetailRow>
                        )}
                    </Section>
                )}

                {isEditing && (
                    <div className="flex justify-end gap-3 pt-4 border-t border-border">
                        <Button
                            variant="secondary"
                            onClick={cancelEditing}
                        >
                            {labels.buttons.cancel}
                        </Button>
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
                <HalFormDisplay
                    template={enrichedTemplate}
                    templateName="updateMember"
                    resourceData={resourceData as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={cancelEditing}
                    postprocessPayload={postprocessPayload}
                    successMessage={labels.ui.savedSuccessfully}
                    submitButtonLabel={labels.buttons.saveChanges}
                    submitIcon={<Check className="w-4 h-4"/>}
                    customLayout={renderContent}
                />
            ) : renderContent() as ReactElement}
        </>
    );
};
