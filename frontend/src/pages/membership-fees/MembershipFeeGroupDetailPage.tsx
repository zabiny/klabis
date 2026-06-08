import {type ReactElement, useState} from 'react';
import {Link} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {labels, getEnumLabel} from '../../localization';
import {Pencil, UserPlus} from 'lucide-react';
import {formatDate} from '../../utils/dateUtils.ts';

interface CoParticipationRule {
    raceTypeId: string;
    ranking: string;
    ruleType: 'PERCENTAGE' | 'FIXED_SURCHARGE';
    value: number;
}

interface FeeGroupMember {
    memberId: string;
    joinedAt: string;
    source: 'MEMBER_CHOICE' | 'ADMIN_ASSIGNMENT';
}

interface MembershipFeeGroupDetail extends HalResponse {
    id: string;
    name: string;
    annualFeeSnapshot: number;
    status: 'EDITABLE' | 'FROZEN';
    coParticipationRules?: CoParticipationRule[];
    members?: FeeGroupMember[];
}

const MembershipFeeGroupDetailContent = ({resourceData}: {resourceData: MembershipFeeGroupDetail}): ReactElement => {
    const {route} = useHalPageData<MembershipFeeGroupDetail>();
    const [isEditing, setIsEditing] = useState(false);
    const [assignMemberModal, setAssignMemberModal] = useState(false);

    const editTemplate = resourceData._templates?.updateMembershipFeeGroup ?? null;
    const assignMemberTemplate = resourceData._templates?.assignMember ?? null;

    const rules = resourceData.coParticipationRules ?? [];
    const members = resourceData.members ?? [];

    const canEdit = resourceData.status === 'EDITABLE' && !!editTemplate;

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to=".." relative="path" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>
                    <span className={`inline-flex items-center mt-2 px-2 py-0.5 rounded text-xs font-medium ${
                        resourceData.status === 'EDITABLE'
                            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                            : 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300'
                    }`}>
                        {getEnumLabel('feeGroupStatus', resourceData.status)}
                    </span>
                </div>

                <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                    {canEdit && !isEditing && (
                        <Button
                            variant="secondary"
                            onClick={() => setIsEditing(true)}
                            startIcon={<Pencil className="w-4 h-4"/>}
                        >
                            {labels.templates.updateMembershipFeeGroup}
                        </Button>
                    )}
                    {assignMemberTemplate && (
                        <Button
                            variant="primary"
                            onClick={() => setAssignMemberModal(true)}
                            startIcon={<UserPlus className="w-4 h-4"/>}
                        >
                            {labels.templates.assignMember}
                        </Button>
                    )}
                </div>
            </div>

            <Card className="p-6">
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <dt className="text-xs uppercase font-semibold text-text-secondary">{labels.fields.annualFee}</dt>
                        <dd className="mt-1 text-text-primary font-medium">{resourceData.annualFeeSnapshot} {labels.finance.currency}</dd>
                    </div>
                </dl>
            </Card>

            {isEditing && editTemplate && (
                <Card className="p-6">
                    <HalFormDisplay
                        template={editTemplate as HalFormsTemplate}
                        templateName="updateMembershipFeeGroup"
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
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.raceTypeId}</th>
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.ranking}</th>
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.ruleType}</th>
                                    <th className="px-4 py-3 text-right font-semibold text-text-secondary">{labels.tables.value}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {rules.map((rule, index) => (
                                    <tr key={index} className="border-b border-border last:border-0">
                                        <td className="px-4 py-3 text-text-primary">{rule.raceTypeId}</td>
                                        <td className="px-4 py-3 text-text-primary">{rule.ranking}</td>
                                        <td className="px-4 py-3 text-text-primary">{rule.ruleType}</td>
                                        <td className="px-4 py-3 text-right text-text-primary">
                                            {rule.ruleType === 'PERCENTAGE'
                                                ? `${rule.value} %`
                                                : `${rule.value} ${labels.finance.currency}`}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </Card>
                </div>
            )}

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.membershipFeeGroupMembers}</h2>
                {members.length === 0 ? (
                    <p className="text-text-secondary text-sm">Žádní členové ve skupině.</p>
                ) : (
                    <Card className="p-0 overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b border-border bg-surface-secondary">
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.memberId}</th>
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.tables.joinedAt}</th>
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.source}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {members.map((member) => (
                                    <tr key={member.memberId} className="border-b border-border last:border-0">
                                        <td className="px-4 py-3 text-text-primary font-medium">{member.memberId}</td>
                                        <td className="px-4 py-3 text-text-primary">{formatDate(member.joinedAt)}</td>
                                        <td className="px-4 py-3 text-text-primary">
                                            {getEnumLabel('feeMemberSource', member.source)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </Card>
                )}
            </div>

            {assignMemberTemplate && assignMemberModal && (
                <HalFormModal
                    title={labels.templates.assignMember}
                    template={assignMemberTemplate as HalFormsTemplate}
                    templateName="assignMember"
                    resourceData={resourceData as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => { setAssignMemberModal(false); void route.refetch(); }}
                    successMessage={labels.ui.savedSuccessfully}
                />
            )}
        </div>
    );
};

export const MembershipFeeGroupDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<MembershipFeeGroupDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <MembershipFeeGroupDetailContent resourceData={resourceData}/>;
};
