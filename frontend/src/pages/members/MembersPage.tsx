import {type ReactElement, useCallback, useMemo, useState} from "react";
import {useSearchParams} from "react-router-dom";
import type {EntityModel, HalFormsTemplate, HalResourceLinks} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {PermissionsDialog} from "../../components/members/PermissionsDialog.tsx";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {Button, DetailRow, Modal} from "../../components/UI";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {Section} from "./MemberSection.tsx";
import {BirthNumberConditionalField} from "./BirthNumberConditionalField.tsx";
import type {HalFormPanelRenderHelpers} from "../../components/HalNavigator2/HalFormPanel.tsx";
import {Pencil, PiggyBank, Shield, UserCheck, UserX} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {labels} from "../../localization";
import {SuspensionWarningDialog, type AffectedGroup} from "./SuspensionWarningDialog.tsx";
import {parseSuspensionWarning409} from "./suspensionUtils.ts";
import {DEFAULT_MEMBER_STATUS, MembersFilterBar, type MembersFilterValue} from "../../components/members/MembersFilterBar.tsx";
import {useDefaultSearchParam} from "../../hooks/useDefaultSearchParam.ts";
import {usePermissionsEditor} from "../../hooks/usePermissionsEditor.ts";

type MemberSummaryData = EntityModel<{
    id: string,
    registrationNumber: string,
    lastName: string,
    firstName: string,
    email: string | null,
    active: boolean | null,
}> & {
    _templates?: Record<string, HalFormsTemplate>;
    _links: Record<string, HalResourceLinks>;
    [key: string]: unknown;
};

interface MemberActionModalState {
    member: MemberSummaryData;
    templateName: string;
    template: HalFormsTemplate;
}

interface MemberPermissionsDialogState {
    member: MemberSummaryData;
    permissionsUrl: string;
}

