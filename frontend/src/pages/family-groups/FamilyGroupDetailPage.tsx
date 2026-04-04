import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Modal, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormsTemplate, HalResourceLinks, HalResponse} from '../../api';
import {formatDate} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';
import {Trash2, UserMinus, UserPlus} from 'lucide-react';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../../components/members/MemberNameWithRegNumber.tsx';

interface FamilyGroupParent {
    memberId: string;
    _links: {
        member: HalResourceLinks;
        self?: { href: string };
    };
    _templates?: Record<string, HalFormsTemplate>;
}

interface FamilyGroupMember {
    memberId: string;
    joinedAt: string;
    _links: {
        member: HalResourceLinks;
    };
}

interface FamilyGroupDetail extends HalResponse {
    id: string;
    name: string;
    parents?: FamilyGroupParent[];
    members?: FamilyGroupMember[];
}

const FamilyGroupDetailContent = ({resourceData}: {resourceData: FamilyGroupDetail}): ReactElement => {
    const {route} = useHalPageData<FamilyGroupDetail>();
    const navigate = useNavigate();
    const [deleteModal, setDeleteModal] = useState(false);
    const [addParentModal, setAddParentModal] = useState(false);
    const [removeParentModal, setRemoveParentModal] = useState<{template: HalFormsTemplate; parentSelfHref: string} | null>(null);

    const deleteTemplate = resourceData._templates?.deleteFamilyGroup ?? null;
    const addParentTemplate = resourceData._templates?.addFamilyGroupParent ?? null;

    const parents: FamilyGroupParent[] = resourceData.parents ?? [];
    const members: FamilyGroupMember[] = resourceData.members ?? [];

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/family-groups" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>

                <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                    {deleteTemplate && (
                        <Button
                            variant="danger"
                            onClick={() => setDeleteModal(true)}
                            startIcon={<Trash2 className="w-4 h-4"/>}
                        >
                            {labels.templates.deleteFamilyGroup}
                        </Button>
                    )}
                </div>
            </div>

            <hr className="border-border"/>

            {(parents.length > 0 || addParentTemplate) && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.familyGroupParents}
                        </h3>
                        {addParentTemplate && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => setAddParentModal(true)}
                                startIcon={<UserPlus className="w-4 h-4"/>}
                            >
                                {labels.templates.addFamilyGroupParent}
                            </Button>
                        )}
                    </div>
                    <dl>
                        {parents.map((parent) => {
                            const removeParentTpl = parent._templates?.removeFamilyGroupParent;
                            const selfHref = parent._links?.self?.href ?? '';
                            return (
                                <DetailRow key={parent.memberId} label="">
                                    <div className="flex items-center justify-between w-full">
                                        <HalRouteProvider routeLink={parent._links.member}>
                                            <MemberNameWithRegNumber/>
                                        </HalRouteProvider>
                                        {removeParentTpl && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-red-600"
                                                aria-label={labels.templates.removeFamilyGroupParent}
                                                onClick={() => setRemoveParentModal({template: removeParentTpl, parentSelfHref: selfHref})}
                                            >
                                                <UserMinus className="w-4 h-4"/>
                                            </Button>
                                        )}
                                    </div>
                                </DetailRow>
                            );
                        })}
                    </dl>
                </Card>
            )}

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.familyGroupMembers}</h2>

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
                            </tr>
                            </thead>
                            <tbody>
                            {members.map((member) => (
                                <tr key={member.memberId}
                                    className="border-b border-border last:border-0 hover:bg-slate-50 dark:hover:bg-zinc-800/50">
                                    <td className="px-4 py-3">
                                        <HalRouteProvider routeLink={member._links.member}>
                                            <MemberNameWithRegNumber/>
                                        </HalRouteProvider>
                                    </td>
                                    <td className="px-4 py-3 text-text-secondary">{formatDate(member.joinedAt)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </Card>
                )}
            </div>

            {addParentTemplate && addParentModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setAddParentModal(false)}
                    title={labels.templates.addFamilyGroupParent}
                    size="md"
                >
                    <HalFormDisplay
                        template={addParentTemplate as HalFormsTemplate}
                        templateName="addFamilyGroupParent"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => { setAddParentModal(false); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {removeParentModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setRemoveParentModal(null)}
                    title={labels.templates.removeFamilyGroupParent}
                    size="md"
                >
                    <HalFormDisplay
                        template={removeParentModal.template}
                        templateName="removeFamilyGroupParent"
                        resourceData={{}}
                        pathname={removeParentModal.parentSelfHref ? '/family-groups/' + removeParentModal.parentSelfHref.split('/family-groups/')[1] : route.pathname}
                        onClose={() => { setRemoveParentModal(null); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {deleteTemplate && deleteModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setDeleteModal(false)}
                    title={(deleteTemplate as HalFormsTemplate).title ?? labels.templates.deleteFamilyGroup}
                    size="md"
                >
                    <HalFormDisplay
                        template={deleteTemplate as HalFormsTemplate}
                        templateName="deleteFamilyGroup"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setDeleteModal(false)}
                        onSubmitSuccess={() => navigate('/family-groups')}
                    />
                </Modal>
            )}
        </div>
    );
};

export const FamilyGroupDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<FamilyGroupDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <FamilyGroupDetailContent resourceData={resourceData}/>;
};
