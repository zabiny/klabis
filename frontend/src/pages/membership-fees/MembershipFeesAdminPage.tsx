import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {HalSubresourceProvider} from '../../contexts/HalRouteContext.tsx';
import {CampaignDetail} from '../../components/membership-fees/CampaignDetail.tsx';
import {TierCatalogSection} from '../../components/membership-fees/TierCatalogSection.tsx';
import {PastCampaignsSection} from '../../components/membership-fees/PastCampaignsSection.tsx';
import type {HalResponse} from '../../api';
import {labels} from '../../localization';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';

interface TierCatalogResource extends HalResponse {
    _links: {
        self: { href: string };
        activeCampaign?: { href: string };
        pastCampaigns?: { href: string };
    };
}

export const MembershipFeesAdminPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<TierCatalogResource>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    const activeCampaignHref = resourceData._links?.activeCampaign?.href ?? null;
    const pastCampaignsHref = resourceData._links?.pastCampaigns?.href ?? null;

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-[28px] font-bold text-[#18181B]">{labels.nav['membership-fees']}</h1>

            {activeCampaignHref ? (
                <div className="flex flex-col gap-4">
                    <h2 className="text-xl font-bold text-[#18181B]">{labels.sections.activeCampaign}</h2>
                    <HalSubresourceProvider subresourceLinkName="activeCampaign">
                        <CampaignDetail/>
                    </HalSubresourceProvider>
                </div>
            ) : (
                <HalFormButton name="publishYear"/>
            )}

            <TierCatalogSection/>

            <PastCampaignsSection pastCampaignsHref={pastCampaignsHref}/>
        </div>
    );
};
