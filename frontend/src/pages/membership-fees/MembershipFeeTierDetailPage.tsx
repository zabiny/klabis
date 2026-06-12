import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {labels} from '../../localization';
import {ChevronRight, Pencil, Save, Trash2} from 'lucide-react';
import {HalSubresourceProvider} from '../../contexts/HalRouteContext.tsx';
import {RulesTable} from '../../components/membership-fees/RulesTable.tsx';

interface FeeTierDetail extends HalResponse {
    id: string;
    name: string;
    yearlyFeeAmount: number;
    yearlyFeeCurrency: string;
}

const FeeTierDetailContent = ({resourceData}: {resourceData: FeeTierDetail}): ReactElement => {
    const {route} = useHalPageData<FeeTierDetail>();
    const navigate = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const [deleteModal, setDeleteModal] = useState(false);

    const editTemplate = resourceData._templates?.editTier ?? null;
    const deleteTemplate = resourceData._templates?.deleteTier ?? null;

    return (
        <div className="flex flex-col gap-6">
            <nav className="flex items-center gap-1 text-sm text-zinc-500">
                <Link to="/membership-fee-tiers" className="hover:text-zinc-700 transition-colors">
                    Katalog tierů
                </Link>
                <ChevronRight className="w-4 h-4 text-zinc-400"/>
                <span className="text-zinc-800 font-medium">{resourceData.name}</span>
            </nav>

            <div className="bg-white rounded-xl border border-zinc-200 p-6">
                <h2 className="text-base font-bold text-zinc-900 mb-4">Základní informace</h2>
                <hr className="border-slate-100 mb-5"/>

                {!isEditing ? (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-5">
                            <div>
                                <label className="block text-xs font-bold text-gray-700 mb-1">Název tieru</label>
                                <div className="h-[38px] flex items-center px-3 border border-zinc-200 rounded-md text-sm text-zinc-800">
                                    {resourceData.name}
                                </div>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-gray-700 mb-1">Roční poplatek (Kč)</label>
                                <div className="h-[38px] flex items-center px-3 border border-zinc-200 rounded-md text-sm text-zinc-800">
                                    {resourceData.yearlyFeeAmount} {resourceData.yearlyFeeCurrency ?? labels.finance.currency}
                                </div>
                            </div>
                        </div>
                        <div className="flex gap-3">
                            {editTemplate && (
                                <button
                                    type="button"
                                    onClick={() => setIsEditing(true)}
                                    className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-zinc-700 bg-white border border-zinc-200 hover:bg-zinc-50 transition-colors"
                                >
                                    <Pencil className="w-[15px] h-[15px]"/>
                                    {labels.buttons.edit}
                                </button>
                            )}
                            {deleteTemplate && (
                                <button
                                    type="button"
                                    onClick={() => setDeleteModal(true)}
                                    className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-red-600 bg-red-50 border border-red-200 hover:bg-red-100 transition-colors"
                                >
                                    <Trash2 className="w-[15px] h-[15px]"/>
                                    {labels.templates.deleteMembershipFeeTier}
                                </button>
                            )}
                        </div>
                    </>
                ) : (
                    editTemplate && (
                        <HalFormDisplay
                            template={editTemplate as HalFormsTemplate}
                            templateName="editTier"
                            resourceData={resourceData as unknown as Record<string, unknown>}
                            pathname={route.pathname}
                            onClose={() => setIsEditing(false)}
                            successMessage={labels.ui.savedSuccessfully}
                            submitButtonLabel={labels.buttons.saveChanges}
                            submitIcon={<Save className="w-[15px] h-[15px]"/>}
                            navigateOnSuccess={false}
                        />
                    )
                )}
            </div>

            <HalSubresourceProvider subresourceLinkName="rules">
                <RulesTable/>
            </HalSubresourceProvider>

            {deleteTemplate && deleteModal && (
                <HalFormModal
                    title={(deleteTemplate as HalFormsTemplate).title ?? labels.templates.deleteMembershipFeeTier}
                    template={deleteTemplate as HalFormsTemplate}
                    templateName="deleteTier"
                    resourceData={resourceData as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => setDeleteModal(false)}
                    onSubmitSuccess={() => navigate('/membership-fee-tiers')}
                />
            )}
        </div>
    );
};

export const MembershipFeeTierDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<FeeTierDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <FeeTierDetailContent resourceData={resourceData}/>;
};
