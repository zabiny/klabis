import {Box, Typography,} from '@mui/material';
import {type EventListItem} from '../api/eventsApi';
import {KlabisTable, TableCell} from "../components/KlabisTable";
import MemberName from "../components/members/MemberName";
import {Actions} from "../components/Actions";
import React from "react";

const EventsPage = () => {
    // Function to format date
    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return new Intl.DateTimeFormat('cs-CZ').format(date);
    };

    // Function to get event type label
    const getEventTypeLabel = (type: string) => {
        switch (type) {
            case 'T':
                return 'Trénink';
            case 'S':
                return 'Závod';
            default:
                return 'Neznamy';
        }
    };

    // Function to get event type color
    const getEventTypeColor = (type: string) => {
        switch (type) {
            case 'T':
                return 'primary';
            case 'S':
                return 'secondary';
            default:
                return 'default';
        }
    };

    return (
        <Box>
            <Typography variant="h4" component="h1" gutterBottom>
                Akce
            </Typography>

            <KlabisTable<EventListItem> api={"/events"} queryKey={"events"} defaultOrderBy={"date"}>
                <TableCell sortable column={"date"} dataRender={({value}) => formatDate(value)}>Datum</TableCell>
                <TableCell sortable column={"name"}>Název</TableCell>
                <TableCell sortable column={"location"}>Místo</TableCell>
                <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                <TableCell column={"type"}>Typ</TableCell>
                <TableCell column={"web"}>Web</TableCell>
                <TableCell sortable column={"registrationDeadline"}
                           dataRender={({value}) => formatDate(value)}>Uzávěrka přihlášek</TableCell>
                <TableCell sortable column={"coordinator"} dataRender={({value}) => value ?
                    <MemberName memberId={value}/> : <>--</>}>Vedoucí</TableCell>
                <TableCell column="_actions"
                           dataRender={props => (<Actions value={props.value}/>)}>Možnosti</TableCell>

            </KlabisTable>
        </Box>
    );
};

export default EventsPage;