import {type ReactElement} from 'react';
import {Link} from 'react-router-dom';
import {ArrowRight} from 'lucide-react';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {formatDate} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';

interface CampaignSummary {
    id: string;
    year: number;
    votingDeadline: string;
    _links: {
        self: {href: string};
    };
}

interface CampaignsCollection {
    _embedded?: {
        feeSelectionCampaignResponseList?: CampaignSummary[];
    };
}

interface PastCampaignsSectionProps {
    pastCampaignsHref: string | null;
}

export const PastCampaignsSection = ({pastCampaignsHref}: PastCampaignsSectionProps): ReactElement | null => {
    const {data: campaignsData, isLoading} = useAuthorizedQuery<CampaignsCollection>(
        pastCampaignsHref ? extractNavigationPath(pastCampaignsHref) : '',
        {enabled: !!pastCampaignsHref}
    );

    if (!pastCampaignsHref) {
        return null;
    }

    if (isLoading) {
        return null;
    }

    const campaigns = campaignsData?._embedded?.feeSelectionCampaignResponseList ?? [];

    return (
        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-[#18181B]">{labels.sections.pastCampaigns}</h2>

            {campaigns.length === 0 ? (
                <p className="text-sm text-[#71717A]">Žádné minulé kampaně.</p>
            ) : (
                <div className="bg-white border border-[#E4E4E7] rounded-xl overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="bg-[#F8FAFC] border-b border-[#E4E4E7]" style={{height: '44px'}}>
                                <th className="px-5 text-left text-xs font-semibold text-[#71717A]">Rok</th>
                                <th className="px-5 text-left text-xs font-semibold text-[#71717A]">{labels.fields.votingDeadline}</th>
                                <th className="px-5 w-12"></th>
                            </tr>
                        </thead>
                        <tbody>
                            {campaigns.map((campaign) => (
                                <tr
                                    key={campaign.id}
                                    className="border-b border-[#E4E4E7] last:border-0 hover:bg-[#F8FAFC]"
                                    style={{height: '60px'}}
                                >
                                    <td className="px-5 text-[#18181B] font-medium">
                                        <Link
                                            to={extractNavigationPath(campaign._links.self.href)}
                                            className="hover:text-[#2563EB]"
                                        >
                                            {campaign.year}
                                        </Link>
                                    </td>
                                    <td className="px-5 text-[#18181B]">
                                        {campaign.votingDeadline ? formatDate(campaign.votingDeadline) : '—'}
                                    </td>
                                    <td className="px-5">
                                        <Link
                                            to={extractNavigationPath(campaign._links.self.href)}
                                            className="flex items-center justify-center w-8 h-8 rounded-[6px] bg-[#F8FAFC] text-[#71717A] hover:bg-[#F1F5F9]"
                                            aria-label="Otevřít detail kampaně"
                                        >
                                            <ArrowRight size={16}/>
                                        </Link>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};
