import {type ReactElement, useState} from 'react';
import {Pencil} from 'lucide-react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Skeleton} from '../../components/UI';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import type {GetLegalGuardianResource} from '../../api';
import {labels} from '../../localization';

const LegalGuardianDetailContent = ({resourceData}: {resourceData: GetLegalGuardianResource}): ReactElement => {
    const {route} = useHalPageData<GetLegalGuardianResource>();
    const [editOpen, setEditOpen] = useState(false);

    const updateTemplate = resourceData._templates?.updateLegalGuardian ?? null;
    const formValues = {
        firstName: resourceData.firstName,
        lastName: resourceData.lastName,
        email: resourceData.email,
        phone: resourceData.phone,
    };

    return (
        <div className="flex flex-col gap-8">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <h1 className="text-3xl font-bold text-text-primary">
                    {resourceData.firstName} {resourceData.lastName}
                </h1>
                {updateTemplate && (
                    <Button
                        variant="primary"
                        onClick={() => setEditOpen(true)}
                        startIcon={<Pencil className="w-4 h-4"/>}
                    >
                        {labels.templates.updateLegalGuardian}
                    </Button>
                )}
            </div>

            <hr className="border-border"/>

            <Card className="p-6">
                <dl>
                    <DetailRow label={labels.fields.loginName}>
                        <span className="font-mono">{resourceData.loginName}</span>
                    </DetailRow>
                    <DetailRow label={labels.fields.email}>{resourceData.email}</DetailRow>
                    <DetailRow label={labels.fields.phone}>{resourceData.phone}</DetailRow>
                </dl>
            </Card>

            {updateTemplate && editOpen && (
                <HalFormModal
                    title={labels.templates.updateLegalGuardian}
                    template={updateTemplate}
                    templateName="updateLegalGuardian"
                    resourceData={formValues}
                    pathname={route.pathname}
                    onClose={() => {
                        setEditOpen(false);
                        void route.refetch();
                    }}
                    successMessage={labels.ui.savedSuccessfully}
                />
            )}
        </div>
    );
};

export const LegalGuardianDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<GetLegalGuardianResource>();

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (isLoading || !resourceData) {
        return <Skeleton/>;
    }

    return <LegalGuardianDetailContent resourceData={resourceData}/>;
};
