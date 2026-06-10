import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {labels} from '../../localization';
import {ChevronRight, Pencil, Plus, Save, Trash2, X} from 'lucide-react';

interface CoParticipationRule {
    eventTypeId: string;
    rankingShortName: string;
    ruleType: 'PERCENTAGE' | 'FIXED_SURCHARGE';
    percent?: number;
    fixedAmount?: number;
    fixedCurrency?: string;
}

interface FeeLevelDetail extends HalResponse {
    id: string;
    name: string;
    yearlyFeeAmount: number;
    yearlyFeeCurrency: string;
    rules?: CoParticipationRule[];
}

const RuleTypeBadge = ({ruleType}: {ruleType: 'PERCENTAGE' | 'FIXED_SURCHARGE'}): ReactElement => {
    if (ruleType === 'PERCENTAGE') {
        return (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700">
                Procento
            </span>
        );
    }
    return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-50 text-green-700">
            Fixní částka
        </span>
    );
};

const FeeLevelDetailContent = ({resourceData}: {resourceData: FeeLevelDetail}): ReactElement => {
    const {route} = useHalPageData<FeeLevelDetail>();
    const navigate = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const [deleteModal, setDeleteModal] = useState(false);
    const [addRuleModal, setAddRuleModal] = useState(false);

    const editTemplate = resourceData._templates?.editLevel ?? null;
    const deleteTemplate = resourceData._templates?.deleteLevel ?? null;
    const addRuleTemplate = resourceData._templates?.addRule ?? null;
    const rules = resourceData.rules ?? [];

    return (
        <div className="flex flex-col gap-6">
            {/* Breadcrumb */}
            <nav className="flex items-center gap-1 text-sm text-zinc-500">
                <Link to="/membership-fee-levels" className="hover:text-zinc-700 transition-colors">
                    Katalog úrovní
                </Link>
                <ChevronRight className="w-4 h-4 text-zinc-400"/>
                <span className="text-zinc-800 font-medium">{resourceData.name}</span>
            </nav>

            {/* Basic info card */}
            <div className="bg-white rounded-xl border border-zinc-200 p-6">
                <h2 className="text-base font-bold text-zinc-900 mb-4">Základní informace</h2>
                <hr className="border-slate-100 mb-5"/>

                {!isEditing ? (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-5">
                            <div>
                                <label className="block text-xs font-bold text-gray-700 mb-1">Název úrovně</label>
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
                                    {labels.templates.deleteMembershipFeeLevel}
                                </button>
                            )}
                        </div>
                    </>
                ) : (
                    editTemplate && (
                        <HalFormDisplay
                            template={editTemplate as HalFormsTemplate}
                            templateName="editLevel"
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

            {/* Co-participation rules card */}
            <div className="bg-white rounded-xl border border-zinc-200 overflow-hidden">
                {/* Card header */}
                <div className="px-6 py-4">
                    <h2 className="text-base font-bold text-zinc-900">Pravidla spoluúčasti</h2>
                    <p className="text-[13px] text-zinc-500 mt-0.5">Příplatky ke startovnému na závodech</p>
                </div>
                <hr className="border-zinc-200"/>

                {/* Table */}
                <table className="w-full text-sm">
                    <thead>
                        <tr className="bg-slate-50" style={{height: '44px'}}>
                            <th className="px-5 text-left font-semibold text-zinc-500 text-xs">Typ závodu</th>
                            <th className="px-5 text-left font-semibold text-zinc-500 text-xs w-[100px]">Žebříček</th>
                            <th className="px-5 text-left font-semibold text-zinc-500 text-xs w-[160px]">Typ pravidla</th>
                            <th className="px-5 text-left font-semibold text-zinc-500 text-xs w-[100px]">Hodnota</th>
                            <th className="px-5 w-[40px]"/>
                        </tr>
                    </thead>
                    <tbody>
                        {rules.map((rule, index) => (
                            <tr key={index} className="border-t border-zinc-100" style={{height: '52px'}}>
                                <td className="px-5 text-zinc-800">{rule.eventTypeId}</td>
                                <td className="px-5 text-zinc-800">{rule.rankingShortName}</td>
                                <td className="px-5">
                                    <RuleTypeBadge ruleType={rule.ruleType}/>
                                </td>
                                <td className="px-5 text-zinc-800">
                                    {rule.ruleType === 'PERCENTAGE'
                                        ? `${rule.percent} %`
                                        : `${rule.fixedAmount} ${rule.fixedCurrency ?? labels.finance.currency}`}
                                </td>
                                <td className="px-3">
                                    <button
                                        type="button"
                                        aria-label="Smazat pravidlo"
                                        className="w-8 h-8 flex items-center justify-center rounded-md text-red-500 bg-red-50 hover:bg-red-100 transition-colors"
                                    >
                                        <X className="w-4 h-4"/>
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                {/* Add rule footer */}
                {addRuleTemplate && (
                    <>
                        <div className="border-t border-zinc-200"/>
                        <button
                            type="button"
                            onClick={() => setAddRuleModal(true)}
                            className="w-full flex items-center gap-2 px-5 bg-slate-50 hover:bg-slate-100 transition-colors text-blue-600 font-bold text-sm"
                            style={{height: '48px'}}
                        >
                            <Plus className="w-4 h-4 text-blue-600"/>
                            Přidat pravidlo…
                        </button>
                    </>
                )}
            </div>

            {/* Delete modal */}
            {deleteTemplate && deleteModal && (
                <HalFormModal
                    title={(deleteTemplate as HalFormsTemplate).title ?? labels.templates.deleteMembershipFeeLevel}
                    template={deleteTemplate as HalFormsTemplate}
                    templateName="deleteLevel"
                    resourceData={resourceData as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => setDeleteModal(false)}
                    onSubmitSuccess={() => navigate('/membership-fee-levels')}
                />
            )}

            {/* Add rule modal */}
            {addRuleTemplate && addRuleModal && (
                <HalFormModal
                    title={(addRuleTemplate as HalFormsTemplate).title ?? 'Přidat pravidlo'}
                    template={addRuleTemplate as HalFormsTemplate}
                    templateName="addRule"
                    resourceData={resourceData as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => setAddRuleModal(false)}
                />
            )}
        </div>
    );
};

export const MembershipFeeLevelDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<FeeLevelDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <FeeLevelDetailContent resourceData={resourceData}/>;
};
