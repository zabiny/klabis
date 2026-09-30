import {type ReactElement, useState} from 'react';
import {Pencil} from 'lucide-react';
import {Button, Card, DetailRow} from '../UI';
import {HalFormModal} from '../HalNavigator2/HalFormModal.tsx';
import {useLegalGuardians} from '../../hooks/useLegalGuardians.ts';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import type {HalFormsTemplate, HalResourceLinks, Link} from '../../api';
import {getTemplateLabel, labels} from '../../localization';

interface LegalGuardiansSectionProps {
    guardiansLink?: HalResourceLinks;
    /** Template that replaces the guardians; the edit action is offered only when present. */
    editTemplate?: HalFormsTemplate | null;
    editTemplateName?: string;
    onEditStarted?: () => void;
}

export const LegalGuardiansSection = ({
                                          guardiansLink,
                                          editTemplate,
                                          editTemplateName = '',
                                          onEditStarted,
                                      }: LegalGuardiansSectionProps): ReactElement => {
    const {guardians, isLoading} = useLegalGuardians(guardiansLink);
    const {route} = useHalPageData();
    const [editOpen, setEditOpen] = useState(false);
    const editLabel = getTemplateLabel(editTemplateName) ?? '';

    return (
        <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-xs uppercase font-semibold text-text-secondary">
                    {labels.sections.legalGuardians}
                </h3>
                {editTemplate && (
                    <Button
                        variant="secondary"
                        disabled={isLoading}
                        onClick={() => {
                            onEditStarted?.();
                            setEditOpen(true);
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
            {editTemplate && editOpen && (
                <HalFormModal
                    title={editLabel}
                    template={editTemplate}
                    templateName={editTemplateName}
                    resourceData={{legalGuardians: guardians.map(guardian => ({userId: guardian.userId}))}}
                    pathname={route.pathname}
                    onClose={() => {
                        setEditOpen(false);
                        void route.refetch();
                    }}
                    successMessage={labels.ui.savedSuccessfully}
                />
            )}
        </Card>
    );
};
