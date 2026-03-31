import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {TableCell} from '../../components/KlabisTable';
import {Alert, Button, Card, Skeleton} from '../../components/UI';
import type {EntityModel, HalResponse} from '../../api';
import {labels} from '../../localization';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {useQueryClient} from '@tanstack/react-query';
import {toHref} from '../../api/hateoas.ts';
import {Check, X} from 'lucide-react';
import type {PendingInvitation} from './types.ts';

type GroupSummary = EntityModel<{
    id: string;
    name: string;
}>;

interface PendingInvitationsData extends HalResponse {
    _embedded?: {
        pendingInvitationResponseList?: PendingInvitation[];
    };
}

const PendingInvitationsSection = (): ReactElement | null => {
    const queryClient = useQueryClient();
    const {route} = useHalPageData();

    const {data} = useAuthorizedQuery<PendingInvitationsData>('/api/invitations/pending');

    const {mutate} = useAuthorizedMutation({
        method: 'POST',
    });

    const invitations = data?._embedded?.pendingInvitationResponseList ?? [];

    if (invitations.length === 0) {
        return null;
    }

    const handleAction = (url: string) => {
        mutate({url}, {
            onSuccess: async () => {
                await queryClient.invalidateQueries({queryKey: ['authorized', '/api/invitations/pending']});
                await queryClient.invalidateQueries({queryKey: ['authorized', '/api/groups']});
                await route.refetch();
            },
        });
    };

    return (
        <div className="flex flex-col gap-4">
            <h2 className="text-xs uppercase font-semibold text-text-secondary">
                {labels.sections.pendingInvitations}
            </h2>
            <Card className="p-0 overflow-hidden">
                <table className="w-full text-sm">
                    <tbody>
                    {invitations.map((invitation) => (
                        <tr key={invitation.invitationId}
                            className="border-b border-border last:border-0">
                            <td className="px-4 py-3 font-medium text-text-primary">
                                {invitation.groupName}
                            </td>
                            <td className="px-4 py-3 text-right">
                                <div className="flex justify-end gap-2">
                                    {invitation._links?.accept && (
                                        <Button
                                            variant="primary"
                                            size="sm"
                                            onClick={() => handleAction(toHref(invitation._links.accept!))}
                                            startIcon={<Check className="w-4 h-4"/>}
                                        >
                                            {labels.buttons.accept}
                                        </Button>
                                    )}
                                    {invitation._links?.reject && (
                                        <Button
                                            variant="danger"
                                            size="sm"
                                            onClick={() => handleAction(toHref(invitation._links.reject!))}
                                            startIcon={<X className="w-4 h-4"/>}
                                        >
                                            {labels.buttons.reject}
                                        </Button>
                                    )}
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </Card>
        </div>
    );
};

export const GroupsPage = (): ReactElement => {
    const {isLoading, error, route} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.groups}</h1>

            <PendingInvitationsSection/>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.groupsList}</h2>
                    <HalFormButton name="createGroup" modal={true}/>
                </div>
                <HalEmbeddedTable<GroupSummary>
                    collectionName="groupSummaryResponseList"
                    defaultOrderBy="name"
                    onRowClick={route.navigateToResource}
                    emptyMessage="Nejste členem žádné skupiny."
                >
                    <TableCell sortable column="name">{labels.fields.name}</TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
