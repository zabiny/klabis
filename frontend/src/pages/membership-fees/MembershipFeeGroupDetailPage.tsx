import {type ReactElement, useState} from 'react';
import {Link} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import type {GetFeeGroupResource} from '../../api';
import {getEnumLabel, labels} from '../../localization';
import {ChevronRight, Pencil, UserPlus, Users} from 'lucide-react';
import {formatDate} from '../../utils/dateUtils.ts';
import {HalSubresourceProvider} from '../../contexts/HalRouteContext.tsx';
import {RulesTable} from '../../components/membership-fees/RulesTable.tsx';

type MembershipFeeGroupDetail = GetFeeGroupResource;

const MemberSourceBadge = ({source}: {source: 'MEMBER_CHOICE' | 'ADMIN_ASSIGNMENT'}): ReactElement => {
    if (source === 'MEMBER_CHOICE') {
        return (
            <span
                className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                style={{background: 'var(--color-success-bg)', color: 'var(--color-success)'}}
            >
                {getEnumLabel('feeMemberSource', source)}
            </span>
        );
    }
    return (
        <span
            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
            style={{background: 'var(--color-primary-subtle)', color: 'var(--color-primary)'}}
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
                style={{background: 'var(--color-success-bg)', color: 'var(--color-success)'}}
            >
                {getEnumLabel('feeGroupStatus', status)}
            </span>
        );
    }
    return (
        <span
            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
            style={{background: 'var(--color-secondary-subtle)', color: 'var(--color-text-secondary)'}}
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
    const members = resourceData._embedded?.members ?? [];
    const canEdit = resourceData.status === 'EDITABLE' && !!editTemplate;

    return (
        <div className="flex flex-col gap-6">
            <nav className="flex items-center gap-1 text-sm" style={{color: 'var(--color-text-secondary)'}}>
                <Link to="/membership-fee-tiers" className="hover:text-text-primary transition-colors">
                    {labels.ui.backToList}
                </Link>
                <ChevronRight className="w-4 h-4" style={{color: 'var(--color-text-tertiary)'}}/>
                <span className="font-medium" style={{color: 'var(--color-text-primary)'}}>{resourceData.name}</span>
            </nav>

            <h1 className="sr-only">{resourceData.name}</h1>

            <div
                className="rounded-xl border p-6"
                style={{background: 'var(--color-bg-elevated)', borderColor: 'var(--color-border-subtle)', borderRadius: '12px'}}
            >
                <div className="flex items-center justify-between mb-4">
                    <h2 className="font-bold" style={{fontSize: '16px', color: 'var(--color-text-primary)'}}>
                        Základní informace
                    </h2>
                    <div className="flex gap-3">
                        {canEdit && !isEditing && (
                            <button
                                type="button"
                                onClick={() => setIsEditing(true)}
                                className="inline-flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium border transition-colors hover:bg-bg-subtle"
                                style={{borderColor: 'var(--color-border-subtle)', color: 'var(--color-text-secondary)'}}
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
                                style={{background: 'var(--color-primary)'}}
                            >
                                <UserPlus className="w-4 h-4"/>
                                {labels.templates.assignMember}
                            </button>
                        )}
                    </div>
                </div>
                <hr style={{borderColor: 'var(--color-border-subtle)'}} className="mb-5"/>

                {isEditing && editTemplate ? (
                    <HalFormDisplay
                        template={editTemplate}
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
                            <label className="block text-xs font-bold mb-1" style={{color: 'var(--color-text-secondary)'}}>
                                {labels.fields.yearlyFeeAmount}
                            </label>
                            <div
                                className="h-[38px] flex items-center px-3 rounded-md text-sm border"
                                style={{borderColor: 'var(--color-border-subtle)', color: 'var(--color-text-primary)'}}
                            >
                                {resourceData.yearlyFeeAmount} {resourceData.yearlyFeeCurrency ?? labels.finance.currency}
                            </div>
                        </div>
                        <div>
                            <label className="block text-xs font-bold mb-1" style={{color: 'var(--color-text-secondary)'}}>
                                Status
                            </label>
                            <div className="h-[38px] flex items-center">
                                {resourceData.status && <StatusBadge status={resourceData.status}/>}
                            </div>
                        </div>
                    </div>
                )}
            </div>

            <HalSubresourceProvider subresourceLinkName="rules">
                <RulesTable/>
            </HalSubresourceProvider>

            <div
                className="rounded-xl border overflow-hidden"
                style={{background: 'var(--color-bg-elevated)', borderColor: 'var(--color-border-subtle)', borderRadius: '12px'}}
            >
                <div className="px-6 py-4">
                    <h2 className="font-bold" style={{fontSize: '16px', color: 'var(--color-text-primary)'}}>
                        {labels.sections.membershipFeeGroupMembers}
                    </h2>
                </div>
                <hr style={{borderColor: 'var(--color-border-subtle)'}}/>

                {members.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-12 gap-3">
                        <Users className="w-7 h-7" style={{color: 'var(--color-text-tertiary)'}}/>
                        <p className="text-sm" style={{color: 'var(--color-text-secondary)'}}>Žádní členové ve skupině.</p>
                    </div>
                ) : (
                    <table className="w-full text-sm">
                        <thead>
                            <tr style={{background: 'var(--color-bg-subtle)', height: '44px'}}>
                                <th className="px-5 text-left text-xs font-semibold" style={{color: 'var(--color-text-secondary)'}}>
                                    {labels.fields.memberId}
                                </th>
                                <th className="px-5 text-left text-xs font-semibold" style={{color: 'var(--color-text-secondary)'}}>
                                    {labels.fields.registrationNumber}
                                </th>
                                <th className="px-5 text-left text-xs font-semibold" style={{color: 'var(--color-text-secondary)'}}>
                                    {labels.tables.joinedAt}
                                </th>
                                <th className="px-5 text-left text-xs font-semibold" style={{color: 'var(--color-text-secondary)'}}>
                                    {labels.fields.source}
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {members.map((member) => (
                                <tr key={member.memberId} className="border-t" style={{borderColor: 'var(--color-border-subtle)', height: '60px'}}>
                                    <td className="px-5 font-medium" style={{color: 'var(--color-text-primary)'}}>
                                        {member.firstName && member.lastName
                                            ? `${member.firstName} ${member.lastName}`
                                            : member.memberId}
                                    </td>
                                    <td className="px-5" style={{color: 'var(--color-text-primary)'}}>{member.registrationNumber ?? '—'}</td>
                                    <td className="px-5" style={{color: 'var(--color-text-primary)'}}>{formatDate(member.joinedAt)}</td>
                                    <td className="px-5">
                                        {member.source && <MemberSourceBadge source={member.source}/>}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {assignMemberTemplate && assignMemberModal && (
                <HalFormModal
                    title={labels.templates.assignMember}
                    template={assignMemberTemplate}
                    templateName="assignMember"
                    resourceData={resourceData as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => { setAssignMemberModal(false); void route.refetch(); }}
                    successMessage={labels.ui.savedSuccessfully}
                    navigateOnSuccess={false}
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
