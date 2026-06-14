import {type ReactElement, useState} from 'react';
import {Calendar} from 'lucide-react';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {HalFormModal} from '../HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {labels} from '../../localization';
import {formatDate} from '../../utils/dateUtils.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {FeeGroupsTable} from './FeeGroupsTable.tsx';
import type {FeeGroupSummary} from './FeeGroupsTable.tsx';

interface FeeSelectionCampaignDetail extends HalResponse {
    id: string;
    year: number;
    votingDeadline: string;
    _links: {
        self: {href: string};
        levels?: {href: string};
    };
}

interface FeeGroupsCollection {
    _embedded?: {
        membershipFeeGroupResponseList?: FeeGroupSummary[];
    };
}

interface ActiveCampaignSectionProps {
    activeCampaignHref: string | null;
}

export const ActiveCampaignSection = ({activeCampaignHref}: ActiveCampaignSectionProps): ReactElement | null => {
    const [changeDeadlineOpen, setChangeDeadlineOpen] = useState(false);

    const {data: campaignData} = useAuthorizedQuery<FeeSelectionCampaignDetail>(
        activeCampaignHref ? extractNavigationPath(activeCampaignHref) : '',
        {enabled: !!activeCampaignHref}
    );

    const levelsHref = campaignData?._links?.levels?.href ?? '';
    const {data: groupsData} = useAuthorizedQuery<FeeGroupsCollection>(
        levelsHref ? extractNavigationPath(levelsHref) : '',
        {enabled: !!levelsHref}
    );

    if (!activeCampaignHref) {
        return null;
    }

    if (!campaignData) {
        return null;
    }

    const groups = groupsData?._embedded?.membershipFeeGroupResponseList ?? [];
    const changeDeadlineTemplate = campaignData._templates?.changeDeadline ?? null;

    return (
        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-[#18181B]">{labels.sections.activeCampaign}</h2>

            <div className="bg-white border border-[#E4E4E7] rounded-xl p-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                        <p className="text-xs font-semibold text-[#71717A] uppercase tracking-wide">Rok</p>
                        <p className="mt-1 text-[#18181B] font-medium">{campaignData.year}</p>
                    </div>
                    <div>
                        <p className="text-xs font-semibold text-[#71717A] uppercase tracking-wide">{labels.fields.votingDeadline}</p>
                        <p className="mt-1 text-[#18181B] font-medium">
                            {campaignData.votingDeadline ? formatDate(campaignData.votingDeadline) : '—'}
                        </p>
                    </div>
                </div>

                {changeDeadlineTemplate && (
                    <div className="mt-5 pt-5 border-t border-[#E4E4E7]">
                        <button
                            type="button"
                            onClick={() => setChangeDeadlineOpen(true)}
                            className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-zinc-700 bg-white border border-zinc-200 hover:bg-zinc-50 transition-colors"
                            aria-label={labels.templates.changeDeadline}
                        >
                            <Calendar className="w-[15px] h-[15px]"/>
                            {labels.templates.changeDeadline}
                        </button>
                    </div>
                )}
            </div>

            <div className="flex flex-col gap-4">
                <h3 className="text-lg font-bold text-[#18181B]">{labels.sections.membershipFeeGroups}</h3>
                <FeeGroupsTable groups={groups}/>
            </div>

            {changeDeadlineTemplate && changeDeadlineOpen && (
                <HalFormModal
                    title={labels.templates.changeDeadline}
                    template={changeDeadlineTemplate as HalFormsTemplate}
                    templateName="changeDeadline"
                    resourceData={campaignData as unknown as Record<string, unknown>}
                    pathname={extractNavigationPath(campaignData._links?.self?.href ?? '')}
                    onClose={() => setChangeDeadlineOpen(false)}
                    navigateOnSuccess={false}
                />
            )}
        </div>
    );
};
