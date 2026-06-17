import {type ReactElement, useState} from 'react';
import {Calendar, Lock} from 'lucide-react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {HalFormModal} from '../HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {labels} from '../../localization';
import {formatDate} from '../../utils/dateUtils.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {FeeGroupsTable} from './FeeGroupsTable.tsx';
import type {FeeGroupSummary} from './FeeGroupsTable.tsx';

export interface FeeSelectionCampaignDetail extends HalResponse {
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

export const CampaignDetail = (): ReactElement | null => {
    const {resourceData, route} = useHalPageData<FeeSelectionCampaignDetail>();
    const [changeDeadlineOpen, setChangeDeadlineOpen] = useState(false);
    const [closeCampaignOpen, setCloseCampaignOpen] = useState(false);

    const levelsHref = resourceData?._links?.levels?.href ?? '';
    const {data: groupsData} = useAuthorizedQuery<FeeGroupsCollection>(
        levelsHref ? extractNavigationPath(levelsHref) : '',
        {enabled: !!levelsHref}
    );
    const groups = groupsData?._embedded?.membershipFeeGroupResponseList ?? [];

    const changeDeadlineTemplate = resourceData?._templates?.changeDeadline ?? null;
    const closeCampaignTemplate = resourceData?._templates?.closeCampaign ?? null;

    if (!resourceData) {
        return null;
    }

    return (
        <div className="flex flex-col gap-6">
            <div className="bg-white border border-[#E4E4E7] rounded-xl p-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                        <p className="text-xs font-semibold text-[#71717A] uppercase tracking-wide">{labels.fields.year}</p>
                        <p className="mt-1 text-[#18181B] font-medium">{resourceData?.year}</p>
                    </div>
                    <div>
                        <p className="text-xs font-semibold text-[#71717A] uppercase tracking-wide">{labels.fields.votingDeadline}</p>
                        <p className="mt-1 text-[#18181B] font-medium">
                            {resourceData?.votingDeadline ? formatDate(resourceData.votingDeadline) : '—'}
                        </p>
                    </div>
                </div>

                {(changeDeadlineTemplate || closeCampaignTemplate) && (
                    <div className="mt-5 pt-5 border-t border-[#E4E4E7] flex flex-wrap gap-2">
                        {changeDeadlineTemplate && (
                            <button
                                type="button"
                                onClick={() => setChangeDeadlineOpen(true)}
                                className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-zinc-700 bg-white border border-zinc-200 hover:bg-zinc-50 transition-colors"
                                aria-label={labels.templates.changeDeadline}
                            >
                                <Calendar className="w-[15px] h-[15px]"/>
                                {labels.templates.changeDeadline}
                            </button>
                        )}
                        {closeCampaignTemplate && (
                            <button
                                type="button"
                                onClick={() => setCloseCampaignOpen(true)}
                                className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-white bg-red-600 border border-red-600 hover:bg-red-700 transition-colors"
                                aria-label={labels.templates.closeCampaign}
                            >
                                <Lock className="w-[15px] h-[15px]"/>
                                {labels.templates.closeCampaign}
                            </button>
                        )}
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
                    resourceData={resourceData as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => setChangeDeadlineOpen(false)}
                    navigateOnSuccess={false}
                />
            )}

            {closeCampaignTemplate && closeCampaignOpen && (
                <HalFormModal
                    title={labels.templates.closeCampaign}
                    template={closeCampaignTemplate as HalFormsTemplate}
                    templateName="closeCampaign"
                    resourceData={resourceData as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => setCloseCampaignOpen(false)}
                    navigateOnSuccess={false}
                />
            )}
        </div>
    );
};
