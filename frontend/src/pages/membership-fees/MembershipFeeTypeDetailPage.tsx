import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {useTemplateDialog} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {halResource} from '../../api/halResource';
import type {GetFeeTypeResource} from '../../api';
import {labels} from '../../localization';
import {ChevronRight, Pencil, Save, Trash2} from 'lucide-react';
import {HalSubresourceProvider} from '../../contexts/HalRouteContext.tsx';
import {RulesTable} from '../../components/membership-fees/RulesTable.tsx';
import {HalPageGate} from '../HalPageGate.tsx';

type FeeTypeDetail = GetFeeTypeResource;

const FeeTypeDetailContent = ({resourceData}: {resourceData: FeeTypeDetail}): ReactElement => {
    const navigate = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const deleteFeeTypeAction = halResource(resourceData).template('deleteFeeType');
    const deleteDialog = useTemplateDialog({
        onSubmitSuccess: () => navigate('/membership-fee-types'),
    });

    const editTemplate = resourceData._templates?.editFeeType ?? null;

    return (
        <div className="flex flex-col gap-6">
            <nav className="flex items-center gap-1 text-sm text-text-secondary">
                <Link to="/membership-fee-types" className="hover:text-text-primary transition-colors">
                    {labels.nav['membership-fees']}
                </Link>
                <ChevronRight className="w-4 h-4 text-text-tertiary"/>
                <Link to="/membership-fee-types" className="hover:text-text-primary transition-colors">
                    {labels.nav['membership-fee-types']}
                </Link>
                <ChevronRight className="w-4 h-4 text-text-tertiary"/>
                <span className="text-text-primary font-medium">{resourceData.name}</span>
            </nav>

            <div className="bg-surface rounded-xl border border-border-subtle p-6">
                <h2 className="text-base font-bold text-text-primary mb-4">Základní informace</h2>
                <hr className="border-border-subtle mb-5"/>

                {!isEditing ? (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-5">
                            <div>
                                <label className="block text-xs font-bold text-text-secondary mb-1">Název tieru</label>
                                <div className="h-[38px] flex items-center px-3 border border-border-subtle rounded-md text-sm text-text-primary">
                                    {resourceData.name}
                                </div>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-text-secondary mb-1">Roční poplatek (Kč)</label>
                                <div className="h-[38px] flex items-center px-3 border border-border-subtle rounded-md text-sm text-text-primary">
                                    {resourceData.yearlyFeeAmount} {resourceData.yearlyFeeCurrency ?? labels.finance.currency}
                                </div>
                            </div>
                        </div>
                        <div className="flex gap-3">
                            {editTemplate && (
                                <button
                                    type="button"
                                    onClick={() => setIsEditing(true)}
                                    className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-text-secondary bg-surface border border-border-subtle hover:bg-bg-subtle transition-colors"
                                >
                                    <Pencil className="w-[15px] h-[15px]"/>
                                    {labels.buttons.edit}
                                </button>
                            )}
                            {deleteFeeTypeAction && (
                                <button
                                    type="button"
                                    onClick={() => deleteDialog.open(deleteFeeTypeAction, {
                                        title: (template) => template.title ?? labels.templates.deleteMembershipFeeType,
                                    })}                                    className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-error bg-error-bg border border-error hover:opacity-80 transition-colors"
                                >
                                    <Trash2 className="w-[15px] h-[15px]"/>
                                    {labels.templates.deleteMembershipFeeType}
                                </button>
                            )}
                        </div>
                    </>
                ) : (
                    editTemplate && (
                        <HalFormDisplay
                            template={editTemplate}
                            templateName="editFeeType"
                            resourceData={resourceData as unknown as Record<string, unknown>}
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

            {deleteDialog.dialog}
        </div>
    );
};

export const MembershipFeeTypeDetailPage = (): ReactElement => (
    <HalPageGate<FeeTypeDetail>>
        {(resourceData) => <FeeTypeDetailContent resourceData={resourceData}/>}
    </HalPageGate>
);
