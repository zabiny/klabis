import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    FormControlLabel,
    Paper,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    Typography,
} from '@mui/material';
import {useGetMembers} from '../api/membersApi';

const MembersPage = () => {
    const navigate = useNavigate();
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [showSuspended, setShowSuspended] = useState(false);
    const [view, setView] = useState<'compact' | 'full'>('compact');

    // Fetch members data
    const {data: membersResponse, isLoading, error} = useGetMembers(view, showSuspended);

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const handleShowSuspendedChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setShowSuspended(event.target.checked);
    };

    const handleViewChange = () => {
        setView(view === 'compact' ? 'full' : 'compact');
    };

    const handleRowClick = (memberId: number) => {
        navigate(`/members/${memberId}`);
    };

    return (
        <Box>
            <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3}}>
                <Typography variant="h4" component="h1">
                    Členové klubu
                </Typography>
                <Box>
                    <FormControlLabel
                        control={
                            <Switch
                                checked={showSuspended}
                                onChange={handleShowSuspendedChange}
                                color="primary"
                            />
                        }
                        label="Zobrazit pozastavené členství"
                    />
                    <Button
                        variant="outlined"
                        color="primary"
                        onClick={handleViewChange}
                        sx={{ml: 2}}
                    >
                        {view === 'compact' ? 'Zobrazit detaily' : 'Skrýt detaily'}
                    </Button>
                </Box>
            </Box>

            {isLoading ? (
                <Box sx={{display: 'flex', justifyContent: 'center', mt: 4}}>
                    <CircularProgress/>
                </Box>
            ) : error ? (
                <Alert severity="error">Nepodařilo se načíst seznam členů. Zkuste to prosím později.</Alert>
            ) : (
                <Paper>
                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>ID</TableCell>
                                    <TableCell>Jméno</TableCell>
                                    <TableCell>Příjmení</TableCell>
                                    <TableCell>Registrační číslo</TableCell>
                                    {view === 'full' && (
                                        <>
                                            <TableCell>Pohlaví</TableCell>
                                            <TableCell>Datum narození</TableCell>
                                            <TableCell>Národnost</TableCell>
                                        </>
                                    )}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {membersResponse?.data.content
                                    .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
                                    .map((member) => (
                                        <TableRow
                                            key={member.id}
                                            hover
                                            onClick={() => handleRowClick(member.id)}
                                            sx={{cursor: 'pointer'}}
                                        >
                                            <TableCell>{member.id}</TableCell>
                                            <TableCell>{member.firstName}</TableCell>
                                            <TableCell>{member.lastName}</TableCell>
                                            <TableCell>{member.registrationNumber}</TableCell>
                                            {view === 'full' && (
                                                <>
                                                    <TableCell>{member.sex === 'male' ? 'Muž' : 'Žena'}</TableCell>
                                                    <TableCell>{member.dateOfBirth}</TableCell>
                                                    <TableCell>{member.nationality}</TableCell>
                                                </>
                                            )}
                                        </TableRow>
                                    ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                    <TablePagination
                        rowsPerPageOptions={[5, 10, 25]}
                        component="div"
                        count={membersResponse?.data.content.length || 0}
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

export default MembersPage;