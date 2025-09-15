import {Box, Typography,} from '@mui/material';
import {type EventListItem} from '../api/eventsApi';
import {KlabisTable, SortableTableCell, TableCell} from "../components/KlabisTable";

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
                return type;
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

            <KlabisTable<EventListItem> api={"/events"}>
                <SortableTableCell column={"date"}>Datum</SortableTableCell>
                <SortableTableCell column={"name"}>Název</SortableTableCell>
                <TableCell hidden column={"type"}>Typ</TableCell>
                <SortableTableCell column={"organizer"}>Pořadatel</SortableTableCell>
                <SortableTableCell hidden column={"coordinator"}>Koordinátor</SortableTableCell>
                <SortableTableCell column={"registrationDeadline"}>Uzávěrka přihlášek</SortableTableCell>
                <TableCell hidden column={"web"}>Web</TableCell>

            </KlabisTable>
        </Box>
    );
};

export default EventsPage;