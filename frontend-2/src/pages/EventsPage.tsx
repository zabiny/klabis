import {Box, Typography,} from '@mui/material';
import {type EventListItem} from '../api/eventsApi';
import {KlabisTable, TableCell} from "../components/KlabisTable";

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

            <KlabisTable<EventListItem> api={"/events"}>
                <TableCell sortable column={"date"} as={({value}) => formatDate(value)}>Datum</TableCell>
                <TableCell sortable column={"name"}>Název</TableCell>
                <TableCell hidden column={"type"}>Typ</TableCell>
                <TableCell hidden sortable column={"coordinator"}>Koordinátor</TableCell>
                <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                <TableCell sortable column={"registrationDeadline"} as={({value}) => formatDate(value)}>Uzávěrka
                    přihlášek</TableCell>
                <TableCell hidden column={"web"}>Web</TableCell>

            </KlabisTable>
        </Box>
    );
};

export default EventsPage;