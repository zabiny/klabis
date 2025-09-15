import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Box, Button, FormControlLabel, Switch, Typography,} from '@mui/material';
import {KlabisTable, TableCell} from '../components/KlabisTable';
import RegisterMemberDialog from "../components/RegisterMemberForm.tsx";
import {hasAction} from "../hooks/klabisJsonUtils.tsx";

interface Member {
    id: number;
    firstName: string;
    lastName: string;
    registrationNumber: string;
    dateOfBirth: string;
    sex: 'male' | 'female';
    nationality: string;
}

const MembersPage = () => {
    const navigate = useNavigate();
    const [showSuspended, setShowSuspended] = useState(false);
    const [view, setView] = useState<'SUMMARY' | 'DETAILED'>('SUMMARY');
    const [openRegisterDialog, setOpenRegisterDialog] = useState(false);

    const [tableActions, setTableActions] = useState<string[]>();

    const handleShowSuspendedChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setShowSuspended(event.target.checked);
    };

    const handleViewChange = () => {
        setView(view === 'SUMMARY' ? 'DETAILED' : 'SUMMARY');
    };

    const handleRowClick = (member: Member) => {
        navigate(`/members/${member.id}`);
    };

    const canRegisterMember = hasAction(tableActions, 'members:register') || false;

    const additionalParams = {
        suspended: showSuspended,
        view: view,
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

            {canRegisterMember && (
                <RegisterMemberDialog
                    open={openRegisterDialog}
                    onClose={() => setOpenRegisterDialog(false)}
                    onSuccess={() => setOpenRegisterDialog(false)}
                />
            )}

            <KlabisTable<Member>
                api="/members"
                onRowClick={handleRowClick}
                defaultOrderBy="lastName"
                defaultOrderDirection="asc"
                additionalParams={additionalParams}
                onTableActionsLoaded={setTableActions}
                queryKey="members-table"
            >
                <TableCell column="id">ID</TableCell>
                <TableCell sortable column="firstName">Jméno</TableCell>
                <TableCell sortable column="lastName">Příjmení</TableCell>
                <TableCell sortable column="registrationNumber">Registrační číslo</TableCell>
                <TableCell hidden={view === 'SUMMARY'} column="sex">Pohlaví</TableCell>
                <TableCell sortable hidden={view === 'SUMMARY'} column="dateOfBirth">Datum narození</TableCell>
                <TableCell hidden={view === 'SUMMARY'} column="nationality">Národnost</TableCell>
            </KlabisTable>

        </Box>
    );
};

export default MembersPage;
