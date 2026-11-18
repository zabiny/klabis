import {type ReactElement, useMemo, useState} from "react";
import type {HalResponse, Link, ListMembersItemResource} from "../../api";
import {FinanceTransactionDialog} from "../../components/finance/FinanceTransactionDialog.tsx";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {PermissionsDialog} from "../../components/members/PermissionsDialog.tsx";
import {useTemplateDialog} from "../../components/HalNavigator2/useTemplateDialog.tsx";
import {halResource, type HalLinkAction} from "../../api/halResource";
import {Badge, Button} from "../../components/UI";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {SyncMembersFromOrisModal} from "../../components/members/SyncMembersFromOrisModal.tsx";
import {DisplayContactText} from "./DisplayContactText.tsx";
import {Banknote, Pencil, RefreshCw, Shield, UserCheck, UserX} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {labels} from "../../localization";
import {SyncStatusIndicator} from "../../components/sync/SyncStatusIndicator.tsx";
import {SuspensionWarningDialog} from "./SuspensionWarningDialog.tsx";
import {useSuspendMemberAction} from "./useSuspendMemberAction.ts";
import {
    DEFAULT_MEMBER_STATUS,
    MembersFilterBar,
    type MembersFilterValue
} from "../../components/members/MembersFilterBar.tsx";
import {useUrlFilters} from "../../hooks/useUrlFilters.ts";
import {toQuery} from "./membersQuery.ts";
import {useTakeOverRegistration} from "./useTakeOverRegistration.ts";
import {renderRegistrationForm} from "./RegistrationForm.tsx";
import {usePermissionsEditor} from "../../hooks/usePermissionsEditor.ts";
import {HalPageGate} from "../HalPageGate.tsx";

type MemberSummaryData = ListMembersItemResource;

interface MemberPermissionsDialogState {
    member: MemberSummaryData;
    permissionsUrl: string;
}

