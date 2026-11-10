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

    const addMemberDialog = useTemplateDialog('addTrainingGroupMember', {
        resource: trainingGroup,
        title: labels.templates.addTrainingGroupMember,
        successMessage: labels.ui.savedSuccessfully,
        fieldsFactory: addMemberFactory,
    });
    const removeMemberDialog = useTemplateDialog('removeTrainingGroupMember', {
        title: (template) => template.title ?? labels.templates.removeTrainingGroupMember,
        successMessage: labels.ui.savedSuccessfully,
    });
    const addTrainerDialog = useTemplateDialog('addTrainer', {
        resource: trainingGroup,
        title: labels.templates.addTrainer,
        successMessage: labels.ui.savedSuccessfully,
        fieldsFactory: addTrainerFactory,
    });
    const removeTrainerDialog = useTemplateDialog('removeTrainer', {
        title: labels.templates.removeTrainer,
        successMessage: labels.ui.savedSuccessfully,
    });
    const deleteDialog = useTemplateDialog('deleteTrainingGroup', {
        resource: trainingGroup,
        title: (template) => template.title ?? labels.templates.deleteTrainingGroup,
        onSubmitSuccess: () => navigate('/training-groups'),
    });

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
                    {deleteDialog.available && (
                        <Button
                            variant="danger"
                            onClick={() => deleteDialog.open()}
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

            {(trainers.length > 0 || addTrainerDialog.available) && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.trainingGroupTrainers}
                        </h3>
                        {addTrainerDialog.available && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => addTrainerDialog.open()}
                                startIcon={<UserPlus className="w-4 h-4"/>}
                            >
                                {labels.templates.addTrainer}
                            </Button>
                        )}
                    </div>
                    <dl>
                        {trainers.map((trainer) => {
                            const canRemove = removeTrainerDialog.availableOn(trainer) && trainer._links?.self;
                            return (
                                <MemberRowWithRemove
                                    key={trainer.memberId}
                                    memberId={trainer.memberId ?? ''}
                                    memberLink={trainer._links?.member}
                                    removeAriaLabel={labels.templates.removeTrainer}
                                    onRemove={canRemove ? () => removeTrainerDialog.open(trainer) : undefined}
                                />
                            );
                        })}
                    </dl>
                </Card>
            )}

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.trainingGroupMembers}</h2>
                    {addMemberDialog.available && (
                        <Button
                            variant="primary"
                            onClick={() => addMemberDialog.open()}
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
                        onRemove: removeMemberDialog.availableOn(member) && member._links?.self ? () => removeMemberDialog.open(member) : undefined,
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