export const MembersPage = (): ReactElement => {
    const {route, resourceData} = useHalPageData();
    const [searchParams, setSearchParams] = useSearchParams();
    const [actionModal, setActionModal] = useState<MemberActionModalState | null>(null);
    const [permissionsDialog, setPermissionsDialog] = useState<MemberPermissionsDialogState | null>(null);
    const [suspensionWarning, setSuspensionWarning] = useState<AffectedGroup[] | null>(null);

    const hasManageAuthority = resourceData?._templates?.registerMember !== undefined;

    const urlStatus = searchParams.get('status');
    const urlQ = searchParams.get('q') ?? '';

    useDefaultSearchParam('status', DEFAULT_MEMBER_STATUS);

    const filterValue: MembersFilterValue = useMemo(() => ({
        q: urlQ,
        status: (urlStatus ?? DEFAULT_MEMBER_STATUS) as MembersFilterValue['status'],
    }), [urlQ, urlStatus]);

    const handleFilterChange = useCallback((next: MembersFilterValue) => {
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);
            params.set('status', next.status);
            if (next.q) { params.set('q', next.q); } else { params.delete('q'); }
            return params;
        });
    }, [setSearchParams]);

    const extraParams = useMemo((): Record<string, string> => {
        const params: Record<string, string> = {};
        if (urlStatus) params.status = urlStatus;
        if (urlQ && urlQ.length >= 2) params.q = urlQ;
        return params;
    }, [urlStatus, urlQ]);

    const openActionModal = (member: MemberSummaryData, templateName: string) => {
        const template = member._templates?.[templateName];
        if (!template) return;
        setActionModal({member, templateName, template});
    };


    const openPermissionsDialog = (member: MemberSummaryData) => {
        const permissionsLink = member._links?.permissions;
        if (!permissionsLink) return;
        const link = Array.isArray(permissionsLink) ? permissionsLink[0] : permissionsLink;
        if (!link?.href) return;
        setPermissionsDialog({member, permissionsUrl: link.href});
    };

    const openAccountPage = (member: MemberSummaryData) => {
        const accountLink = member._links?.account;
        if (!accountLink) return;
        const link = Array.isArray(accountLink) ? accountLink[0] : accountLink;
        if (!link?.href) return;
        route.navigateToResource(link);
    };

    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const member = item as unknown as MemberSummaryData;
        const hasEditTemplate = !!member._templates?.updateMember;
        const hasPermissionsLink = !!member._links?.permissions;
        const hasAccountLink = !!member._links?.account;
        const hasSuspendTemplate = !!member._templates?.suspendMember;
        const hasResumeTemplate = !!member._templates?.resumeMember;

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {hasAccountLink && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.finance.openMemberAccount}
                        className="text-primary"
                        onClick={(e) => {
                            e.stopPropagation();
                            openAccountPage(member);
                        }}
                    >
                        <PiggyBank className="w-4 h-4"/>
                    </Button>
                )}
                {hasEditTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.buttons.edit}
                        className="text-primary"
                        onClick={(e) => {
                            e.stopPropagation();
                            route.navigateToResource(member, {state: {editing: true}});
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
                {hasPermissionsLink && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.permissions['MEMBERS:PERMISSIONS'].label}
                        className="text-gray-500"
                        onClick={(e) => {
                            e.stopPropagation();
                            openPermissionsDialog(member);
                        }}
                    >
                        <Shield className="w-4 h-4"/>
                    </Button>
                )}
                {hasSuspendTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.suspendMember}
                        className="text-red-600"
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(member, 'suspendMember');
                        }}
                    >
                        <UserX className="w-4 h-4"/>
                    </Button>
                )}
                {hasResumeTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.resumeMember}
                        className="text-green-600"
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(member, 'resumeMember');
                        }}
                    >
                        <UserCheck className="w-4 h-4"/>
                    </Button>
                )}
            </div>
        );
    };

    const renderEmailCell = ({item}: TableCellRenderProps) => {
        return (item.email as string | null) ?? null;
    };

    const memberName = permissionsDialog
        ? `${permissionsDialog.member.firstName ?? ''} ${permissionsDialog.member.lastName ?? ''}`.trim()
        : '';

    const memberRegistrationNumber = permissionsDialog
        ? permissionsDialog.member.registrationNumber ?? undefined
        : undefined;

    const permissionsEditor = usePermissionsEditor(
        permissionsDialog?.permissionsUrl,
        {enabled: !!permissionsDialog, onSaved: () => setPermissionsDialog(null)},
    );

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.members}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.membersList}</h2>
                    <div className="flex gap-2">
                        <HalFormButton name="registerMember" modal={false}>
                            {({renderInput, renderField, hasField, hasType}: HalFormPanelRenderHelpers) => {
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
                                            {renderField('cancel')}
                                            {renderField('submit')}
                                        </div>
                                    </div>
                                );
                            }}
                        </HalFormButton>
                    </div>
                </div>
                <MembersFilterBar
                    value={filterValue}
                    onChange={handleFilterChange}
                    hasManageAuthority={hasManageAuthority}
                />
                <HalEmbeddedTable<MemberSummaryData> collectionName={"memberSummaryResponseList"}
                                                      tableId="members"
                                                      defaultOrderBy={"lastName"}
                                                      hideEmptyColumns
                                                      extraParams={extraParams}
                                                      emptyMessage={labels.membersFilter.emptyState}
                                                      onRowClick={route.navigateToResource}>
                    <TableCell sortable column={"registrationNumber"}>{labels.fields.registrationNumber}</TableCell>
                    <TableCell sortable column={"lastName"}>{labels.fields.lastName}</TableCell>
                    <TableCell sortable column={"firstName"}>{labels.fields.firstName}</TableCell>
                    <TableCell column={"email"} dataRender={renderEmailCell}>{labels.fields.email}</TableCell>
                    <TableCell column={"_actions"} dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
                </HalEmbeddedTable>
            </div>

            <PermissionsDialog
                isOpen={!!permissionsDialog}
                onClose={() => setPermissionsDialog(null)}
                memberName={memberName}
                memberRegistrationNumber={memberRegistrationNumber}
                {...permissionsEditor}
            />

            {actionModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setActionModal(null)}
                    title={actionModal.template.title ?? actionModal.templateName}
                    size="2xl"
                >
                    <HalFormDisplay
                        template={actionModal.template}
                        templateName={actionModal.templateName}
                        resourceData={actionModal.member as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setActionModal(null)}
                        onSubmitError={actionModal.templateName === 'suspendMember' ? (error) => {
                            const groups = parseSuspensionWarning409(error);
                            if (groups) {
                                setSuspensionWarning(groups);
                                return true;
                            }
                        } : undefined}
                    />
                </Modal>
            )}
            <SuspensionWarningDialog
                isOpen={suspensionWarning !== null}
                onClose={() => setSuspensionWarning(null)}
                affectedGroups={suspensionWarning ?? []}
            />
        </div>
    );
};
