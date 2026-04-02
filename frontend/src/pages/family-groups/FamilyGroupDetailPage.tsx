import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Modal, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormsTemplate, HalResourceLinks, HalResponse} from '../../api';
import {formatDate} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';
import {Crown, Trash2, UserMinus} from 'lucide-react';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../../components/members/MemberNameWithRegNumber.tsx';

interface FamilyGroupOwner {
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
    owners?: FamilyGroupOwner[];
    members?: FamilyGroupMember[];
}

const FamilyGroupDetailContent = ({resourceData}: {resourceData: FamilyGroupDetail}): ReactElement => {
    const {route} = useHalPageData<FamilyGroupDetail>();
    const navigate = useNavigate();
    const [deleteModal, setDeleteModal] = useState(false);
    const [addOwnerModal, setAddOwnerModal] = useState(false);
    const [removeOwnerModal, setRemoveOwnerModal] = useState<{template: HalFormsTemplate; ownerSelfHref: string} | null>(null);

    const deleteTemplate = resourceData._templates?.deleteFamilyGroup ?? null;
    const addOwnerTemplate = resourceData._templates?.addFamilyGroupOwner ?? null;

    const owners: FamilyGroupOwner[] = resourceData.owners ?? [];
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

            {(owners.length > 0 || addOwnerTemplate) && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.familyGroupOwners}
                        </h3>
                        {addOwnerTemplate && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => setAddOwnerModal(true)}
                                startIcon={<Crown className="w-4 h-4"/>}
                            >
                                {labels.templates.addFamilyGroupOwner}
                            </Button>
                        )}
                    </div>
                    <dl>
                        {owners.map((owner) => {
                            const removeOwnerTpl = owner._templates?.removeFamilyGroupOwner;
                            const selfHref = owner._links?.self?.href ?? '';
                            return (
                                <DetailRow key={owner.memberId} label="">
                                    <div className="flex items-center justify-between w-full">
                                        <HalRouteProvider routeLink={owner._links.member}>
                                            <MemberNameWithRegNumber/>
                                        </HalRouteProvider>
                                        {removeOwnerTpl && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-red-600"
                                                aria-label={labels.templates.removeFamilyGroupOwner}
                                                onClick={() => setRemoveOwnerModal({template: removeOwnerTpl, ownerSelfHref: selfHref})}
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

            {addOwnerTemplate && addOwnerModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setAddOwnerModal(false)}
                    title={labels.templates.addFamilyGroupOwner}
                    size="md"
                >
                    <HalFormDisplay
                        template={addOwnerTemplate as HalFormsTemplate}
                        templateName="addFamilyGroupOwner"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => { setAddOwnerModal(false); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {removeOwnerModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setRemoveOwnerModal(null)}
                    title={labels.templates.removeFamilyGroupOwner}
                    size="md"
                >
                    <HalFormDisplay
                        template={removeOwnerModal.template}
                        templateName="removeFamilyGroupOwner"
                        resourceData={{}}
                        pathname={removeOwnerModal.ownerSelfHref ? '/family-groups/' + removeOwnerModal.ownerSelfHref.split('/family-groups/')[1] : route.pathname}
                        onClose={() => { setRemoveOwnerModal(null); void route.refetch(); }}
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
