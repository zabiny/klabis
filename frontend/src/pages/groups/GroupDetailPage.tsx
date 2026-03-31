import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Modal, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {toHref} from '../../api/hateoas.ts';
import {formatDate} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';
import {Pencil, Trash2, UserMinus, UserPlus} from 'lucide-react';

interface GroupOwner {
    id: string;
    firstName: string;
    lastName: string;
    registrationNumber?: string;
}

interface GroupMember extends HalResponse {
    memberId: string;
    firstName: string;
    lastName: string;
    registrationNumber?: string;
    joinedAt: string;
}

interface GroupDetail extends HalResponse {
    id: string;
    name: string;
    owners?: GroupOwner[];
    members?: GroupMember[];
}

interface MemberActionModalState {
    member: GroupMember;
    templateName: string;
    template: HalFormsTemplate;
}

const GroupDetailContent = ({resourceData}: {resourceData: GroupDetail}): ReactElement => {
    const {route} = useHalPageData<GroupDetail>();
    const navigate = useNavigate();
    const [isEditingName, setIsEditingName] = useState(false);
    const [addMemberModal, setAddMemberModal] = useState(false);
    const [deleteModal, setDeleteModal] = useState(false);
    const [removeMemberModal, setRemoveMemberModal] = useState<MemberActionModalState | null>(null);

    const editTemplate = resourceData._templates?.updateGroup ?? null;
    const deleteTemplate = resourceData._templates?.deleteGroup ?? null;
    const addMemberTemplate = resourceData._templates?.addGroupMember ?? null;

    const handleRemoveMember = (member: GroupMember) => {
        const template = member._templates?.removeGroupMember;
        if (!template) return;
        setRemoveMemberModal({member, templateName: 'removeGroupMember', template});
    };

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

            {resourceData.owners && resourceData.owners.length > 0 && (
                <Card className="p-6">
                    <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">
                        {labels.sections.groupOwners}
                    </h3>
                    <dl>
                        {resourceData.owners.map((owner) => (
                            <DetailRow key={owner.id} label={owner.registrationNumber ?? owner.id}>
                                {owner.firstName} {owner.lastName}
                            </DetailRow>
                        ))}
                    </dl>
                </Card>
            )}

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.groupMembers}</h2>
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

                {(!resourceData.members || resourceData.members.length === 0) ? (
                    <p className="text-sm text-text-tertiary">Skupina nemá žádné členy.</p>
                ) : (
                    <Card className="p-0 overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                            <tr className="border-b border-border bg-slate-50 dark:bg-zinc-800">
                                <th className="text-left px-4 py-3 font-medium text-text-secondary">
                                    {labels.fields.registrationNumber}
                                </th>
                                <th className="text-left px-4 py-3 font-medium text-text-secondary">
                                    {labels.fields.lastName}
                                </th>
                                <th className="text-left px-4 py-3 font-medium text-text-secondary">
                                    {labels.fields.firstName}
                                </th>
                                <th className="text-left px-4 py-3 font-medium text-text-secondary">
                                    {labels.tables.joinedAt}
                                </th>
                                <th className="px-4 py-3"/>
                            </tr>
                            </thead>
                            <tbody>
                            {resourceData.members.map((member) => (
                                <tr key={member.memberId} className="border-b border-border last:border-0 hover:bg-slate-50 dark:hover:bg-zinc-800/50">
                                    <td className="px-4 py-3 text-text-secondary">{member.registrationNumber ?? '—'}</td>
                                    <td className="px-4 py-3">{member.lastName}</td>
                                    <td className="px-4 py-3">{member.firstName}</td>
                                    <td className="px-4 py-3 text-text-secondary">{formatDate(member.joinedAt)}</td>
                                    <td className="px-4 py-3 text-right">
                                        {member._templates?.removeGroupMember && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-red-600"
                                                aria-label={labels.templates.removeGroupMember}
                                                onClick={() => handleRemoveMember(member)}
                                            >
                                                <UserMinus className="w-4 h-4"/>
                                            </Button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </Card>
                )}
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
                        pathname={toHref(removeMemberModal.member._links!.self).replace(/^https?:\/\/[^/]+/, '').replace(/^\/api/, '')}
                        onClose={() => setRemoveMemberModal(null)}
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
