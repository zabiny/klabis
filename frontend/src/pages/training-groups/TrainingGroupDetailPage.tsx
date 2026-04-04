import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Modal, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormsTemplate, HalResourceLinks} from '../../api';
import {toHref} from '../../api/hateoas.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {formatDate} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';
import {Pencil, Trash2, UserMinus, UserPlus} from 'lucide-react';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../../components/members/MemberNameWithRegNumber.tsx';
import type {TrainingGroupDetail, TrainingGroupMember, TrainingGroupTrainer} from './types.ts';

interface MemberActionModalState {
    member: TrainingGroupMember & { _templates?: Record<string, HalFormsTemplate>; _links: { member?: HalResourceLinks; self?: HalResourceLinks } };
    templateName: string;
    template: HalFormsTemplate;
}

const TrainingGroupDetailContent = ({resourceData}: {resourceData: TrainingGroupDetail}): ReactElement => {
    const {route} = useHalPageData<TrainingGroupDetail>();
    const navigate = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const [addMemberModal, setAddMemberModal] = useState(false);
    const [deleteModal, setDeleteModal] = useState(false);
    const [removeMemberModal, setRemoveMemberModal] = useState<MemberActionModalState | null>(null);
    const [addTrainerModal, setAddTrainerModal] = useState(false);
    const [removeTrainerModal, setRemoveTrainerModal] = useState<{template: HalFormsTemplate; trainerSelfHref: string} | null>(null);

    const editTemplate = resourceData._templates?.updateTrainingGroup ?? null;
    const deleteTemplate = resourceData._templates?.deleteTrainingGroup ?? null;
    const addMemberTemplate = resourceData._templates?.addTrainingGroupMember ?? null;
    const addTrainerTemplate = resourceData._templates?.addTrainer ?? null;

    const trainers: TrainingGroupTrainer[] = (resourceData.trainers as TrainingGroupTrainer[] | undefined) ?? [];
    const members = (resourceData.members as TrainingGroupMember[] | undefined ?? []) as Array<TrainingGroupMember & { _templates?: Record<string, HalFormsTemplate> }>;

    const handleRemoveMember = (member: TrainingGroupMember & { _templates?: Record<string, HalFormsTemplate> }) => {
        const template = member._templates?.removeTrainingGroupMember;
        if (!template || !member._links.self) return;
        setRemoveMemberModal({
            member: member as MemberActionModalState['member'],
            templateName: 'removeTrainingGroupMember',
            template,
        });
    };

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/training-groups" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>
                    <p className="text-sm text-text-secondary mt-1">
                        {labels.fields.ageRange}: {resourceData.minAge}–{resourceData.maxAge}
                    </p>
                </div>

                <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                    {editTemplate && !isEditing && (
                        <Button
                            variant="secondary"
                            onClick={() => setIsEditing(true)}
                            startIcon={<Pencil className="w-4 h-4"/>}
                        >
                            {labels.templates.updateTrainingGroup}
                        </Button>
                    )}
                    {deleteTemplate && (
                        <Button
                            variant="danger"
                            onClick={() => setDeleteModal(true)}
                            startIcon={<Trash2 className="w-4 h-4"/>}
                        >
                            {labels.templates.deleteTrainingGroup}
                        </Button>
                    )}
                </div>
            </div>

            {isEditing && editTemplate && (
                <Card className="p-6">
                    <HalFormDisplay
                        template={editTemplate as HalFormsTemplate}
                        templateName="updateTrainingGroup"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setIsEditing(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        submitButtonLabel={labels.buttons.saveChanges}
                    />
                </Card>
            )}

            <hr className="border-border"/>

            {(trainers.length > 0 || addTrainerTemplate) && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.trainingGroupTrainers}
                        </h3>
                        {addTrainerTemplate && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => setAddTrainerModal(true)}
                                startIcon={<UserPlus className="w-4 h-4"/>}
                            >
                                {labels.templates.addTrainer}
                            </Button>
                        )}
                    </div>
                    <dl>
                        {trainers.map((trainer) => {
                            const removeTrainerTpl = trainer._templates?.removeTrainer;
                            const selfHref = trainer._links?.self ? toHref(trainer._links.self) : '';
                            return trainer._links.member ? (
                                <DetailRow key={trainer.memberId} label="">
                                    <div className="flex items-center justify-between w-full">
                                        <HalRouteProvider routeLink={trainer._links.member as HalResourceLinks}>
                                            <MemberNameWithRegNumber/>
                                        </HalRouteProvider>
                                        {removeTrainerTpl && selfHref && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-red-600"
                                                aria-label={labels.templates.removeTrainer}
                                                onClick={() => setRemoveTrainerModal({template: removeTrainerTpl, trainerSelfHref: selfHref})}
                                            >
                                                <UserMinus className="w-4 h-4"/>
                                            </Button>
                                        )}
                                    </div>
                                </DetailRow>
                            ) : null;
                        })}
                    </dl>
                </Card>
            )}

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.trainingGroupMembers}</h2>
                    {addMemberTemplate && (
                        <Button
                            variant="primary"
                            onClick={() => setAddMemberModal(true)}
                            startIcon={<UserPlus className="w-4 h-4"/>}
                        >
                            {labels.templates.addTrainingGroupMember}
                        </Button>
                    )}
                </div>

                {members.length === 0 ? (
                    <p className="text-sm text-text-tertiary">Skupina nemá žádné členy.</p>
                ) : (
                    <Card className="p-0 overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                            <tr className="border-b border-border bg-slate-50 dark:bg-zinc-800">
                                <th className="text-left px-4 py-3 font-medium text-text-secondary">
                                    {labels.fields.memberId}
                                </th>
                                <th className="text-left px-4 py-3 font-medium text-text-secondary">
                                    {labels.tables.joinedAt}
                                </th>
                                <th className="px-4 py-3"/>
                            </tr>
                            </thead>
                            <tbody>
                            {members.map((member) => (
                                <tr key={member.memberId}
                                    className="border-b border-border last:border-0 hover:bg-slate-50 dark:hover:bg-zinc-800/50">
                                    <td className="px-4 py-3">
                                        {member._links.member && (
                                            <HalRouteProvider routeLink={member._links.member as HalResourceLinks}>
                                                <MemberNameWithRegNumber/>
                                            </HalRouteProvider>
                                        )}
                                    </td>
                                    <td className="px-4 py-3 text-text-secondary">{formatDate(member.joinedAt)}</td>
                                    <td className="px-4 py-3 text-right">
                                        {member._templates?.removeTrainingGroupMember && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-red-600"
                                                aria-label={labels.templates.removeTrainingGroupMember}
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
                    title={labels.templates.addTrainingGroupMember}
                    size="md"
                >
                    <HalFormDisplay
                        template={addMemberTemplate as HalFormsTemplate}
                        templateName="addTrainingGroupMember"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => { setAddMemberModal(false); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {removeMemberModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setRemoveMemberModal(null)}
                    title={removeMemberModal.template.title ?? labels.templates.removeTrainingGroupMember}
                    size="md"
                >
                    <HalFormDisplay
                        template={removeMemberModal.template}
                        templateName={removeMemberModal.templateName}
                        resourceData={removeMemberModal.member as unknown as Record<string, unknown>}
                        pathname={extractNavigationPath(toHref(removeMemberModal.member._links.self!))}
                        onClose={() => { setRemoveMemberModal(null); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {addTrainerTemplate && addTrainerModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setAddTrainerModal(false)}
                    title={labels.templates.addTrainer}
                    size="md"
                >
                    <HalFormDisplay
                        template={addTrainerTemplate as HalFormsTemplate}
                        templateName="addTrainer"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => { setAddTrainerModal(false); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {removeTrainerModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setRemoveTrainerModal(null)}
                    title={labels.templates.removeTrainer}
                    size="md"
                >
                    <HalFormDisplay
                        template={removeTrainerModal.template}
                        templateName="removeTrainer"
                        resourceData={{}}
                        pathname={removeTrainerModal.trainerSelfHref ? extractNavigationPath(removeTrainerModal.trainerSelfHref) : route.pathname}
                        onClose={() => { setRemoveTrainerModal(null); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {deleteTemplate && deleteModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setDeleteModal(false)}
                    title={(deleteTemplate as HalFormsTemplate).title ?? labels.templates.deleteTrainingGroup}
                    size="md"
                >
                    <HalFormDisplay
                        template={deleteTemplate as HalFormsTemplate}
                        templateName="deleteTrainingGroup"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setDeleteModal(false)}
                        onSubmitSuccess={() => navigate('/training-groups')}
                    />
                </Modal>
            )}
        </div>
    );
};

export const TrainingGroupDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<TrainingGroupDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <TrainingGroupDetailContent resourceData={resourceData}/>;
};
