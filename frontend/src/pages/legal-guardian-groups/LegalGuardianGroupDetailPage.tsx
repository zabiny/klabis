import {type ReactElement, useEffect, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {LegalGuardiansSection} from '../../components/legal-guardians/LegalGuardiansSection.tsx';
import {GroupMembersTable} from '../../components/groups/GroupMembersTable.tsx';
import type {GetLegalGuardianGroupResource, HalResourceLinks} from '../../api';
import {labels} from '../../localization';
import {FetchError} from '../../api/authorizedFetch.ts';
import {useToast} from '../../contexts/toastContext.ts';

const LegalGuardianGroupDetailContent = ({resourceData, onEditStarted}: {
    resourceData: GetLegalGuardianGroupResource;
    onEditStarted: () => void;
}): ReactElement => {
    const minors = resourceData.minors ?? [];

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/legal-guardian-groups" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <h1 className="text-3xl font-bold text-text-primary">{resourceData.name}</h1>

            <hr className="border-border"/>

            <LegalGuardiansSection
                guardiansLink={resourceData._links?.legalGuardians}
                editTemplate={resourceData._templates?.setLegalGuardianGroupGuardians}
                editTemplateName="setLegalGuardianGroupGuardians"
                onEditStarted={onEditStarted}
            />

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.legalGuardianGroupMinors}</h2>
                <GroupMembersTable
                    emptyMessage={labels.ui.noMinorsInGroup}
                    linkMembers
                    members={minors.map(minor => ({
                        memberId: minor.memberId ?? '',
                        memberName: `${minor.firstName ?? ''} ${minor.lastName ?? ''}${minor.registrationNumber ? ` (${minor.registrationNumber})` : ''}`.trim(),
                        joinedAt: minor.joinedAt ?? '',
                        memberLink: minor._links?.member as HalResourceLinks | undefined,
                    }))}
                />
            </div>
        </div>
    );
};

export const LegalGuardianGroupDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<GetLegalGuardianGroupResource>();
    const navigate = useNavigate();
    const {addToast} = useToast();
    const [guardiansEdited, setGuardiansEdited] = useState(false);

    // Changing guardians may merge this group into an existing one, which deletes it: the refetch then answers 404.
    const groupGone = guardiansEdited && error instanceof FetchError && error.responseStatus === 404;

    useEffect(() => {
        if (!groupGone) return;
        addToast(labels.ui.legalGuardianGroupMerged, 'info');
        navigate('/legal-guardian-groups', {replace: true});
    }, [groupGone, addToast, navigate]);

    if (groupGone) {
        return <Skeleton/>;
    }

    if (isLoading || (!error && !resourceData)) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return <LegalGuardianGroupDetailContent resourceData={resourceData!} onEditStarted={() => setGuardiansEdited(true)}/>;
};
