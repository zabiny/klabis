import {type ReactElement, useState} from "react";
import {Link} from "react-router-dom";
import type {EntityModel, HalFormsTemplate, HalResourceLinks} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {PermissionsDialog} from "../../components/members/PermissionsDialog.tsx";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {Badge, Button, Modal} from "../../components/UI";
import {Pencil, Shield, UserCheck, UserX} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {labels} from "../../localization";
import {SuspensionWarningDialog, type AffectedGroup} from "./SuspensionWarningDialog.tsx";
import {FetchError} from "../../api/authorizedFetch.ts";

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
    const [actionModal, setActionModal] = useState<MemberActionModalState | null>(null);
    const [permissionsDialog, setPermissionsDialog] = useState<MemberPermissionsDialogState | null>(null);
    const [suspensionWarning, setSuspensionWarning] = useState<AffectedGroup[] | null>(null);

    const openActionModal = (member: MemberSummaryData, templateName: string) => {
        const template = member._templates?.[templateName];
        if (!template) return;
        setActionModal({member, templateName, template});
    };

    const parseSuspensionWarning409 = (error: unknown): AffectedGroup[] | null => {
        if (!(error instanceof FetchError) || error.responseStatus !== 409) return null;
        try {
            const body = JSON.parse(error.responseBody ?? '{}');
            if (Array.isArray(body.affectedGroups)) return body.affectedGroups as AffectedGroup[];
        } catch {
            // not a structured 409
        }
        return null;
    };

    const openPermissionsDialog = (member: MemberSummaryData) => {
        const permissionsLink = member._links?.permissions;
        if (!permissionsLink) return;
        const link = Array.isArray(permissionsLink) ? permissionsLink[0] : permissionsLink;
        if (!link?.href) return;
        setPermissionsDialog({member, permissionsUrl: link.href});
    };

    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const member = item as unknown as MemberSummaryData;
        const hasEditTemplate = !!member._templates?.updateMember;
        const hasPermissionsLink = !!member._links?.permissions;
        const hasSuspendTemplate = !!member._templates?.suspendMember;
        const hasResumeTemplate = !!member._templates?.resumeMember;

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
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

    const renderStatusCell = ({item}: TableCellRenderProps) => {
        const active = item.active as boolean | null;
        if (active === null || active === undefined) return null;
        return active ? (
            <Badge variant="success" size="sm">{labels.enums.memberStatus.active}</Badge>
        ) : (
            <Badge variant="default" size="sm">{labels.enums.memberStatus.inactive}</Badge>
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

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.members}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.membersList}</h2>
                    <div className="flex gap-2">
                        {resourceData?._templates?.registerMember && (
                            <Link to="/members/new">
                                <Button variant="primary">
                                    {labels.templates.registerMember}
                                </Button>
                            </Link>
                        )}
                    </div>
                </div>
                <HalEmbeddedTable<MemberSummaryData> collectionName={"memberSummaryResponseList"}
                                                      defaultOrderBy={"lastName"}
                                                      hideEmptyColumns
                                                      onRowClick={route.navigateToResource}>
                    <TableCell sortable column={"registrationNumber"}>{labels.fields.registrationNumber}</TableCell>
                    <TableCell sortable column={"lastName"}>{labels.fields.lastName}</TableCell>
                    <TableCell sortable column={"firstName"}>{labels.fields.firstName}</TableCell>
                    <TableCell column={"email"} dataRender={renderEmailCell}>{labels.fields.email}</TableCell>
                    <TableCell column={"active"} dataRender={renderStatusCell}>{labels.tables.status}</TableCell>
                    <TableCell column={"_actions"} dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
                </HalEmbeddedTable>
            </div>

            <PermissionsDialog
                isOpen={!!permissionsDialog}
                onClose={() => setPermissionsDialog(null)}
                permissionsUrl={permissionsDialog?.permissionsUrl ?? ''}
                memberName={memberName}
                memberRegistrationNumber={memberRegistrationNumber}
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
