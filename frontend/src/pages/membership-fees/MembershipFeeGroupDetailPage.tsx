import {type ReactElement, useState} from 'react';
import {Link} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {labels, getEnumLabel} from '../../localization';
import {ChevronRight, Pencil, UserPlus, Users} from 'lucide-react';
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
    yearlyFeeAmount: number;
    yearlyFeeCurrency: string;
    status: 'EDITABLE' | 'FROZEN';
    coParticipationRules?: CoParticipationRule[];
    members?: FeeGroupMember[];
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

const MemberSourceBadge = ({source}: {source: 'MEMBER_CHOICE' | 'ADMIN_ASSIGNMENT'}): ReactElement => {
    if (source === 'MEMBER_CHOICE') {
        return (
            <span
                className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                style={{background: '#DCFCE7', color: '#15803D'}}
            >
                {getEnumLabel('feeMemberSource', source)}
            </span>
        );
    }
    return (
        <span
            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
            style={{background: '#EFF6FF', color: '#2563EB'}}
        >
            {getEnumLabel('feeMemberSource', source)}
        </span>
    );
};

const StatusBadge = ({status}: {status: 'EDITABLE' | 'FROZEN'}): ReactElement => {
    if (status === 'EDITABLE') {
        return (
            <span
                className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                style={{background: '#DCFCE7', color: '#15803D'}}
            >
                {getEnumLabel('feeGroupStatus', status)}
            </span>
        );
    }
    return (
        <span
            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
            style={{background: '#F4F4F5', color: '#71717A'}}
        >
            {getEnumLabel('feeGroupStatus', status)}
        </span>
    );
};

