import {type ReactElement, useMemo, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, Modal, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormsTemplate, HalResourceLinks, HalResponse} from '../../api';
import type {PendingInvitation} from './types.ts';
import {toHref} from '../../api/hateoas.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {labels} from '../../localization';
import {Ban, Crown, Pencil, Trash2, UserPlus} from 'lucide-react';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../../components/members/MemberNameWithRegNumber.tsx';
import {GroupMembersTable} from '../../components/groups/GroupMembersTable.tsx';
import {MemberRowWithRemove} from '../../components/groups/MemberRowWithRemove.tsx';

interface GroupOwner {
    memberId: string;
    _links: {
        member: HalResourceLinks;
    };
}

interface GroupMember extends HalResponse {
    memberId: string;
    joinedAt: string;
    _links: {
        self: HalResourceLinks;
        member: HalResourceLinks;
    };
}

interface GroupDetail extends HalResponse {
    id: string;
    name: string;
    owners?: GroupOwner[];
    members?: GroupMember[];
    pendingInvitations?: PendingInvitation[];
}

interface MemberActionModalState {
    member: GroupMember;
    templateName: string;
    template: HalFormsTemplate;
}

// Spring HATEOAS does not emit properties for DELETE affordances. The backend does
// accept a reason body — inject the property client-side so the form renders the field.
const withReasonProperty = (template: HalFormsTemplate): HalFormsTemplate => {
    const alreadyHasReason = template.properties.some(p => p.name === 'reason');
    if (alreadyHasReason) return template;
    return {
        ...template,
        properties: [
            ...template.properties,
            {name: 'reason', prompt: labels.fields.reason, type: 'textarea', required: false},
        ],
    };
};

