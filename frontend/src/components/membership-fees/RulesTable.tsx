import type {ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useTemplateDialog} from '../HalNavigator2/useTemplateDialog.tsx';
import type {HalFormsTemplate, ListGroupRulesResource, ListRulesItemResource, ListRulesResource, OptionItem} from '../../api';
import {labels} from '../../localization';
import {Pencil, Plus, X} from 'lucide-react';
import {useEventTypes, type EventTypeCatalogItem} from '../../hooks/useEventTypes.ts';
import {CoParticipationRuleTypeBadge} from './CoParticipationRuleTypeBadge.tsx';

type PaymentRuleItem = NonNullable<NonNullable<RulesCollection['_embedded']>['paymentRuleResponseList']>[number] | ListRulesItemResource;

type RulesCollection = ListRulesResource | ListGroupRulesResource;

const RuleRow = ({rule, getRankingLabel, getEventTypeById, onEdit, onRemove}: {
    rule: PaymentRuleItem;
    getRankingLabel: (code: string) => string;
    getEventTypeById: (id: string | null | undefined) => EventTypeCatalogItem | undefined;
    onEdit?: () => void;
    onRemove?: () => void;
}): ReactElement => {
    return (
        <tr className="border-t border-border-subtle" style={{height: '52px'}}>
            <td className="px-5 text-text-primary">{getEventTypeById(rule.eventTypeId)?.name ?? rule.eventTypeId}</td>
            <td className="px-5 text-text-primary">{getRankingLabel(rule.rankingShortName ?? '')}</td>
            <td className="px-5">
                {rule.ruleType && <CoParticipationRuleTypeBadge ruleType={rule.ruleType}/>}
            </td>
            <td className="px-5 text-text-primary">
                {rule.ruleType === 'PERCENTAGE'
                    ? `${rule.percentage} %`
                    : `${rule.fixedAmount} ${rule.fixedCurrency ?? labels.finance.currency}`}
            </td>
            <td className="px-3">
                <div className="flex items-center gap-1">
                    {onEdit && (
                        <button
                            type="button"
                            aria-label="Upravit pravidlo"
                            onClick={onEdit}
                            className="w-8 h-8 flex items-center justify-center rounded-md text-text-secondary bg-bg-subtle hover:bg-bg-subtle transition-colors"
                        >
                            <Pencil className="w-4 h-4"/>
                        </button>
                    )}
                    {onRemove && (
                        <button
                            type="button"
                            aria-label="Smazat pravidlo"
                            onClick={onRemove}
                            className="w-8 h-8 flex items-center justify-center rounded-md text-error bg-error-bg hover:opacity-80 transition-colors"
                        >
                            <X className="w-4 h-4"/>
                        </button>
                    )}
                </div>
            </td>
        </tr>
    );
};

const buildRankingLabelLookup = (addRuleTemplate: HalFormsTemplate | null): Map<string, string> => {
    const rankingProperty = addRuleTemplate?.properties.find(p => p.name === 'rankingShortName');
    const options = rankingProperty?.options?.inline ?? [];
    return new Map(
        (options as OptionItem[])
            .filter((o): o is OptionItem & {prompt: string} => typeof o === 'object' && o.prompt !== undefined)
            .map(o => [String(o.value), o.prompt])
    );
};

export const RulesTable = (): ReactElement => {
    const {resourceData} = useHalPageData<RulesCollection>();
    const {getById: getEventTypeById} = useEventTypes();

    const rules = resourceData?._embedded?.paymentRuleResponseList ?? [];
    const addRuleTemplate = (resourceData?._templates?.addRule ?? null) as HalFormsTemplate | null;
    const rankingLabelLookup = buildRankingLabelLookup(addRuleTemplate);
    const getRankingLabel = (code: string): string => rankingLabelLookup.get(code) ?? code;
    const addRuleDialog = useTemplateDialog('addRule', {
        resource: resourceData,
        title: (template) => template.title ?? 'Přidat pravidlo',
        navigateOnSuccess: false,
    });
    const editRuleDialog = useTemplateDialog('editRule', {
        title: 'Upravit pravidlo',
        navigateOnSuccess: false,
    });
    const removeRuleDialog = useTemplateDialog('removeRule', {
        title: 'Smazat pravidlo',
        navigateOnSuccess: false,
    });

    return (
        <div className="bg-surface rounded-xl border border-border-subtle overflow-hidden">
            <div className="px-6 py-4">
                <h2 className="text-base font-bold text-text-primary">{labels.sections.coParticipationRules}</h2>
                <p className="text-[13px] text-text-secondary mt-0.5">Příplatky ke startovnému na závodech</p>
            </div>
            <hr className="border-border-subtle"/>

            <table className="w-full text-sm">
                <thead>
                    <tr className="bg-bg-subtle" style={{height: '44px'}}>
                        <th className="px-5 text-left font-semibold text-text-secondary text-xs">{labels.fields.raceTypeId}</th>
                        <th className="px-5 text-left font-semibold text-text-secondary text-xs w-[100px]">{labels.fields.ranking}</th>
                        <th className="px-5 text-left font-semibold text-text-secondary text-xs w-[160px]">{labels.fields.ruleType}</th>
                        <th className="px-5 text-left font-semibold text-text-secondary text-xs w-[100px]">{labels.tables.value}</th>
                        <th className="px-5 w-[40px]"/>
                    </tr>
                </thead>
                <tbody>
                    {rules.length === 0 ? (
                        <tr>
                            <td colSpan={5} className="px-5 py-8 text-center text-sm text-text-secondary">
                                Žádná pravidla spoluúčasti
                            </td>
                        </tr>
                    ) : (
                        rules.map((rule) => (
                            <RuleRow
                                key={`${rule.eventTypeId}-${rule.rankingShortName}`}
                                rule={rule}
                                getRankingLabel={getRankingLabel}
                                getEventTypeById={getEventTypeById}
                                onEdit={editRuleDialog.availableOn(rule) ? () => editRuleDialog.open(rule) : undefined}
                                onRemove={removeRuleDialog.availableOn(rule) ? () => removeRuleDialog.open(rule) : undefined}
                            />
                        ))
                    )}
                </tbody>
            </table>

            {addRuleDialog.available && (
                <>
                    <div className="border-t border-border-subtle"/>
                    <button
                        type="button"
                        onClick={() => addRuleDialog.open()}
                        className="w-full flex items-center gap-2 px-5 bg-bg-subtle hover:bg-bg-subtle transition-colors text-primary font-bold text-sm"
                        style={{height: '48px'}}
                    >
                        <Plus className="w-4 h-4 text-primary"/>
                        Přidat pravidlo…
                    </button>
                </>
            )}
            {addRuleDialog.dialog}
            {editRuleDialog.dialog}
            {removeRuleDialog.dialog}
        </div>
    );
};
