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
    TableSortLabel,
    Typography,
} from '@mui/material';
import {type GetMembersParams, useGetMembers} from '../api/membersApi';
import RegisterMemberDialog from "../components/RegisterMemberForm.tsx";
import {hasAction} from "../hooks/klabisJsonUtils.tsx";

type SortableColumn = 'firstName' | 'lastName' | 'registrationNumber' | 'dateOfBirth';
type SortDirection = 'asc' | 'desc';

const MembersPage = () => {
    const navigate = useNavigate();
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [showSuspended, setShowSuspended] = useState(false);
    const [view, setView] = useState<'SUMMARY' | 'DETAILED'>('SUMMARY');
    const [orderBy, setOrderBy] = useState<SortableColumn>('lastName');
    const [orderDirection, setOrderDirection] = useState<SortDirection>('asc');

    // Convert sorting state to API format
    const sort = [`${orderBy},${orderDirection}`];

    // Fetch members data with server-side pagination and sorting
    const membersParams: GetMembersParams = {
        suspended: showSuspended,
        view: view,
        page: page,
        size: rowsPerPage,
        sort: sort
    };
    const {data: membersResponse, isLoading, error} = useGetMembers(membersParams);

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const handleShowSuspendedChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setShowSuspended(event.target.checked);
        setPage(0); // Reset to first page when filtering changes
    };

    const handleViewChange = () => {
        setView(view === 'SUMMARY' ? 'DETAILED' : 'SUMMARY');
    };

    const handleRequestSort = (column: SortableColumn) => {
        const isAsc = orderBy === column && orderDirection === 'asc';
        setOrderDirection(isAsc ? 'desc' : 'asc');
        setOrderBy(column);
        setPage(0); // Reset to first page when sorting changes
    };

    const SortableTableCell = ({column, children}: { column: SortableColumn, children: React.ReactNode }) => (
        <TableCell>
            <TableSortLabel
                active={orderBy === column}
                direction={orderBy === column ? orderDirection : 'asc'}
                onClick={() => handleRequestSort(column)}
                sx={{cursor: 'pointer'}}
            >
                {children}
            </TableSortLabel>
        </TableCell>
    );

    const handleRowClick = (memberId: number) => {
        navigate(`/members/${memberId}`);
    };

    // Vlastní stav pro otevření/zavření dialogu a logika
    const [openRegisterDialog, setOpenRegisterDialog] = useState(false);

    // Vyhodnocení povolení akce registrace podle _actions z members response
    // (hasAction je importován z ../hooks/klabisJsonUtils)
    const canRegisterMember = membersResponse && hasAction(membersResponse.data, 'members:register') || false;

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
                        {view === 'SUMMARY' ? 'Zobrazit detaily' : 'Skrýt detaily'}
                    </Button>
                    {canRegisterMember && (
                        <Button
                            variant="contained"
                            color="secondary"
                            sx={{ml: 2}}
                            onClick={() => setOpenRegisterDialog(true)}
                        >
                            Registrovat nového člena
                        </Button>
                    )}
                </Box>
            </Box>
            {/* Dialog pro registraci člena */}
            {canRegisterMember && (
                <RegisterMemberDialog
                    open={openRegisterDialog}
                    onClose={() => setOpenRegisterDialog(false)}
                    onSuccess={() => setOpenRegisterDialog(false)}
                />
            )}

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
                                    <SortableTableCell column="firstName">Jméno</SortableTableCell>
                                    <SortableTableCell column="lastName">Příjmení</SortableTableCell>
                                    <SortableTableCell column="registrationNumber">Registrační číslo</SortableTableCell>
                                    {view === 'DETAILED' && (
                                        <>
                                            <TableCell>Pohlaví</TableCell>
                                            <SortableTableCell column="dateOfBirth">Datum narození</SortableTableCell>
                                            <TableCell>Národnost</TableCell>
                                        </>
                                    )}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {membersResponse?.data.content.map((member) => (
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
                                        {view === 'DETAILED' && (
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
                        rowsPerPageOptions={[5, 10, 25, 50]}
                        component="div"
                        count={membersResponse?.data.page.totalElements || 0}
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