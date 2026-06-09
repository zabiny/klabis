import {type ReactElement, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {labels} from '../../localization';
import {Pencil, Trash2} from 'lucide-react';

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
    coParticipationRules?: CoParticipationRule[];
}

const FeeLevelDetailContent = ({resourceData}: {resourceData: FeeLevelDetail}): ReactElement => {
    const {route} = useHalPageData<FeeLevelDetail>();
    const navigate = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const [deleteModal, setDeleteModal] = useState(false);

    const editTemplate = resourceData._templates?.editLevel ?? null;
    const deleteTemplate = resourceData._templates?.deleteLevel ?? null;
    const rules = resourceData.coParticipationRules ?? [];

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/membership-fee-levels" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>

                <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                    {editTemplate && !isEditing && (
                        <Button
                            variant="secondary"
                            onClick={() => setIsEditing(true)}
                            startIcon={<Pencil className="w-4 h-4"/>}
                        >
                            {labels.buttons.edit}
                        </Button>
                    )}
                    {deleteTemplate && (
                        <Button
                            variant="danger"
                            onClick={() => setDeleteModal(true)}
                            startIcon={<Trash2 className="w-4 h-4"/>}
                        >
                            {labels.templates.deleteMembershipFeeLevel}
                        </Button>
                    )}
                </div>
            </div>

            <Card className="p-6">
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <dt className="text-xs uppercase font-semibold text-text-secondary">{labels.fields.yearlyFeeAmount}</dt>
                        <dd className="mt-1 text-text-primary font-medium">{resourceData.yearlyFeeAmount} {resourceData.yearlyFeeCurrency ?? labels.finance.currency}</dd>
                    </div>
                </dl>
            </Card>

            {isEditing && editTemplate && (
                <Card className="p-6">
                    <HalFormDisplay
                        template={editTemplate as HalFormsTemplate}
                        templateName="editLevel"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setIsEditing(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        submitButtonLabel={labels.buttons.saveChanges}
                    />
                </Card>
            )}

            {rules.length > 0 && (
                <div className="flex flex-col gap-4">
                    <h2 className="text-xs uppercase font-semibold text-text-secondary">
                        {labels.sections.coParticipationRules}
                    </h2>
                    <Card className="p-0 overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b border-border bg-surface-secondary">
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.eventTypeId}</th>
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.rankingShortName}</th>
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.ruleType}</th>
                                    <th className="px-4 py-3 text-right font-semibold text-text-secondary">{labels.tables.value}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {rules.map((rule, index) => (
                                    <tr key={index} className="border-b border-border last:border-0">
                                        <td className="px-4 py-3 text-text-primary">{rule.eventTypeId}</td>
                                        <td className="px-4 py-3 text-text-primary">{rule.rankingShortName}</td>
                                        <td className="px-4 py-3 text-text-primary">{rule.ruleType}</td>
                                        <td className="px-4 py-3 text-right text-text-primary">
                                            {rule.ruleType === 'PERCENTAGE'
                                                ? `${rule.percent} %`
                                                : `${rule.fixedAmount} ${rule.fixedCurrency ?? labels.finance.currency}`}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </Card>
                </div>
            )}

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
