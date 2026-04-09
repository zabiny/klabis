import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Modal, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import type {HalFormsTemplate, HalResourceLinks, HalResponse} from '../../api';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
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

interface FamilyGroupChild {
    memberId: string;
    joinedAt: string;
    _links: {
        member: HalResourceLinks;
        self?: { href: string };
    };
    _templates?: Record<string, HalFormsTemplate>;
}

interface FamilyGroupDetail extends HalResponse {
    id: string;
    name: string;
    parents?: FamilyGroupParent[];
    members?: FamilyGroupChild[];
}

type AddMemberRole = 'parent' | 'child';

interface AddMemberModalState {
    role: AddMemberRole;
    template: HalFormsTemplate;
}

const FamilyGroupDetailContent = ({resourceData}: {resourceData: FamilyGroupDetail}): ReactElement => {
    const {route} = useHalPageData<FamilyGroupDetail>();
    const navigate = useNavigate();
    const [deleteModal, setDeleteModal] = useState(false);
    const [addMemberRolePicker, setAddMemberRolePicker] = useState(false);
    const [addMemberModal, setAddMemberModal] = useState<AddMemberModalState | null>(null);
    const [removeParentModal, setRemoveParentModal] = useState<{template: HalFormsTemplate; parentSelfHref: string} | null>(null);
    const [removeChildModal, setRemoveChildModal] = useState<{template: HalFormsTemplate; childSelfHref: string} | null>(null);

    const deleteTemplate = resourceData._templates?.deleteFamilyGroup ?? null;
    const addParentTemplate = resourceData._templates?.addFamilyGroupParent ?? null;
    const addChildTemplate = resourceData._templates?.addFamilyGroupChild ?? null;
    const hasAddMemberButton = addParentTemplate != null || addChildTemplate != null;

    const parents: FamilyGroupParent[] = resourceData.parents ?? [];
    const children: FamilyGroupChild[] = resourceData.members ?? [];

    const openAddMemberRolePicker = () => setAddMemberRolePicker(true);
    const closeAddMember = () => {
        setAddMemberRolePicker(false);
        setAddMemberModal(null);
    };
    const selectRole = (role: AddMemberRole) => {
        const template = role === 'parent' ? addParentTemplate : addChildTemplate;
        if (!template) return;
        setAddMemberRolePicker(false);
        setAddMemberModal({role, template});
    };

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
                    {hasAddMemberButton && (
                        <Button
                            variant="primary"
                            onClick={openAddMemberRolePicker}
                            startIcon={<UserPlus className="w-4 h-4"/>}
                        >
                            {labels.templates.addFamilyGroupMember}
                        </Button>
                    )}
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

            {parents.length > 0 && (
                <Card className="p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary">
                            {labels.sections.familyGroupParents}
                        </h3>
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
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.familyGroupChildren}</h2>

                {children.length === 0 ? (
                    <p className="text-sm text-text-tertiary">Skupina nemá žádné děti.</p>
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
                            {children.map((child) => (
                                <tr key={child.memberId}
                                    className="border-b border-border last:border-0 hover:bg-slate-50 dark:hover:bg-zinc-800/50">
                                    <td className="px-4 py-3">
                                        <HalRouteProvider routeLink={child._links.member}>
                                            <MemberNameWithRegNumber/>
                                        </HalRouteProvider>
                                    </td>
                                    <td className="px-4 py-3 text-text-secondary">{formatDate(child.joinedAt)}</td>
                                    <td className="px-4 py-3 text-right">
                                        {child._templates?.removeFamilyGroupChild && child._links.self && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-red-600"
                                                aria-label={labels.templates.removeFamilyGroupChild}
                                                onClick={() => setRemoveChildModal({
                                                    template: child._templates!.removeFamilyGroupChild,
                                                    childSelfHref: child._links.self!.href,
                                                })}
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

            {addMemberRolePicker && (
                <Modal
                    isOpen={true}
                    onClose={closeAddMember}
                    title={labels.templates.addFamilyGroupMember}
                    size="sm"
                >
                    <div className="flex flex-col gap-3 p-2">
                        {addParentTemplate && (
                            <Button
                                variant="secondary"
                                onClick={() => selectRole('parent')}
                            >
                                {labels.familyGroupRoles.parent}
                            </Button>
                        )}
                        {addChildTemplate && (
                            <Button
                                variant="secondary"
                                onClick={() => selectRole('child')}
                            >
                                {labels.familyGroupRoles.child}
                            </Button>
                        )}
                    </div>
                </Modal>
            )}

            {addMemberModal && (
                <Modal
                    isOpen={true}
                    onClose={closeAddMember}
                    title={addMemberModal.role === 'parent' ? labels.templates.addFamilyGroupParent : labels.templates.addFamilyGroupChild}
                    size="md"
                >
                    <HalFormDisplay
                        template={addMemberModal.template}
                        templateName={addMemberModal.role === 'parent' ? 'addFamilyGroupParent' : 'addFamilyGroupChild'}
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => { closeAddMember(); void route.refetch(); }}
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
                        pathname={removeParentModal.parentSelfHref ? extractNavigationPath(removeParentModal.parentSelfHref) : route.pathname}
                        onClose={() => { setRemoveParentModal(null); void route.refetch(); }}
                        successMessage={labels.ui.savedSuccessfully}
                    />
                </Modal>
            )}

            {removeChildModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setRemoveChildModal(null)}
                    title={labels.templates.removeFamilyGroupChild}
                    size="md"
                >
                    <HalFormDisplay
                        template={removeChildModal.template}
                        templateName="removeFamilyGroupChild"
                        resourceData={{}}
                        pathname={extractNavigationPath(removeChildModal.childSelfHref)}
                        onClose={() => { setRemoveChildModal(null); void route.refetch(); }}
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
