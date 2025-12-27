import {type ReactElement} from "react";
import {TableCell} from "../components/KlabisTable";
import {HalEmbeddedTable} from "../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalSubresourceProvider, useHalRoute} from "../contexts/HalRouteContext.tsx";
import {Skeleton} from "../components/UI";
import {HalFormButton} from "../components/HalNavigator2/HalFormButton.tsx";

interface MemberNameProps {
    user?: { firstName: string, lastName: string }
}

export const MemberName = ({user}: MemberNameProps): ReactElement => {
    const {resourceData} = useHalRoute();
    if (!user) {
        user = resourceData as { firstName: string, lastName: string };
    }
    return <span>{user?.firstName || '-'} {user?.lastName || '-'}</span>
}

function formatCurrency(amount?: number): string {
    return `${amount ?? '-'} Kč`;
}

export const MemberFinancePage = (): ReactElement => {
    const {isLoading, resourceData, navigateToResource} = useHalRoute();

    if (isLoading) {
        return <Skeleton/>
    }

    return <div>
        <h1>Finance</h1>

        <div>
            <span>Zustatek:</span><span>{formatCurrency(resourceData?.balance as number)}</span>
            <HalSubresourceProvider subresourceLinkName={"owner"}>
                <MemberName/>
            </HalSubresourceProvider>
        </div>

        <HalSubresourceProvider subresourceLinkName={"transactions"}>
            <HalEmbeddedTable collectionName={"transactionItemResponseList"} defaultOrderBy={"lastName"}
                              onRowClick={navigateToResource}>
                <TableCell sortable column="date">Datum</TableCell>
                <TableCell column="amount">Částka</TableCell>
                <TableCell column="note">Poznámka</TableCell>
            </HalEmbeddedTable>
        </HalSubresourceProvider>

        <div>
            <HalFormButton name={"deposit"}/>
        </div>
    </div>;

}