import {type ReactElement} from "react";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {Badge} from "../../components/UI/Badge";

interface MemberSummaryData extends EntityModel<{
    id: string,
    registrationNumber: string,
    lastName: string,
    firstName: string,
    email: string,
    active: boolean,
}> {
}

export const MembersPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Členové</h1>

        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-text-primary">Seznam členů</h2>
                <HalFormButton name="default" modal={true}/>
            </div>
            <HalEmbeddedTable<MemberSummaryData> collectionName={"memberSummaryResponseList"}
                                                  defaultOrderBy={"lastName"}
                                                  onRowClick={route.navigateToResource}>
                <TableCell sortable column={"registrationNumber"}>Reg. číslo</TableCell>
                <TableCell sortable column={"lastName"}>Příjmení</TableCell>
                <TableCell sortable column={"firstName"}>Jméno</TableCell>
                <TableCell column={"email"} dataRender={({value}) => (
                    <span className="truncate max-w-[200px] block" title={String(value ?? '')}>
                        {String(value ?? '')}
                    </span>
                )}>E-mail</TableCell>
                <TableCell column={"active"} dataRender={({value}) => (
                    <Badge variant={value ? 'success' : 'default'} size="sm">
                        {value ? 'Aktivní' : 'Neaktivní'}
                    </Badge>
                )}>Stav</TableCell>
            </HalEmbeddedTable>
        </div>
    </div>;
};
