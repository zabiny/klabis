import {type ReactElement, useMemo, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {useTemplateDialog} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {createMemberFilteredFactory} from '../../components/KlabisFieldsFactory.tsx';
import type {
    GetTrainingGroupMembersItemResource,
    GetTrainingGroupResource,
    GetTrainingGroupTrainersItemResource,
    HalFormsTemplate,
} from '../../api';
import {describeError, labels} from '../../localization';
import {halResource} from '../../api/halResource';
import {Pencil, Trash2, UserPlus} from 'lucide-react';
import {GroupMembersTable} from '../../components/groups/GroupMembersTable.tsx';
import {MemberRowWithRemove} from '../../components/groups/MemberRowWithRemove.tsx';

const TrainingGroupDetailContent = ({resourceData}: {resourceData: GetTrainingGroupResource}): ReactElement => {
    const navigate = useNavigate();
    const [isEditing, setIsEditing] = useState(false);

    const editTemplate = resourceData._templates?.updateTrainingGroup ?? null;

    const trainers: GetTrainingGroupTrainersItemResource[] = resourceData.trainers ?? [];
    const members: GetTrainingGroupMembersItemResource[] = resourceData.members ?? [];

    const trainerIds = useMemo(() => trainers.flatMap(t => t.memberId ? [t.memberId] : []), [trainers]);
    const memberIds = useMemo(() => members.flatMap(m => m.memberId ? [m.memberId] : []), [members]);
    // A trainer is never a trainee of the group they train, so the picker must not offer them.
    const nonTraineeIds = useMemo(() => [...memberIds, ...trainerIds], [memberIds, trainerIds]);

    const addMemberFactory = useMemo(() => createMemberFilteredFactory(nonTraineeIds), [nonTraineeIds]);
    const addTrainerFactory = useMemo(() => createMemberFilteredFactory(trainerIds), [trainerIds]);

    const trainingGroup = resourceData;
    const trainingGroupActions = halResource(trainingGroup);
    const addMemberAction = trainingGroupActions.template('addTrainingGroupMember');
    const addTrainerAction = trainingGroupActions.template('addTrainer');
    const deleteTrainingGroupAction = trainingGroupActions.template('deleteTrainingGroup');

    const trainingGroupInitialData = trainingGroup as Record<string, unknown>;
    const addMemberDialog = useTemplateDialog({fieldsFactory: addMemberFactory, initialData: trainingGroupInitialData});
    const removeMemberDialog = useTemplateDialog();
    const addTrainerDialog = useTemplateDialog({fieldsFactory: addTrainerFactory, initialData: trainingGroupInitialData});
    const removeTrainerDialog = useTemplateDialog();
    const deleteDialog = useTemplateDialog({
        initialData: trainingGroupInitialData,
        onSubmitSuccess: () => navigate('/training-groups'),
    });

    const openRemoveMember = (member: GetTrainingGroupMembersItemResource) => {
        const action = halResource(member).template('removeTrainingGroupMember');
        if (!action) return;
        removeMemberDialog.open(action, {
            title: (template) => template.title ?? labels.templates.removeTrainingGroupMember,
            successMessage: labels.ui.savedSuccessfully,
            initialData: member as unknown as Record<string, unknown>,
        });
    };

    const openRemoveTrainer = (trainer: GetTrainingGroupTrainersItemResource) => {
        const action = halResource(trainer).template('removeTrainer');
        if (!action) return;
        removeTrainerDialog.open(action, {
            title: labels.templates.removeTrainer,
            successMessage: labels.ui.savedSuccessfully,
            initialData: trainer as unknown as Record<string, unknown>,
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
                    {resourceData.ageRange && (
                        <p className="text-sm text-text-secondary mt-1">
                            {labels.fields.ageRange}: {resourceData.ageRange.minAge}–{resourceData.ageRange.maxAge}
                        </p>
                    )}
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
                    {deleteTrainingGroupAction && (
                        <Button
                            variant="danger"
                            onClick={() => deleteDialog.open(deleteTrainingGroupAction, {
                                title: (template) => template.title ?? labels.templates.deleteTrainingGroup,
                            })}
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
                        onClose={() => setIsEditing(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        submitButtonLabel={labels.buttons.saveChanges}
                    />
                </Card>
            )}

            <hr className="border-border"/>

            {(trainers.length > 0 || addTrainerAction) && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.trainingGroupTrainers}
                        </h3>
                        {addTrainerAction && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => addTrainerDialog.open(addTrainerAction, {
                                    title: labels.templates.addTrainer,
                                    successMessage: labels.ui.savedSuccessfully,
                                })}
                                startIcon={<UserPlus className="w-4 h-4"/>}
                            >
                                {labels.templates.addTrainer}
                            </Button>
                        )}
                    </div>
                    <dl>
                        {trainers.map((trainer) => {
                            const canRemove = halResource(trainer).template('removeTrainer') !== undefined && trainer._links?.self;
                            return (
                                <MemberRowWithRemove
                                    key={trainer.memberId}
                                    memberId={trainer.memberId ?? ''}
                                    memberLink={trainer._links?.member}
                                    removeAriaLabel={labels.templates.removeTrainer}
                                    onRemove={canRemove ? () => openRemoveTrainer(trainer) : undefined}
                                />
                            );
                        })}
                    </dl>
                </Card>
            )}

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.trainingGroupMembers}</h2>
                    {addMemberAction && (
                        <Button
                            variant="primary"
                            onClick={() => addMemberDialog.open(addMemberAction, {
                                title: labels.templates.addTrainingGroupMember,
                                successMessage: labels.ui.savedSuccessfully,
                            })}
                            startIcon={<UserPlus className="w-4 h-4"/>}
                        >
                            {labels.templates.addTrainingGroupMember}
                        </Button>
                    )}
                </div>

                <GroupMembersTable
                    emptyMessage="Skupina nemá žádné členy."
                    members={members.map((member) => ({
                        memberId: member.memberId ?? '',
                        joinedAt: member.joinedAt ?? '',
                        memberLink: member._links?.member,
                        removeAriaLabel: labels.templates.removeTrainingGroupMember,
                        onRemove: halResource(member).template('removeTrainingGroupMember') && member._links?.self ? () => openRemoveMember(member) : undefined,
                    }))}
                />
            </div>

            {addMemberDialog.dialog}
            {removeMemberDialog.dialog}
            {addTrainerDialog.dialog}
            {removeTrainerDialog.dialog}
            {deleteDialog.dialog}
        </div>
    );
};

export const TrainingGroupDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<GetTrainingGroupResource>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{describeError(error)}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <TrainingGroupDetailContent resourceData={resourceData}/>;
};