const MembershipFeeGroupDetailContent = ({resourceData}: {resourceData: MembershipFeeGroupDetail}): ReactElement => {
    const {route} = useHalPageData<MembershipFeeGroupDetail>();
    const [isEditing, setIsEditing] = useState(false);
    const [assignMemberModal, setAssignMemberModal] = useState(false);

    const editTemplate = resourceData._templates?.editSnapshot ?? null;
    const assignMemberTemplate = resourceData._templates?.assignMember ?? null;

    const rules = resourceData.coParticipationRules ?? [];
    const members = resourceData.members ?? [];

    const canEdit = resourceData.status === 'EDITABLE' && !!editTemplate;

    return (
        <div className="flex flex-col gap-6">
            {/* Breadcrumb */}
            <nav className="flex items-center gap-1 text-sm" style={{color: '#71717A'}}>
                <Link to=".." relative="path" className="hover:text-zinc-700 transition-colors">
                    {labels.ui.backToList}
                </Link>
                <ChevronRight className="w-4 h-4" style={{color: '#A1A1AA'}}/>
                <span className="font-medium" style={{color: '#18181B'}}>{resourceData.name}</span>
            </nav>

            <h1 className="sr-only">{resourceData.name}</h1>

            {/* Basic info card */}
            <div
                className="rounded-xl border p-6"
                style={{background: '#FFFFFF', borderColor: '#E4E4E7', borderRadius: '12px'}}
            >
                <div className="flex items-center justify-between mb-4">
                    <h2 className="font-bold" style={{fontSize: '16px', color: '#18181B'}}>
                        Základní informace
                    </h2>
                    <div className="flex gap-3">
                        {canEdit && !isEditing && (
                            <button
                                type="button"
                                onClick={() => setIsEditing(true)}
                                className="inline-flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium border transition-colors hover:bg-zinc-50"
                                style={{borderColor: '#E4E4E7', color: '#374151'}}
                            >
                                <Pencil className="w-4 h-4"/>
                                {labels.buttons.edit}
                            </button>
                        )}
                        {assignMemberTemplate && (
                            <button
                                type="button"
                                onClick={() => setAssignMemberModal(true)}
                                className="inline-flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium text-white transition-colors hover:opacity-90"
                                style={{background: '#2563EB'}}
                            >
                                <UserPlus className="w-4 h-4"/>
                                {labels.templates.assignMember}
                            </button>
                        )}
                    </div>
                </div>
                <hr style={{borderColor: '#E4E4E7'}} className="mb-5"/>

                {isEditing && editTemplate ? (
                    <HalFormDisplay
                        template={editTemplate as HalFormsTemplate}
                        templateName="editSnapshot"
                        resourceData={resourceData as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setIsEditing(false)}
                        successMessage={labels.ui.savedSuccessfully}
                        submitButtonLabel={labels.buttons.saveChanges}
                    />
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                        <div>
                            <label className="block text-xs font-bold mb-1" style={{color: '#374151'}}>
                                {labels.fields.yearlyFeeAmount}
                            </label>
                            <div
                                className="h-[38px] flex items-center px-3 rounded-md text-sm border"
                                style={{borderColor: '#E4E4E7', color: '#18181B'}}
                            >
                                {resourceData.yearlyFeeAmount} {resourceData.yearlyFeeCurrency ?? labels.finance.currency}
                            </div>
                        </div>
                        <div>
                            <label className="block text-xs font-bold mb-1" style={{color: '#374151'}}>
                                Status
                            </label>
                            <div className="h-[38px] flex items-center">
                                <StatusBadge status={resourceData.status}/>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* Co-participation rules card */}
            <div
                className="rounded-xl border overflow-hidden"
                style={{background: '#FFFFFF', borderColor: '#E4E4E7', borderRadius: '12px'}}
            >
                <div className="px-6 py-4">
                    <h2 className="font-bold" style={{fontSize: '16px', color: '#18181B'}}>
                        {labels.sections.coParticipationRules}
                    </h2>
                    <p className="text-[13px] mt-0.5" style={{color: '#71717A'}}>
                        Příplatky ke startovnému na závodech
                    </p>
                </div>
                <hr style={{borderColor: '#E4E4E7'}}/>

                <table className="w-full text-sm">
                    <thead>
                        <tr style={{background: '#F8FAFC', height: '44px'}}>
                            <th className="px-5 text-left text-xs font-semibold" style={{color: '#71717A'}}>
                                {labels.fields.raceTypeId}
                            </th>
                            <th className="px-5 text-left text-xs font-semibold w-[100px]" style={{color: '#71717A'}}>
                                {labels.fields.ranking}
                            </th>
                            <th className="px-5 text-left text-xs font-semibold w-[160px]" style={{color: '#71717A'}}>
                                {labels.fields.ruleType}
                            </th>
                            <th className="px-5 text-left text-xs font-semibold w-[100px]" style={{color: '#71717A'}}>
                                {labels.tables.value}
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {rules.length === 0 ? (
                            <tr>
                                <td colSpan={4} className="px-5 py-8 text-center text-sm" style={{color: '#71717A'}}>
                                    Žádná pravidla spoluúčasti
                                </td>
                            </tr>
                        ) : (
                            rules.map((rule, index) => (
                                <tr key={index} className="border-t" style={{borderColor: '#F4F4F5', height: '52px'}}>
                                    <td className="px-5" style={{color: '#18181B'}}>{rule.raceTypeId}</td>
                                    <td className="px-5" style={{color: '#18181B'}}>{rule.ranking}</td>
                                    <td className="px-5">
                                        <RuleTypeBadge ruleType={rule.ruleType}/>
                                    </td>
                                    <td className="px-5" style={{color: '#18181B'}}>
                                        {rule.ruleType === 'PERCENTAGE'
                                            ? `${rule.value} %`
                                            : `${rule.value} ${labels.finance.currency}`}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Members card */}
            <div
                className="rounded-xl border overflow-hidden"
                style={{background: '#FFFFFF', borderColor: '#E4E4E7', borderRadius: '12px'}}
            >
                <div className="px-6 py-4">
                    <h2 className="font-bold" style={{fontSize: '16px', color: '#18181B'}}>
                        {labels.sections.membershipFeeGroupMembers}
                    </h2>
                </div>
                <hr style={{borderColor: '#E4E4E7'}}/>

                {members.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-12 gap-3">
                        <Users className="w-7 h-7" style={{color: '#A1A1AA'}}/>
                        <p className="text-sm" style={{color: '#71717A'}}>Žádní členové ve skupině.</p>
                    </div>
                ) : (
                    <table className="w-full text-sm">
                        <thead>
                            <tr style={{background: '#F8FAFC', height: '44px'}}>
                                <th className="px-5 text-left text-xs font-semibold" style={{color: '#71717A'}}>
                                    {labels.fields.memberId}
                                </th>
                                <th className="px-5 text-left text-xs font-semibold" style={{color: '#71717A'}}>
                                    {labels.tables.joinedAt}
                                </th>
                                <th className="px-5 text-left text-xs font-semibold" style={{color: '#71717A'}}>
                                    {labels.fields.source}
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {members.map((member) => (
                                <tr key={member.memberId} className="border-t" style={{borderColor: '#F4F4F5', height: '60px'}}>
                                    <td className="px-5 font-medium" style={{color: '#18181B'}}>{member.memberId}</td>
                                    <td className="px-5" style={{color: '#18181B'}}>{formatDate(member.joinedAt)}</td>
                                    <td className="px-5">
                                        <MemberSourceBadge source={member.source}/>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Assign member modal */}
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
