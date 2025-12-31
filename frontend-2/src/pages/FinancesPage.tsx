import {type ReactElement} from "react";
import {TableCell} from "../components/KlabisTable";
import {HalEmbeddedTable} from "../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalSubresourceProvider, useHalRoute} from "../contexts/HalRouteContext.tsx";
import {Skeleton} from "../components/UI";
import {HalFormButton} from "../components/HalNavigator2/HalFormButton.tsx";
import {useHalPageData} from "../hooks/useHalPageData.ts";

interface MemberNameProps {
    user?: { firstName: string, lastName: string }
}

export const MemberName = ({user}: MemberNameProps): ReactElement => {
    const {resourceData} = useHalRoute();
    if (!user) {
        user = resourceData as { firstName: string, lastName: string };
    }
    return <span className="text-text-primary">{user?.firstName || '-'} {user?.lastName || '-'}</span>
}

function formatCurrency(amount?: number): string {
    return `${amount ?? '-'} Kč`;
}

export const MemberFinancePage = (): ReactElement => {
    const {isLoading, resourceData, route} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>
    }

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Finance</h1>

        <div className="bg-surface-raised rounded-md border border-border p-6">
            <div className="flex flex-col gap-4">
                <div>
                    <p className="text-xs uppercase font-semibold text-text-secondary mb-2">Zůstatek</p>
                    <p className="text-2xl font-semibold text-text-primary">{formatCurrency(resourceData?.balance as number)}</p>
                </div>
                <div>
                    <p className="text-xs uppercase font-semibold text-text-secondary mb-2">Majitel</p>
                    <HalSubresourceProvider subresourceLinkName={"owner"}>
                        <MemberName/>
                    </HalSubresourceProvider>
                </div>
            </div>
        </div>

        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-text-primary">Transakce</h2>
            <HalSubresourceProvider subresourceLinkName={"transactions"}>
                <HalEmbeddedTable collectionName={"transactionItemResponseList"} defaultOrderBy={"lastName"}
                                  onRowClick={route.navigateToResource}>
                    <TableCell sortable column="date">Datum</TableCell>
                    <TableCell column="amount">Částka</TableCell>
                    <TableCell column="note">Poznámka</TableCell>
                </HalEmbeddedTable>
            </HalSubresourceProvider>
        </div>

        <div className="flex gap-3">
            <HalFormButton name={"deposit"}/>
        </div>
    </div>;

}