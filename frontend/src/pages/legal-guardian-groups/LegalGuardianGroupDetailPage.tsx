import {type ReactElement, useState} from 'react';
import {Link} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Button, Card, DetailRow, Skeleton} from '../../components/UI';
import {HalFormModal} from '../../components/HalNavigator2/HalFormModal.tsx';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../../components/members/MemberNameWithRegNumber.tsx';
import {GroupMembersTable} from '../../components/groups/GroupMembersTable.tsx';
import type {GetLegalGuardianGroupResource, HalResourceLinks, Link as HalLink} from '../../api';
import {labels} from '../../localization';
import {Pencil} from 'lucide-react';

const LegalGuardianGroupDetailContent = ({resourceData}: {resourceData: GetLegalGuardianGroupResource}): ReactElement => {
    const {route} = useHalPageData<GetLegalGuardianGroupResource>();
    const [editGuardiansOpen, setEditGuardiansOpen] = useState(false);

    const setGuardiansTemplate = resourceData._templates?.setLegalGuardianGroupGuardians ?? null;
    const guardians = resourceData.guardians ?? [];
    const minors = resourceData.minors ?? [];

    const guardiansFormValues = {
        legalGuardians: guardians.map(guardian => ({userId: guardian.userId})),
    };

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/legal-guardian-groups" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>
                {setGuardiansTemplate && (
                    <Button
                        variant="primary"
                        onClick={() => setEditGuardiansOpen(true)}
                        startIcon={<Pencil className="w-4 h-4"/>}
                    >
                        {labels.templates.setLegalGuardianGroupGuardians}
                    </Button>
                )}
            </div>

            <hr className="border-border"/>

            <Card className="p-6">
                <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">
                    {labels.sections.legalGuardians}
                </h3>
                <dl>
                    {guardians.map(guardian => {
                        const memberLink = guardian._links?.member as HalLink | undefined;
                        return (
                            <DetailRow key={guardian.userId} label="">
                                {memberLink ? (
                                    <HalRouteProvider routeLink={memberLink}>
                                        <button
                                            type="button"
                                            className="text-left hover:underline"
                                            onClick={() => route.navigateToResource(memberLink)}
                                        >
                                            <MemberNameWithRegNumber/>
                                        </button>
                                    </HalRouteProvider>
                                ) : (
                                    <span className="font-mono text-text-primary break-all">{guardian.userId}</span>
                                )}
                            </DetailRow>
                        );
                    })}
                </dl>
            </Card>

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.legalGuardianGroupMinors}</h2>
                <GroupMembersTable
                    emptyMessage={labels.ui.noMinorsInGroup}
                    members={minors.map(minor => ({
                        memberId: minor.memberId ?? '',
                        joinedAt: minor.joinedAt ?? '',
                        memberLink: minor._links?.member as HalResourceLinks | undefined,
                    }))}
                />
            </div>

            {setGuardiansTemplate && editGuardiansOpen && (
                <HalFormModal
                    title={labels.templates.setLegalGuardianGroupGuardians}
                    template={setGuardiansTemplate}
                    templateName="setLegalGuardianGroupGuardians"
                    resourceData={guardiansFormValues}
                    pathname={route.pathname}
                    onClose={() => {
                        setEditGuardiansOpen(false);
                        void route.refetch();
                    }}
                    successMessage={labels.ui.savedSuccessfully}
                />
            )}
        </div>
    );
};

export const LegalGuardianGroupDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<GetLegalGuardianGroupResource>();

    if (isLoading || (!error && !resourceData)) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return <LegalGuardianGroupDetailContent resourceData={resourceData!}/>;
};