const MembersPageContent = ({resourceData}: {resourceData: HalResponse}): ReactElement => {
    const {navigateToResource, refetch} = useHalPageData();
    const [permissionsDialog, setPermissionsDialog] = useState<MemberPermissionsDialogState | null>(null);
    const [transactionDialogAccount, setTransactionDialogAccount] = useState<Link | null>(null);
    const [isSyncModalOpen, setIsSyncModalOpen] = useState(false);

    const importFromOrisTemplate = resourceData._templates?.importFromOris;
    const importFromOrisTarget = importFromOrisTemplate?.target;
    const hasImportFromOris = !!importFromOrisTemplate;

    const suspension = useSuspendMemberAction();
    const [suspendedMemberName, setSuspendedMemberName] = useState('');

    const actionDialog = useTemplateDialog({
        size: '2xl',
        title: (template, name) => template.title ?? name,
        onSubmitError: suspension.onSubmitError,
    });

    const hasManageAuthority = resourceData._templates?.registerMember !== undefined;

    const {values, setValues} = useUrlFilters({
        status: {type: 'string', default: DEFAULT_MEMBER_STATUS},
        q: {type: 'string'},
        incomplete: {type: 'boolean'},
    });
    const filterValue = values as MembersFilterValue;
    const extraParams = useMemo(() => toQuery(filterValue), [filterValue]);

    const openPermissionsDialog = (member: MemberSummaryData, permissionsAction: HalLinkAction) => {
        setPermissionsDialog({member, permissionsUrl: permissionsAction.href});
    };

    useTakeOverRegistration(resourceData);

    const renderActionsCell = ({item: member}: TableCellRenderProps<MemberSummaryData>) => {
        const actions = halResource(member);
        const accountAction = actions.link('account');
        const editAction = actions.template('updateMember');
        const permissionsAction = actions.link('permissions');
        const suspendAction = actions.template('suspendMember');
        const resumeAction = actions.template('resumeMember');

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {accountAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.finance.openTransactionDialogAriaLabel}
                        className="text-primary"
                        onClick={(e) => {
                            e.stopPropagation();
                            setTransactionDialogAccount(accountAction.link);
                        }}
                    >
                        <Banknote className="w-4 h-4"/>
                    </Button>
                )}
                {editAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.buttons.edit}
                        className="text-primary"
                        onClick={(e) => {
                            e.stopPropagation();
                            navigateToResource(member, {state: {editing: true}});
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
                {permissionsAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.permissions['MEMBERS:PERMISSIONS'].label}
                        className="text-gray-500"
                        onClick={(e) => {
                            e.stopPropagation();
                            openPermissionsDialog(member, permissionsAction);
                        }}
                    >
                        <Shield className="w-4 h-4"/>
                    </Button>
                )}
                {suspendAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.suspendMember}
                        className="text-red-600"
                        onClick={(e) => {
                            e.stopPropagation();
                            setSuspendedMemberName(`${member.firstName ?? ''} ${member.lastName ?? ''}`.trim());
                            actionDialog.open(suspendAction, {
                                note: <p className="text-text-secondary">{labels.suspensionWarning.suspendFormGuardianNote}</p>,
                            });
                        }}
                    >
                        <UserX className="w-4 h-4"/>
                    </Button>
                )}
                {resumeAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.resumeMember}
                        className="text-green-600"
                        onClick={(e) => {
                            e.stopPropagation();
                            actionDialog.open(resumeAction);
                        }}
                    >
                        <UserCheck className="w-4 h-4"/>
                    </Button>
                )}
                <SyncStatusIndicator syncLink={member._links?.sync} mode="icon"/>
            </div>
        );
    };

    const renderContactCell = ({item}: TableCellRenderProps<MemberSummaryData>) => {
        const contact = item.displayContact;
        if (!contact) return null;
        return (
            <div className="flex flex-col">
                <div><DisplayContactText contact={contact.email}/></div>
                <div><DisplayContactText contact={contact.phone}/></div>
            </div>
        );
    };

    const renderDataIncompleteCell = ({item: member}: TableCellRenderProps<MemberSummaryData>) => {
        if (!member.dataIncomplete) return null;
        return <Badge variant="orange" size="sm">{labels.membersFilter.incompleteBadge}</Badge>;
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
                        {hasImportFromOris && (
                            <Button
                                variant="ghost"
                                startIcon={<RefreshCw className="w-4 h-4"/>}
                                data-testid="form-template-button-importFromOris"
                                onClick={() => setIsSyncModalOpen(true)}
                            >
                                {labels.templates.importFromOris}
                            </Button>
                        )}
                        <HalFormButton name="registerMember" modal={false}>
                            {renderRegistrationForm()}
                        </HalFormButton>
                    </div>
                </div>
                <MembersFilterBar
                    value={filterValue}
                    onChange={setValues}
                    hasManageAuthority={hasManageAuthority}
                />
                <HalEmbeddedTable<MemberSummaryData> tableId="members"
                                                      defaultOrderBy={"lastName"}
                                                      hideEmptyColumns
                                                      extraParams={extraParams}
                                                      emptyMessage={labels.membersFilter.emptyState}
                                                      onRowClick={navigateToResource}>
                    <TableCell sortable column={"registrationNumber"}>{labels.fields.registrationNumber}</TableCell>
                    <TableCell sortable column={"lastName"}>{labels.fields.lastName}</TableCell>
                    <TableCell sortable column={"firstName"}>{labels.fields.firstName}</TableCell>
                    <TableCell column={"displayContact"} dataRender={renderContactCell}>{labels.fields.contact}</TableCell>
                    {hasManageAuthority && (
                        <TableCell column={"dataIncomplete"} dataRender={renderDataIncompleteCell}>{labels.membersFilter.incompleteBadge}</TableCell>
                    )}
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

            {actionDialog.dialog}
            <SuspensionWarningDialog
                warning={suspension.warning}
                resolvedGroupIds={suspension.resolvedGroupIds}
                cascadeGroupIds={suspension.cascadeGroupIds}
                suspendedMemberName={suspendedMemberName}
                isFinishing={suspension.isFinishing}
                onClose={suspension.clearWarning}
                onGroupResolved={suspension.markGroupResolved}
                onCascadeChosen={suspension.chooseCascade}
                onCascadeCancelled={suspension.cancelCascade}
                onFinish={suspension.finishSuspension}
            />
            <FinanceTransactionDialog
                accountLink={transactionDialogAccount ?? {href: ''}}
                isOpen={transactionDialogAccount !== null}
                onClose={() => setTransactionDialogAccount(null)}
            />
            <SyncMembersFromOrisModal
                isOpen={isSyncModalOpen}
                onClose={() => setIsSyncModalOpen(false)}
                targetUrl={importFromOrisTarget}
                onSyncComplete={() => refetch()}
            />
        </div>
    );
};

export const MembersPage = (): ReactElement => (
    <HalPageGate>
        {(resourceData) => <MembersPageContent resourceData={resourceData}/>}
    </HalPageGate>
);
