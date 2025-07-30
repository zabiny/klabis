import {useState} from 'react';
import {
    Box,
    Typography,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TablePagination,
    CircularProgress,
    Alert,
    Chip,
    Link,
    Button,
} from '@mui/material';
import {Event as EventIcon, Link as LinkIcon} from '@mui/icons-material';
import {useGetEvents} from '../api/eventsApi';

const EventsPage = () => {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    // Fetch events data
    const {data, isLoading, error} = useGetEvents();

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

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

            {isLoading ? (
                <Box sx={{display: 'flex', justifyContent: 'center', mt: 4}}>
                    <CircularProgress/>
                </Box>
            ) : error ? (
                <Alert severity="error">Nepodařilo se načíst seznam akcí. Zkuste to prosím později.</Alert>
            ) : (
                <Paper>
                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>Datum</TableCell>
                                    <TableCell>Název</TableCell>
                                    <TableCell>Typ</TableCell>
                                    <TableCell>Pořadatel</TableCell>
                                    <TableCell>Koordinátor</TableCell>
                                    <TableCell>Uzávěrka přihlášek</TableCell>
                                    <TableCell>Web</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {data?.items
                                    .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
                                    .map((event) => (
                                        <TableRow key={event.id} hover>
                                            <TableCell>{formatDate(event.date)}</TableCell>
                                            <TableCell>{event.name}</TableCell>
                                            <TableCell>
                                                <Chip
                                                    label={getEventTypeLabel(event.type)}
                                                    color={getEventTypeColor(event.type) as any}
                                                    size="small"
                                                    icon={<EventIcon/>}
                                                />
                                            </TableCell>
                                            <TableCell>{event.organizer}</TableCell>
                                            <TableCell>{event.coordinator || '-'}</TableCell>
                                            <TableCell>
                                                {event.registrationDeadline
                                                    ? formatDate(event.registrationDeadline)
                                                    : '-'}
                                            </TableCell>
                                            <TableCell>
                                                {event.web ? (
                                                    <Button
                                                        href={event.web}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        startIcon={<LinkIcon/>}
                                                        size="small"
                                                    >
                                                        Web
                                                    </Button>
                                                ) : (
                                                    '-'
                                                )}
                                            </TableCell>
                                        </TableRow>
                                    ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                    <TablePagination
                        rowsPerPageOptions={[5, 10, 25]}
                        component="div"
                        count={data?.items.length || 0}
                        rowsPerPage={rowsPerPage}
                        page={page}
                        onPageChange={handleChangePage}
                        onRowsPerPageChange={handleChangeRowsPerPage}
                        labelRowsPerPage="Řádků na stránku:"
                        labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
                    />
                </Paper>
            )}
        </Box>
    );
};

export default EventsPage;