import {type ReactElement, useMemo, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {useTemplateDialog} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {createMemberFilteredFactory} from '../../components/KlabisFieldsFactory.tsx';
import type {GetGroupResource, HalFormsTemplate, HalResourceLinks, HalResponse} from '../../api';
import type {PendingInvitation} from './types.ts';
import {labels} from '../../localization';
import {Ban, Crown, LogOut, Pencil, Trash2, UserPlus} from 'lucide-react';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {useAuth} from '../../contexts/authContext.ts';
import {MemberNameWithRegNumber} from '../../components/members/MemberNameWithRegNumber.tsx';
import {GroupMembersTable} from '../../components/groups/GroupMembersTable.tsx';
import {DelegatedAuthoritiesNotice, DelegatedAuthoritiesReadOnly} from '../../components/groups/DelegatedAuthorities.tsx';
import {MemberRowWithRemove} from '../../components/groups/MemberRowWithRemove.tsx';

interface GroupOwner extends HalResponse {
    memberId: string;
    _links: {
        member: HalResourceLinks;
        self?: HalResourceLinks;
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

// GetGroupResource types the payload (id, name, _templates) from the spec. Its
// owners/members/pendingInvitations arrays carry per-row _links / _templates added at runtime
// by the backend (member link, removeGroupOwner, removeGroupMember, cancelInvitation) that the
// generated schema does not describe, so those rows are read through the view types above and
// in ./types.ts.
type GroupDetail = Omit<GetGroupResource, 'owners' | 'members' | 'pendingInvitations'> & {
    owners?: GroupOwner[];
    members?: GroupMember[];
    pendingInvitations?: PendingInvitation[];
};

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
    const {pathname} = useHalPageData<GroupDetail>();
    const navigate = useNavigate();
    const [isEditingName, setIsEditingName] = useState(false);
    const currentMemberId = useAuth().getUser()?.memberId ?? null;

    const editTemplate = resourceData._templates?.updateGroup ?? null;

    const pendingInvitations = resourceData.pendingInvitations ?? [];

    const currentMemberIds = useMemo(
        () => (resourceData.members ?? []).map(m => m.memberId),
        [resourceData.members]
    );
    const addMemberFactory = useMemo(() => createMemberFilteredFactory(currentMemberIds), [currentMemberIds]);
    const addOwnerFactory = useMemo(() => createMemberFilteredFactory(undefined, currentMemberIds), [currentMemberIds]);

    const group = resourceData;

    const addMemberDialog = useTemplateDialog('addGroupMember', {
        resource: group,
        title: labels.templates.addGroupMember,
        successMessage: labels.ui.savedSuccessfully,
        fieldsFactory: addMemberFactory,
    });
    const inviteMemberDialog = useTemplateDialog('inviteMember', {
        resource: group,
        title: labels.templates.inviteMember,
        successMessage: labels.ui.savedSuccessfully,
        fieldsFactory: addMemberFactory,
    });
    const addOwnerDialog = useTemplateDialog('addGroupOwner', {
        resource: group,
        title: labels.templates.addGroupOwner,
        successMessage: labels.ui.savedSuccessfully,
        fieldsFactory: addOwnerFactory,
    });
    const removeMemberDialog = useTemplateDialog('removeGroupMember', {
        title: (template) => template.title ?? labels.templates.removeGroupMember,
        successMessage: labels.ui.savedSuccessfully,
    });
    const removeOwnerDialog = useTemplateDialog('removeGroupOwner', {
        title: labels.templates.removeGroupOwner,
        successMessage: labels.ui.savedSuccessfully,
    });
    const deleteDialog = useTemplateDialog('deleteGroup', {
        resource: group,
        title: (template) => template.title ?? labels.templates.deleteGroup,
        onSubmitSuccess: () => navigate('/groups'),
    });
    const cancelInvitationDialog = useTemplateDialog('cancelInvitation', {
        title: (template) => template.title ?? labels.templates.cancelInvitation,
        successMessage: labels.ui.savedSuccessfully,
        navigateOnSuccess: false,
    });

    const openRemoveOwner = (owner: GroupOwner) => {
        const isSelf = owner.memberId === currentMemberId;
        removeOwnerDialog.open(owner, undefined, {
            note: <span className="text-sm text-text-secondary">
                {isSelf ? labels.templates.removeSelfAsOwnerLeavesGroup : labels.templates.removeGroupOwnerLeavesGroup}
            </span>,
            onSubmitSuccess: isSelf ? () => navigate('/groups') : undefined,
        });
    };

    // The backend sends the caller's own removeGroupMember on the group when the caller is a member but not
    // an owner. It means "leave this group", so it is rendered under its own label.
    const openLeaveGroup = () => removeMemberDialog.open(group, undefined, {
        title: labels.templates.leaveGroup,
        note: <span className="text-sm text-text-secondary">{labels.templates.leaveGroupConfirm}</span>,
        successMessage: undefined,
        navigateOnSuccess: false,
        onSubmitSuccess: () => navigate('/groups'),
    });

    const openCancelInvitation = (invitation: PendingInvitation) => {
        const template = invitation._templates?.cancelInvitation;
        if (!template) return;
        cancelInvitationDialog.open({
            ...invitation,
            _templates: {...invitation._templates, cancelInvitation: withReasonProperty(template)},
        });
    };

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/groups" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex flex-col gap-2">
                    <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>
                    <DelegatedAuthoritiesNotice authorities={resourceData.delegatedAuthorities} audience="owners"/>
                </div>

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
                    {deleteDialog.available && (
                        <Button
                            variant="danger"
                            onClick={() => deleteDialog.open()}
                            startIcon={<Trash2 className="w-4 h-4"/>}
                        >
                            {labels.templates.deleteGroup}
                        </Button>
                    )}
                    {removeMemberDialog.availableOn(group) && (
                        <Button
                            variant="secondary"
                            onClick={openLeaveGroup}
                            startIcon={<LogOut className="w-4 h-4"/>}
                        >
                            {labels.templates.leaveGroup}
                        </Button>
                    )}
                </div>
            </div>

            {isEditingName && editTemplate && (
                <Card className="p-6">
                    <DelegatedAuthoritiesReadOnly authorities={resourceData.delegatedAuthorities}/>
                    <HalFormDisplay
                        template={editTemplate}
                        templateName="updateGroup"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={pathname}
                        onClose={() => setIsEditingName(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        submitButtonLabel={labels.buttons.saveChanges}
                    />
                </Card>
            )}

            <hr className="border-border"/>

            {(resourceData.owners && resourceData.owners.length > 0 || addOwnerDialog.available) && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.groupOwners}
                        </h3>
                        {addOwnerDialog.available && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => addOwnerDialog.open()}
                                startIcon={<Crown className="w-4 h-4"/>}
                            >
                                {labels.templates.addOwner}
                            </Button>
                        )}
                    </div>
                    <dl>
                        {(resourceData.owners ?? []).map((owner) => (
                            <MemberRowWithRemove
                                key={owner.memberId}
                                memberId={owner.memberId}
                                memberLink={owner._links.member}
                                removeAriaLabel={labels.templates.removeOwner}
                                onRemove={removeOwnerDialog.availableOn(owner) ? () => openRemoveOwner(owner) : undefined}
                            />
                        ))}
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
                            return (
                                <li key={invitation.invitationId} className="flex items-center justify-between text-sm text-text-primary">
                                    <span>
                                        {invitation._links?.invitedMember ? (
                                            <HalRouteProvider routeLink={invitation._links.invitedMember}>
                                                <MemberNameWithRegNumber/>
                                            </HalRouteProvider>
                                        ) : invitation.invitationId}
                                    </span>
                                    {cancelInvitationDialog.availableOn(invitation) && (
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            className="text-red-600"
                                            aria-label={labels.templates.cancelInvitation}
                                            onClick={() => openCancelInvitation(invitation)}
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
                        {inviteMemberDialog.available && (
                            <Button
                                variant="secondary"
                                onClick={() => inviteMemberDialog.open()}
                                startIcon={<UserPlus className="w-4 h-4"/>}
                            >
                                {labels.templates.inviteMember}
                            </Button>
                        )}
                        {addMemberDialog.available && (
                            <Button
                                variant="primary"
                                onClick={() => addMemberDialog.open()}
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
                        onRemove: removeMemberDialog.availableOn(member) ? () => removeMemberDialog.open(member) : undefined,
                    }))}
                />
            </div>

            {addMemberDialog.dialog}
            {inviteMemberDialog.dialog}
            {addOwnerDialog.dialog}
            {removeMemberDialog.dialog}
            {removeOwnerDialog.dialog}
            {deleteDialog.dialog}
            {cancelInvitationDialog.dialog}
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
