import type {ReactElement} from 'react';
import {useNavigate} from 'react-router-dom';
import {Pencil, UserPlus} from 'lucide-react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Skeleton} from '../../components/UI';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import type {GetLegalGuardianResource} from '../../api';
import {selfHref, toHref} from '../../api/hateoas.ts';
import {toRoutePath} from '../../api/apiPath.ts';
import type {TakeOverState} from '../members/takeOverState.ts';
import {describeError, labels} from '../../localization';

const LegalGuardianDetailContent = ({resourceData}: {resourceData: GetLegalGuardianResource}): ReactElement => {
    const navigate = useNavigate();
    const registerAsMember = resourceData._links?.registerAsMember;
    const selfLink = selfHref(resourceData);

    const registerAsMemberClick = () => {
        if (!registerAsMember || !selfLink) return;
        const state: TakeOverState = {takeOverLegalGuardianId: resourceData.userId, returnTo: toRoutePath(selfLink)};
        navigate(toRoutePath(toHref(registerAsMember)), {state});
    };

    return (
        <div className="flex flex-col gap-8">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <h1 className="text-3xl font-bold text-text-primary">
                    {resourceData.firstName} {resourceData.lastName}
                </h1>
                <div className="flex gap-2">
                    {registerAsMember && (
                        <Button variant="secondary" startIcon={<UserPlus className="w-4 h-4"/>} onClick={registerAsMemberClick}>
                            {labels.buttons.registerAsMember}
                        </Button>
                    )}
                    <HalFormButton name="updateLegalGuardian" icon={<Pencil className="w-4 h-4"/>}/>
                </div>
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
        return <Alert severity="error">{describeError(error)}</Alert>;
    }

    if (isLoading || !resourceData) {
        return <Skeleton/>;
    }

    return <LegalGuardianDetailContent resourceData={resourceData}/>;
};
