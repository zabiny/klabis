import type {ReactElement} from 'react';
import {Calendar, Lock} from 'lucide-react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useHalCollection} from '../../hooks/useHalCollection.ts';
import {useTemplateDialog} from '../HalNavigator2/useTemplateDialog.tsx';
import {halResource} from '../../api/halResource';
import type {GetPublicationResource} from '../../api';
import {labels} from '../../localization';
import {formatDate} from '../../utils/dateUtils.ts';
import {toHref} from '../../api/hateoas.ts';
import {FeeGroupsTable} from './FeeGroupsTable.tsx';
import type {FeeGroupSummary} from './FeeGroupsTable.tsx';

export type FeeSelectionCampaignDetail = GetPublicationResource;

export const CampaignDetail = (): ReactElement | null => {
    const {resourceData} = useHalPageData<FeeSelectionCampaignDetail>();
    const campaignActions = halResource(resourceData);
    const changeDeadlineAction = campaignActions.template('changeDeadline');
    const closeCampaignAction = campaignActions.template('closeCampaign');
    const changeDeadlineDialog = useTemplateDialog({
        navigateOnSuccess: false,
    });
    const closeCampaignDialog = useTemplateDialog({navigateOnSuccess: false});

    const levelsHref = resourceData?._links?.levels ? toHref(resourceData._links.levels) : undefined;
    const {items: groups} = useHalCollection<FeeGroupSummary>(levelsHref);

    if (!resourceData) {
        return null;
    }

    return (
        <div className="flex flex-col gap-6">
            <div className="bg-surface border border-border-subtle rounded-xl p-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                        <p className="text-xs font-semibold text-text-secondary uppercase tracking-wide">{labels.fields.year}</p>
                        <p className="mt-1 text-text-primary font-medium">{resourceData?.year}</p>
                    </div>
                    <div>
                        <p className="text-xs font-semibold text-text-secondary uppercase tracking-wide">{labels.fields.votingDeadline}</p>
                        <p className="mt-1 text-text-primary font-medium">
                            {resourceData?.votingDeadline ? formatDate(resourceData.votingDeadline) : '—'}
                        </p>
                    </div>
                </div>

                {(changeDeadlineAction || closeCampaignAction) && (
                    <div className="mt-5 pt-5 border-t border-border-subtle flex flex-wrap gap-2">
                        {changeDeadlineAction && (
                            <button
                                type="button"
                                onClick={() => changeDeadlineDialog.open(changeDeadlineAction)}
                                className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-text-secondary bg-surface border border-border-subtle hover:bg-bg-subtle transition-colors"
                                aria-label={labels.templates.changeDeadline}
                            >
                                <Calendar className="w-[15px] h-[15px]"/>
                                {labels.templates.changeDeadline}
                            </button>
                        )}
                        {closeCampaignAction && (
                            <button
                                type="button"
                                onClick={() => closeCampaignDialog.open(closeCampaignAction)}
                                className="inline-flex items-center gap-2 h-[38px] px-4 rounded-md text-sm font-medium text-white bg-error hover:brightness-110 transition-colors"
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
                <h3 className="text-lg font-bold text-text-primary">{labels.sections.membershipFeeGroups}</h3>
                <FeeGroupsTable groups={groups}/>
            </div>

            {changeDeadlineDialog.dialog}
            {closeCampaignDialog.dialog}
        </div>
    );
};
