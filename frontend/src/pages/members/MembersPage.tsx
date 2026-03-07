import {type ReactElement} from "react";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {type FormStep, MultiStepFormModal} from "../../components/HalNavigator2/MultiStepFormModal.tsx";
import {HalLinksSection} from "../../components/HalNavigator2/HalLinksSection.tsx";

type MemberListData = EntityModel<{
    id: string,  // UUID as string
    firstName: string,
    lastName: string,
    registrationNumber: string
}>;

export const MembersPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-6">
        <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-text-primary">Členové</h1>
            <RegisterMemberFormButton/>
        </div>

        <HalEmbeddedTable<MemberListData> collectionName={"memberSummaryResponseList"} defaultOrderBy={"lastName"}
                                          onRowClick={route.navigateToResource}>
            <TableCell sortable column="registrationNumber">Reg. číslo</TableCell>
            <TableCell sortable column="lastName">Příjmení</TableCell>
            <TableCell sortable column="firstName">Jméno</TableCell>
            <TableCell column="_links"
                       dataRender={props => (
                           <HalLinksSection links={props.value as any}/>)}>Akce</TableCell>
        </HalEmbeddedTable>
    </div>;

}

/**
 * Form steps for member registration form
 */
const memberRegistrationSteps: FormStep[] = [
    {
        title: 'Krok 1: Osobní údaje',
        fields: ['firstName', 'lastName', 'sex', 'dateOfBirth', 'birthCertificateNumber', 'nationality'],
    },
    {
        title: 'Krok 2: Kontaktní informace',
        fields: ['address', 'contact', 'guardians'],
    },
    {
        title: 'Krok 3: Údaje člena',
        fields: ['siCard', 'bankAccount', 'registrationNumber', 'orisId'],
    },
];

const RegisterMemberFormButton = () => {
    return <HalFormButton
        name={"memberRegistrationsPost"}
        customLayout={<MultiStepFormModal steps={memberRegistrationSteps}/>}
    />;
}