const GroupDetailContent = ({resourceData}: {resourceData: GroupDetail}): ReactElement => {
    const {route} = useHalPageData<GroupDetail>();
    const navigate = useNavigate();
    const [isEditingName, setIsEditingName] = useState(false);
    const [addMemberModal, setAddMemberModal] = useState(false);
    const [inviteMemberModal, setInviteMemberModal] = useState(false);
    const [deleteModal, setDeleteModal] = useState(false);
    const [removeMemberModal, setRemoveMemberModal] = useState<MemberActionModalState | null>(null);
    const [addOwnerModal, setAddOwnerModal] = useState(false);
    const [removeOwnerModal, setRemoveOwnerModal] = useState<{template: HalFormsTemplate; ownerSelfHref: string} | null>(null);
    const [cancelInvitationModal, setCancelInvitationModal] = useState<{invitation: PendingInvitation; template: HalFormsTemplate} | null>(null);

    const editTemplate = resourceData._templates?.updateGroup ?? null;
    const deleteTemplate = resourceData._templates?.deleteGroup ?? null;
    const addMemberTemplate = resourceData._templates?.addGroupMember ?? null;
    const inviteMemberTemplate = resourceData._templates?.inviteMember ?? null;
    const addOwnerTemplate = resourceData._templates?.addGroupOwner ?? null;

    const handleRemoveMember = (member: GroupMember) => {
        const template = member._templates?.removeGroupMember;
        if (!template) return;
        setRemoveMemberModal({member, templateName: 'removeGroupMember', template});
    };

    const pendingInvitations = resourceData.pendingInvitations ?? [];

    const currentMemberIds = useMemo(
        () => (resourceData.members ?? []).map(m => m.memberId),
        [resourceData.members]
    );

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/groups" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>

                <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                    {editTemplate && !isEditingName && (
                        <Button
                            variant="secondary"
                            onClick={() => setIsEditingName(true)}
                            startIcon={<Pencil className="w-4 h-4"/>}
                        >
                            {labels.templates.updateGroup}
                        </Button>
                    )}
                    {deleteTemplate && (
                        <Button
                            variant="danger"
                            onClick={() => setDeleteModal(true)}
                            startIcon={<Trash2 className="w-4 h-4"/>}
                        >
                            {labels.templates.deleteGroup}
                        </Button>
                    )}
                </div>
            </div>

            {isEditingName && editTemplate && (
                <Card className="p-6">
                    <HalFormDisplay
                        template={editTemplate}
                        templateName="updateGroup"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setIsEditingName(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        submitButtonLabel={labels.buttons.saveChanges}
                    />
                </Card>
            )}

            <hr className="border-border"/>

            {(resourceData.owners && resourceData.owners.length > 0 || addOwnerTemplate) && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.groupOwners}
                        </h3>
                        {addOwnerTemplate && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => setAddOwnerModal(true)}
                                startIcon={<Crown className="w-4 h-4"/>}
                            >
                                {labels.templates.addOwner}
                            </Button>
                        )}
                    </div>
                    <dl>
                        {(resourceData.owners ?? []).map((owner) => {
                            const ownerWithTemplates = owner as typeof owner & {_templates?: Record<string, HalFormsTemplate>; _links: typeof owner._links & {self?: {href: string}}};
                            const removeOwnerTpl = ownerWithTemplates._templates?.removeGroupOwner;
                            const selfHref = ownerWithTemplates._links?.self?.href ?? '';
                            return (
                                <MemberRowWithRemove
                                    key={owner.memberId}
                                    memberId={owner.memberId}
                                    memberLink={owner._links.member}
                                    removeAriaLabel={labels.templates.removeOwner}
                                    onRemove={removeOwnerTpl ? () => setRemoveOwnerModal({template: removeOwnerTpl, ownerSelfHref: selfHref}) : undefined}
                                />
                            );
                        })}
                    </dl>
                </Card>
            )}

            {pendingInvitations.length > 0 && (
                <Card className="p-6">
                    <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">
                        {labels.sections.pendingInvitations}
                    </h3>
                    <ul className="flex flex-col gap-2">
                        {pendingInvitations.map((invitation) => {
                            const cancelTemplate = invitation._templates?.cancelInvitation;
                            return (
                                <li key={invitation.invitationId} className="flex items-center justify-between text-sm text-text-primary">
                                    <span>
                                        {invitation._links?.invitedMember ? (
                                            <HalRouteProvider routeLink={invitation._links.invitedMember}>
                                                <MemberNameWithRegNumber/>
                                            </HalRouteProvider>
                                        ) : invitation.invitationId}
                                    </span>
                                    {cancelTemplate && (
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            className="text-red-600"
                                            aria-label={labels.templates.cancelInvitation}
                                            onClick={() => setCancelInvitationModal({invitation, template: cancelTemplate})}
                                        >
                                            <Ban className="w-4 h-4"/>
                                            <span className="ml-1">{labels.templates.cancelInvitation}</span>
                                        </Button>
                                    )}
                                </li>
                            );
                        })}
                    </ul>
                </Card>
            )}

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.groupMembers}</h2>
                    <div className="flex gap-2">
                        {inviteMemberTemplate && (
                            <Button
                                variant="secondary"
                                onClick={() => setInviteMemberModal(true)}
                                startIcon={<UserPlus className="w-4 h-4"/>}
                            >
                                {labels.templates.inviteMember}
                            </Button>
                        )}
                        {addMemberTemplate && (
                            <Button
                                variant="primary"
                                onClick={() => setAddMemberModal(true)}
                                startIcon={<UserPlus className="w-4 h-4"/>}
                            >
                                {labels.templates.addGroupMember}
                            </Button>
                        )}
                    </div>
                </div>

                <GroupMembersTable
                    emptyMessage="Skupina nemá žádné členy."
                    members={(resourceData.members ?? []).map((member) => ({
                        memberId: member.memberId,
                        joinedAt: member.joinedAt,
                        memberLink: member._links.member,
                        removeAriaLabel: labels.templates.removeGroupMember,
                        onRemove: member._templates?.removeGroupMember ? () => handleRemoveMember(member) : undefined,
                    }))}
                />
            </div>

            {addMemberTemplate && addMemberModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setAddMemberModal(false)}
                    title={labels.templates.addGroupMember}
                    size="md"
                >
                    <HalFormDisplay
                        template={addMemberTemplate}
                        templateName="addGroupMember"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setAddMemberModal(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        excludeMemberIds={currentMemberIds}
                    />
                </Modal>
            )}

            {inviteMemberTemplate && inviteMemberModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setInviteMemberModal(false)}
                    title={labels.templates.inviteMember}
                    size="md"
                >
                    <HalFormDisplay
                        template={inviteMemberTemplate}
                        templateName="inviteMember"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setInviteMemberModal(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        excludeMemberIds={currentMemberIds}
                    />
                </Modal>
            )}

            {removeMemberModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setRemoveMemberModal(null)}
                    title={removeMemberModal.template.title ?? labels.templates.removeGroupMember}
                    size="md"
                >
                    <HalFormDisplay
                        template={removeMemberModal.template}
                        templateName={removeMemberModal.templateName}
                        resourceData={removeMemberModal.member as unknown as Record<string, unknown>}
                        pathname={extractNavigationPath(toHref(removeMemberModal.member._links.self))}
                        onClose={() => setRemoveMemberModal(null)}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {addOwnerTemplate && addOwnerModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setAddOwnerModal(false)}
                    title={labels.templates.addGroupOwner}
                    size="md"
                >
                    <HalFormDisplay
                        template={addOwnerTemplate}
                        templateName="addGroupOwner"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => { setAddOwnerModal(false); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                        includeOnlyMemberIds={currentMemberIds}
                    />
                </Modal>
            )}

            {removeOwnerModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setRemoveOwnerModal(null)}
                    title={labels.templates.removeGroupOwner}
                    size="md"
                >
                    <HalFormDisplay
                        template={removeOwnerModal.template}
                        templateName="removeGroupOwner"
                        resourceData={{}}
                        pathname={removeOwnerModal.ownerSelfHref ? '/groups/' + removeOwnerModal.ownerSelfHref.split('/groups/')[1] : route.pathname}
                        onClose={() => { setRemoveOwnerModal(null); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {deleteTemplate && deleteModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setDeleteModal(false)}
                    title={deleteTemplate.title ?? labels.templates.deleteGroup}
                    size="md"
                >
                    <HalFormDisplay
                        template={deleteTemplate}
                        templateName="deleteGroup"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setDeleteModal(false)}
                        onSubmitSuccess={() => navigate('/groups')}
                    />
                </Modal>
            )}

            {cancelInvitationModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setCancelInvitationModal(null)}
                    title={cancelInvitationModal.template.title ?? labels.templates.cancelInvitation}
                    size="md"
                >
                    <HalFormDisplay
                        template={withReasonProperty(cancelInvitationModal.template)}
                        templateName="cancelInvitation"
                        resourceData={cancelInvitationModal.invitation as unknown as Record<string, unknown>}
                        pathname={extractNavigationPath(toHref(cancelInvitationModal.invitation._links.self ?? {href: ''}))}
                        onClose={() => setCancelInvitationModal(null)}
                        onSubmitSuccess={() => {
                            setCancelInvitationModal(null);
                            void route.refetch();
                        }}
                        successMessage={labels.ui.savedSuccessfully}
                        navigateOnSuccess={false}
                    />
                </Modal>
            )}
        </div>
    );
};

export const GroupDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<GroupDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <GroupDetailContent resourceData={resourceData}/>;
};
