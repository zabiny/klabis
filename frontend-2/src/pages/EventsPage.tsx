import {type ReactElement} from "react";
import type {EntityModel} from "../api";
import {TableCell} from "../components/KlabisTable";
import {HalLinksSection} from "../components/HalNavigator2/HalLinksSection.tsx";
import {HalEmbeddedTable} from "../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormsSection} from "../components/HalNavigator2/HalFormsSection.tsx";
import EventType from "../components/events/EventType.tsx";
import MemberName from "../components/members/MemberName.tsx";
import {formatDate} from "../utils/dateUtils.ts";
import {useHalPageData} from "../hooks/useHalPageData.ts";

interface EventListData extends EntityModel<{ date: string, name: string, id: number, location: string }> {
};

export const EventsPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Závody</h1>

        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-text-primary">Seznam závodů</h2>
            <HalEmbeddedTable<EventListData> collectionName={"eventResponseList"} defaultOrderBy={"date"}
                                             onRowClick={route.navigateToResource}>
                <TableCell sortable column={"date"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Datum</TableCell>
                <TableCell sortable column={"name"}>Název</TableCell>
                <TableCell sortable column={"location"}>Místo</TableCell>
                <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                <TableCell column={"type"}
                           dataRender={({value}) => <EventType eventType={value as any}/>}>Typ</TableCell>
                <TableCell column={"web"}
                           dataRender={({value}) => !value ? null : (
                               <a href={typeof value === 'string' ? value : undefined}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="text-primary hover:text-primary-light transition-colors"
                                  aria-label="External link">
                                   <svg className="w-5 h-5 inline" fill="currentColor" viewBox="0 0 20 20">
                                       <path
                                           d="M11 3a1 1 0 100 2h3.586L9.293 9.293a1 1 0 101.414 1.414L16 6.414V10a1 1 0 102 0V4a1 1 0 00-1-1h-6z"/>
                                       <path
                                           d="M5 5a2 2 0 00-2 2v8a2 2 0 002 2h8a2 2 0 002-2v-3a1 1 0 10-2 0v3H5V7h3a1 1 0 000-2H5z"/>
                                   </svg>
                               </a>
                           )}>Web</TableCell>
                <TableCell sortable column={"registrationDeadline"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Uzávěrka
                    přihlášek</TableCell>
                <TableCell column={"coordinator"} dataRender={({value}) => typeof value === 'number' ?
                    <MemberName memberId={value}/> : <>--</>}>Vedoucí</TableCell>
            </HalEmbeddedTable>
        </div>

        <div className="flex flex-col gap-4">
            <HalLinksSection/>
        </div>
        <div className="flex flex-col gap-4">
            <HalFormsSection/>
        </div>
    </div>;

}