import {type ReactElement, type ReactNode, useState} from "react";
import {PermissionsDialog} from "../../components/members/PermissionsDialog";
import {usePermissionsEditor} from "../../hooks/usePermissionsEditor.ts";
import {Link, useLocation, useNavigate} from "react-router-dom";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {Badge, Button, DetailRow, Modal, Skeleton} from "../../components/UI";
import {ErrorPage} from "../ErrorPage.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {type FormRenderHelpers} from "../../components/HalNavigator2/halforms";
import {formatDate} from "../../utils/dateUtils.ts";
import type {components} from "../../api/klabisApi";
import type {HalFormsTemplate, HalResponse} from "../../api";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {Banknote, Check, Dumbbell, Heart, KeyRound, Pencil, Shield, UserX} from "lucide-react";
import {Section} from "./MemberSection";
import {BirthNumberConditionalField} from "./BirthNumberConditionalField";
import {isCzNationality} from "./isCzNationality";
import {labels, getEnumLabel} from "../../localization";
import {SuspensionWarningDialog} from "./SuspensionWarningDialog.tsx";
import {NegativeBalanceSuspensionDialog} from "./NegativeBalanceSuspensionDialog.tsx";
import {useSuspendMemberAction} from "./useSuspendMemberAction.ts";
import {useInlineEditing} from "../../hooks/useInlineEditing.ts";
import {CalendarFeedSection} from "./CalendarFeedSection.tsx";
import {MemberFeeSection} from "./MemberFeeSection.tsx";
import {ChangePasswordDialog} from "../../components/auth/ChangePasswordDialog.tsx";

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

