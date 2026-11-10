import type {ReactElement} from 'react';
import {Pencil} from 'lucide-react';
import {Button, Card, DetailRow} from '../UI';
import {type TemplateResource, useTemplateDialog} from '../HalNavigator2/useTemplateDialog.tsx';
import {useLegalGuardians} from '../../hooks/useLegalGuardians.ts';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import type {HalResourceLinks, Link} from '../../api';
import {getTemplateLabel, labels} from '../../localization';

interface LegalGuardiansSectionProps {
    guardiansLink?: HalResourceLinks;
    /** Page resource holding the guardians-replacing template; the edit action is offered only when it has it. */
    resource?: TemplateResource | null;
    editTemplateName?: string;
    onEditStarted?: () => void;
}

export const LegalGuardiansSection = ({
                                          guardiansLink,
                                          resource,
                                          editTemplateName = '',
                                          onEditStarted,
                                      }: LegalGuardiansSectionProps): ReactElement => {
    const {guardians, isLoading} = useLegalGuardians(guardiansLink);
    const {route} = useHalPageData();
    const editLabel = getTemplateLabel(editTemplateName) ?? '';
    const editDialog = useTemplateDialog(editTemplateName, {
        resource,
        initialData: {legalGuardians: guardians.map(guardian => ({userId: guardian.userId}))},
        title: editLabel,
        successMessage: labels.ui.savedSuccessfully,
    });

    return (
        <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-xs uppercase font-semibold text-text-secondary">
                    {labels.sections.legalGuardians}
                </h3>
                {editDialog.available && (
                    <Button
                        variant="secondary"
                        disabled={isLoading}
                        onClick={() => {
                            onEditStarted?.();
                            editDialog.open();
                        }}
                        startIcon={<Pencil className="w-4 h-4"/>}
                    >
                        {editLabel}
                    </Button>
                )}
            </div>
            {guardians.length === 0 ? (
                <p className="text-sm text-text-secondary">{labels.ui.noLegalGuardians}</p>
            ) : (
                <dl>
                    {guardians.map(guardian => {
                        const link = (guardian._links?.member ?? guardian._links?.legalGuardian) as Link | undefined;
                        const name = `${guardian.firstName ?? ''} ${guardian.lastName ?? ''}`.trim() || guardian.userId;
                        return (
                            <DetailRow key={guardian.userId} label="">
                                <div className="flex flex-col">
                                    {link ? (
                                        <button type="button" className="text-left hover:underline text-text-primary"
                                                onClick={() => route.navigateToResource(link)}>
                                            {name}
                                        </button>
                                    ) : (
                                        <span className="text-text-primary">{name}</span>
                                    )}
                                    {guardian.email && <span className="text-sm text-text-secondary">{guardian.email}</span>}
                                    {guardian.phone && <span className="text-sm text-text-secondary">{guardian.phone}</span>}
                                </div>
                            </DetailRow>
                        );
                    })}
                </dl>
            )}
            {editDialog.dialog}
        </Card>
    );
};
