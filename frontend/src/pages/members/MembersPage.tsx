import {type ReactElement, useState} from "react";
import {Link} from "react-router-dom";
import type {EntityModel, HalFormsTemplate, HalResourceLinks} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {PermissionsDialog} from "../../components/members/PermissionsDialog.tsx";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {ModalOverlay} from "../../components/UI";
import {Pencil, Shield, UserCheck, UserX} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";

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

    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const member = item as unknown as MemberSummaryData;
        const hasEditTemplate = !!member._templates?.default;
        const hasPermissionsLink = !!member._links?.permissions;
        const hasSuspendTemplate = !!member._templates?.suspendMember;
        const hasResumeTemplate = !!member._templates?.resumeMember;

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {hasEditTemplate && (
                    <button
                        type="button"
                        aria-label="Upravit"
                        className="p-1 rounded hover:bg-surface-base cursor-pointer text-primary"
                        onClick={(e) => {
                            e.stopPropagation();
                            route.navigateToResource(member);
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </button>
                )}
                {hasPermissionsLink && (
                    <button
                        type="button"
                        aria-label="Oprávnění"
                        className="p-1 rounded hover:bg-surface-base cursor-pointer text-gray-500"
                        onClick={(e) => {
                            e.stopPropagation();
                            openPermissionsDialog(member);
                        }}
                    >
                        <Shield className="w-4 h-4"/>
                    </button>
                )}
                {hasSuspendTemplate && (
                    <button
                        type="button"
                        aria-label="Ukončit členství"
                        className="p-1 rounded hover:bg-surface-base cursor-pointer text-red-600"
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(member, 'suspendMember');
                        }}
                    >
                        <UserX className="w-4 h-4"/>
                    </button>
                )}
                {hasResumeTemplate && (
                    <button
                        type="button"
                        aria-label="Reaktivovat"
                        className="p-1 rounded hover:bg-surface-base cursor-pointer text-green-600"
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(member, 'resumeMember');
                        }}
                    >
                        <UserCheck className="w-4 h-4"/>
                    </button>
                )}
            </div>
        );
    };

    const renderStatusCell = ({item}: TableCellRenderProps) => {
        const active = item.active as boolean | null;
        if (active === null || active === undefined) return null;
        return active ? (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">
                Aktivní
            </span>
        ) : (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600">
                Neaktivní
            </span>
        );
    };

    const renderEmailCell = ({item}: TableCellRenderProps) => {
        const email = item.email as string | null;
        if (!email) return null;
        return <span>{email}</span>;
    };

    const memberName = permissionsDialog
        ? `${permissionsDialog.member.firstName ?? ''} ${permissionsDialog.member.lastName ?? ''}`.trim()
        : '';

    const memberRegistrationNumber = permissionsDialog
        ? permissionsDialog.member.registrationNumber ?? undefined
        : undefined;

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">Členové</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">Seznam členů</h2>
                    {resourceData?._templates?.registerMember && (
                        <Link
                            to="/members/new"
                            className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light"
                        >
                            Registrovat člena
                        </Link>
                    )}
                </div>
                <HalEmbeddedTable<MemberSummaryData> collectionName={"memberSummaryResponseList"}
                                                      defaultOrderBy={"lastName"}
                                                      onRowClick={route.navigateToResource}>
                    <TableCell sortable column={"registrationNumber"}>Reg. číslo</TableCell>
                    <TableCell sortable column={"lastName"}>Příjmení</TableCell>
                    <TableCell sortable column={"firstName"}>Jméno</TableCell>
                    <TableCell column={"email"} dataRender={renderEmailCell}>E-mail</TableCell>
                    <TableCell column={"active"} dataRender={renderStatusCell}>Stav</TableCell>
                    <TableCell column={"_actions"} dataRender={renderActionsCell}>Akce</TableCell>
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
                <ModalOverlay
                    isOpen={true}
                    onClose={() => setActionModal(null)}
                    title={actionModal.template.title ?? actionModal.templateName}
                >
                    <HalFormDisplay
                        template={actionModal.template}
                        templateName={actionModal.templateName}
                        resourceData={actionModal.member as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setActionModal(null)}
                    />
                </ModalOverlay>
            )}
        </div>
    );
};
