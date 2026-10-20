import {type ReactElement, useState} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalFormModal} from '../HalNavigator2/HalFormModal.tsx';
import type {HalFormsTemplate, ListGroupRulesResource, OptionItem} from '../../api';
import type {components} from '../../api/klabisApi';
import {labels} from '../../localization';
import {Pencil, Plus, X} from 'lucide-react';
import {useEventTypes, type EventTypeCatalogItem} from '../../hooks/useEventTypes.ts';
import {CoParticipationRuleTypeBadge} from './CoParticipationRuleTypeBadge.tsx';

// PaymentRuleResponse payload + the per-row _links / _templates the backend adds at runtime
// (editRule, removeRule) which the generated schema does not describe.
type PaymentRuleItem = components['schemas']['PaymentRuleResponse'] & {
    _links?: { self?: { href: string } };
    _templates?: Record<string, HalFormsTemplate>;
};

type RulesCollection = ListGroupRulesResource;

const RuleRow = ({rule, getRankingLabel, getEventTypeById}: {
    rule: PaymentRuleItem;
    getRankingLabel: (code: string) => string;
    getEventTypeById: (id: string | null | undefined) => EventTypeCatalogItem | undefined;
}): ReactElement => {
    const [editOpen, setEditOpen] = useState(false);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const selfHref = (rule._links?.self as {href: string} | undefined)?.href ?? '';

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
                    {rule._templates?.editRule && (
                        <>
                            <button
                                type="button"
                                aria-label="Upravit pravidlo"
                                onClick={() => setEditOpen(true)}
                                className="w-8 h-8 flex items-center justify-center rounded-md text-text-secondary bg-bg-subtle hover:bg-bg-subtle transition-colors"
                            >
                                <Pencil className="w-4 h-4"/>
                            </button>
                            {editOpen && (
                                <HalFormModal
                                    title="Upravit pravidlo"
                                    template={rule._templates.editRule as HalFormsTemplate}
                                    templateName="editRule"
                                    resourceData={rule as unknown as Record<string, unknown>}
                                    pathname={selfHref}
                                    resourceUrl={selfHref}
                                    onClose={() => setEditOpen(false)}
                                    navigateOnSuccess={false}
                                />
                            )}
                        </>
                    )}
                    {rule._templates?.removeRule && (
                        <>
                            <button
                                type="button"
                                aria-label="Smazat pravidlo"
                                onClick={() => setDeleteOpen(true)}
                                className="w-8 h-8 flex items-center justify-center rounded-md text-error bg-error-bg hover:opacity-80 transition-colors"
                            >
                                <X className="w-4 h-4"/>
                            </button>
                            {deleteOpen && (
                                <HalFormModal
                                    title="Smazat pravidlo"
                                    template={rule._templates.removeRule as HalFormsTemplate}
                                    templateName="removeRule"
                                    resourceData={rule as unknown as Record<string, unknown>}
                                    pathname={selfHref}
                                    resourceUrl={selfHref}
                                    onClose={() => setDeleteOpen(false)}
                                    navigateOnSuccess={false}
                                />
                            )}
                        </>
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
    const [addRuleModal, setAddRuleModal] = useState(false);

    const rules = resourceData?._embedded?.paymentRuleResponseList ?? [];
    const addRuleTemplate = (resourceData?._templates?.addRule ?? null) as HalFormsTemplate | null;
    const rankingLabelLookup = buildRankingLabelLookup(addRuleTemplate);
    const getRankingLabel = (code: string): string => rankingLabelLookup.get(code) ?? code;

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
                            />
                        ))
                    )}
                </tbody>
            </table>

            {addRuleTemplate && (
                <>
                    <div className="border-t border-border-subtle"/>
                    <button
                        type="button"
                        onClick={() => setAddRuleModal(true)}
                        className="w-full flex items-center gap-2 px-5 bg-bg-subtle hover:bg-bg-subtle transition-colors text-primary font-bold text-sm"
                        style={{height: '48px'}}
                    >
                        <Plus className="w-4 h-4 text-primary"/>
                        Přidat pravidlo…
                    </button>
                    {addRuleModal && (
                        <HalFormModal
                            title={addRuleTemplate.title ?? 'Přidat pravidlo'}
                            template={addRuleTemplate}
                            templateName="addRule"
                            resourceData={resourceData as unknown as Record<string, unknown>}
                            pathname={(resourceData?._links?.self as {href: string} | undefined)?.href ?? ''}
                            onClose={() => setAddRuleModal(false)}
                            navigateOnSuccess={false}
                        />
                    )}
                </>
            )}
        </div>
    );
};