export const MemberDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error, hasLink, route} = useHalPageData<MemberDetail>();
    const location = useLocation();
    const initialEditing = !!(location.state as { editing?: boolean })?.editing;

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <ErrorPage error={error}/>;
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
    const [isPermissionsDialogOpen, setIsPermissionsDialogOpen] = useState(false);
    const [suspendMemberModal, setSuspendMemberModal] = useState(false);

    const {
        suspensionWarning,
        negativeBalanceWarning,
        clearSuspensionWarning,
        clearNegativeBalanceWarning,
        onSubmitError: onSuspendSubmitError,
    } = useSuspendMemberAction({closeActionModal: () => setSuspendMemberModal(false)});
    const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
    const navigate = useNavigate();

    const icalTokenLink = resourceData._links?.['ical-token'];
    const icalTokenHref = icalTokenLink != null
        ? (Array.isArray(icalTokenLink)
            ? (icalTokenLink[0] as {href: string}).href
            : (icalTokenLink as {href: string}).href)
        : null;

    // The ical-token link is only present on the authenticated user's own profile
    const isOwnProfile = icalTokenHref != null;

    const member = resourceData;
    const address = member.address;
    const guardian = member.guardian;
    const identityCard = member.identityCard;
    const medicalCourse = member.medicalCourse;
    const trainerLicense = member.trainerLicense;
    const refereeLicense = member.refereeLicense;
    const showDeactivation = member.active === false;

    const selfLink = resourceData._links?.self;
    const selfHref = selfLink ? (Array.isArray(selfLink) ? (selfLink[0] as {href: string}).href : (selfLink as {href: string}).href) : null;
    const selfMemberId = selfHref?.split('/').pop() ?? '';

    const feeSummaryLink = resourceData._links?.feeSummary;
    const feeSummaryHref = feeSummaryLink
        ? (Array.isArray(feeSummaryLink)
            ? (feeSummaryLink[0] as {href: string}).href
            : (feeSummaryLink as {href: string}).href)
        : null;

    const template: HalFormsTemplate | null = resourceData?._templates?.updateMember ?? null;
    const hasEditTemplate = template !== null;

    const {isEditing, enrichedTemplate, enrichedFieldNames, startEditing, cancelEditing, postprocessPayload} =
        useInlineEditing(template, resourceData as Record<string, unknown>, {
            initialEditing,
            fieldTypeOverrides: MEMBER_FIELD_TYPES,
            onCancel: initialEditing ? () => navigate(-1) : undefined,
        });


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

                    {!isEditing && (
                        <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                            {hasEditTemplate && (
                                <Button
                                    variant="primary"
                                    onClick={startEditing}
                                    startIcon={<Pencil className="w-4 h-4"/>}
                                >
                                    {labels.templates.updateMember}
                                </Button>
                            )}
                            {isOwnProfile && (
                                <Button
                                    variant="secondary"
                                    onClick={() => setIsChangePasswordOpen(true)}
                                    startIcon={<KeyRound className="w-4 h-4"/>}
                                >
                                    {labels.changePassword.sectionButtonLabel}
                                </Button>
                            )}
                            {hasLink('account') && (
                                <Button
                                    variant="secondary"
                                    onClick={() => route.navigateToResource(route.getResourceLink('account')!)}
                                    startIcon={<Banknote className="w-4 h-4"/>}
                                >
                                    {labels.finance.openMemberAccount}
                                </Button>
                            )}
                            {hasLink('trainingGroup') && (
                                <Button
                                    variant="secondary"
                                    onClick={() => route.navigateToResource(route.getResourceLink('trainingGroup')!)}
                                    startIcon={<Dumbbell className="w-4 h-4"/>}
                                >
                                    {labels.links.trainingGroup}
                                </Button>
                            )}
                            {hasLink('familyGroup') && (
                                <Button
                                    variant="secondary"
                                    onClick={() => route.navigateToResource(route.getResourceLink('familyGroup')!)}
                                    startIcon={<Heart className="w-4 h-4"/>}
                                >
                                    {labels.links.familyGroup}
                                </Button>
                            )}
                            {hasEditTemplate && hasLink('permissions') && (
                                <Button
                                    variant="secondary"
                                    onClick={() => setIsPermissionsDialogOpen(true)}
                                    startIcon={<Shield className="w-4 h-4"/>}
                                >
                                    {labels.permissions['MEMBERS:PERMISSIONS'].label}
                                </Button>
                            )}
                            {hasEditTemplate && resourceData._templates?.suspendMember && (
                                <Button
                                    variant="danger"
                                    onClick={() => setSuspendMemberModal(true)}
                                    startIcon={<UserX className="w-4 h-4"/>}
                                >
                                    {labels.templates.suspendMember}
                                </Button>
                            )}
                            {hasEditTemplate && <HalFormButton name="resumeMember" modal={true}/>}
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
                        {member.suspensionReason && (
                            <DetailRow label="Důvod">
                                {getEnumLabel('deactivationReason', member.suspensionReason)}
                            </DetailRow>
                        )}
                        {member.suspendedAt && (
                            <DetailRow label="Datum">{formatDate(member.suspendedAt)}</DetailRow>
                        )}
                        {member.suspensionNote && (
                            <DetailRow label="Poznámka">{member.suspensionNote}</DetailRow>
                        )}
                        {member.suspendedBy && (
                            <DetailRow label="Deaktivoval/a">{member.suspendedBy}</DetailRow>
                        )}
                    </Section>
                )}

                {!isEditing && icalTokenHref && (
                    <CalendarFeedSection icalTokenHref={icalTokenHref}/>
                )}

                {!isEditing && member.active && feeSummaryHref && (
                    <MemberFeeSection
                        feeSummaryHref={feeSummaryHref}
                        memberId={selfMemberId}
                    />
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

    const permissionsUrl = route.getResourceLink('permissions')?.href;

    const permissionsEditor = usePermissionsEditor(
        permissionsUrl,
        {enabled: isPermissionsDialogOpen, onSaved: () => setIsPermissionsDialogOpen(false)},
    );

    const suspendTemplate = resourceData._templates?.suspendMember ?? null;

    return (
        <>
            <ChangePasswordDialog
                isOpen={isChangePasswordOpen}
                onClose={() => setIsChangePasswordOpen(false)}
            />
            <SuspensionWarningDialog
                isOpen={suspensionWarning !== null}
                onClose={clearSuspensionWarning}
                affectedGroups={suspensionWarning ?? []}
            />
            <NegativeBalanceSuspensionDialog
                isOpen={negativeBalanceWarning !== null}
                onClose={clearNegativeBalanceWarning}
                warning={negativeBalanceWarning}
            />
            {suspendTemplate && suspendMemberModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setSuspendMemberModal(false)}
                    title={labels.dialogTitles.suspendMember}
                    size="2xl"
                >
                    <HalFormDisplay
                        template={suspendTemplate}
                        templateName="suspendMember"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setSuspendMemberModal(false)}
                        onSubmitError={onSuspendSubmitError}
                    />
                </Modal>
            )}
            <PermissionsDialog
                isOpen={isPermissionsDialogOpen}
                onClose={() => setIsPermissionsDialogOpen(false)}
                memberName={`${member.firstName} ${member.lastName}`}
                memberRegistrationNumber={member.registrationNumber ?? undefined}
                {...permissionsEditor}
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
