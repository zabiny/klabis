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
            <h2 className="text-xl font-bold text-text-primary">{labels.sections.pastCampaigns}</h2>

            {campaigns.length === 0 ? (
                <p className="text-sm text-text-secondary">Žádné minulé kampaně.</p>
            ) : (
                <div className="bg-surface border border-border-subtle rounded-xl overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="bg-bg-subtle border-b border-border-subtle" style={{height: '44px'}}>
                                <th className="px-5 text-left text-xs font-semibold text-text-secondary">Rok</th>
                                <th className="px-5 text-left text-xs font-semibold text-text-secondary">{labels.fields.votingDeadline}</th>
                                <th className="px-5 w-12"></th>
                            </tr>
                        </thead>
                        <tbody>
                            {campaigns.map((campaign) => (
                                <tr
                                    key={campaign.id}
                                    className="border-b border-border-subtle last:border-0 hover:bg-bg-subtle"
                                    style={{height: '60px'}}
                                >
                                    <td className="px-5 text-text-primary font-medium">
                                        <Link
                                            to={extractNavigationPath(campaign._links.self.href)}
                                            className="hover:text-primary"
                                        >
                                            {campaign.year}
                                        </Link>
                                    </td>
                                    <td className="px-5 text-text-primary">
                                        {campaign.votingDeadline ? formatDate(campaign.votingDeadline) : '—'}
                                    </td>
                                    <td className="px-5">
                                        <Link
                                            to={extractNavigationPath(campaign._links.self.href)}
                                            className="flex items-center justify-center w-8 h-8 rounded-[6px] bg-bg-subtle text-text-secondary hover:bg-bg-subtle"
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
