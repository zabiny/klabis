import type {ReactElement} from 'react';
import {Pencil} from 'lucide-react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Card, DetailRow, Skeleton} from '../../components/UI';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import type {GetLegalGuardianResource} from '../../api';
import {labels} from '../../localization';

const LegalGuardianDetailContent = ({resourceData}: {resourceData: GetLegalGuardianResource}): ReactElement => {
    return (
        <div className="flex flex-col gap-8">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <h1 className="text-3xl font-bold text-text-primary">
                    {resourceData.firstName} {resourceData.lastName}
                </h1>
                <HalFormButton name="updateLegalGuardian" icon={<Pencil className="w-4 h-4"/>}/>
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
