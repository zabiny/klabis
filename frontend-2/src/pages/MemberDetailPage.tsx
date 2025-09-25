import {useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogContent,
    Divider,
    Grid,
    List,
    ListItem,
    ListItemText,
    Paper,
    Tab,
    Tabs,
    Typography,
} from '@mui/material';
import {ArrowBack as ArrowBackIcon, Edit as EditIcon} from '@mui/icons-material';
import {useGetMember} from '../api/membersApi';
import {EditMemberFormUI} from '../components/members/EditOwnMemberInfoForm.tsx';
import MemberSuspendConfirmationDialog from "../components/members/MemberSuspendConfirmationDialog.tsx";
import EditMemberPermissionsForm from "../components/members/EditMemberPermissionsForm.tsx";
import {hasAction} from "../api/klabisJsonUtils.tsx";
import {KlabisApiForm} from "../components/KlabisApiForm";

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

const TabPanel = (props: TabPanelProps) => {
    const {children, value, index, ...other} = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`member-tabpanel-${index}`}
            aria-labelledby={`member-tab-${index}`}
            {...other}
        >
            {value === index && <Box sx={{p: 3}}>{children}</Box>}
        </div>
    );
};

const MemberDetailPage = () => {
    const {memberId} = useParams<{ memberId: string }>();
    const navigate = useNavigate();
    const [tabValue, setTabValue] = useState(0);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isConfirmDialogOpen, setIsConfirmDialogOpen] = useState(false);

    // Fetch member data
    const {data: memberResponse, isLoading, error} = useGetMember(Number(memberId));

    const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
        setTabValue(newValue);
    };

    const handleBack = () => {
        navigate('/members');
    };

    const handleEdit = () => {
        setIsEditModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsEditModalOpen(false);
    };

    const handleOpenConfirmDialog = () => {
        setIsConfirmDialogOpen(true);
    };

    const handleCloseConfirmDialog = () => {
        setIsConfirmDialogOpen(false);
    };

    if (isLoading) {
        return (
            <Box sx={{display: 'flex', justifyContent: 'center', mt: 4}}>
                <CircularProgress/>
            </Box>
        );
    }

    if (error || !memberResponse) {
        return (
            <Box>
                <Button startIcon={<ArrowBackIcon/>} onClick={handleBack} sx={{mb: 2}}>
                    Zpět na seznam členů
                </Button>
                <Alert severity="error">Nepodařilo se načíst detail člena. Zkuste to prosím později.</Alert>
            </Box>
        );
    }

    const member = memberResponse.data;

    return (
        <Box>
            <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3}}>
                <Button startIcon={<ArrowBackIcon/>} onClick={handleBack}>
                    Zpět na seznam členů
                </Button>
                <Box sx={{display: 'flex', gap: 2}}>
                    {hasAction(member, "members:editOwnInfo") && (
                        <>
                            <Button startIcon={<EditIcon/>} variant="contained" color="primary" onClick={handleEdit}>
                                Upravit
                            </Button>

                            <Dialog open={isEditModalOpen} onClose={handleCloseModal} maxWidth="md" fullWidth>
                                <DialogContent>
                                    <KlabisApiForm apiPath={`/members/${memberId}/editOwnMemberInfoForm`}
                                                   form={EditMemberFormUI}/>
                                </DialogContent>
                                {/*<DialogActions>*/}
                                {/*    <Button onClick={handleCloseModal} color="primary">*/}
                                {/*        Zavřít*/}
                                {/*    </Button>*/}
                                {/*</DialogActions>*/}
                            </Dialog>

                        </>
                    )}
                    {hasAction(member, 'members:suspendMembership') && (
                        <>
                            <Button variant="contained" color="secondary" onClick={handleOpenConfirmDialog}>
                                Zrušit členství
                            </Button>
                            <MemberSuspendConfirmationDialog
                                memberId={Number(memberId)}
                                open={isConfirmDialogOpen}
                                onClose={handleCloseConfirmDialog}
                            />
                        </>
                    )}
                </Box>
            </Box>

            <Paper sx={{mb: 3}}>
                <Box sx={{p: 3}}>
                    <Typography variant="h4" component="h1" gutterBottom>
                        {member.firstName} {member.lastName}
                    </Typography>
                    <Typography variant="subtitle1" color="text.secondary">
                        Registrační číslo: {member.registrationNumber}
                    </Typography>
                </Box>

                <Box sx={{borderBottom: 1, borderColor: 'divider'}}>
                    <Tabs value={tabValue} onChange={handleTabChange} aria-label="member tabs">
                        <Tab label="Základní informace" id="member-tab-0" aria-controls="member-tabpanel-0"/>
                        <Tab label="Kontaktní údaje" id="member-tab-1" aria-controls="member-tabpanel-1"/>
                        <Tab label="Různé" id="member-tab-2" aria-controls="member-tabpanel-2"/>
                        {hasAction(member, 'members:permissions') && (
                            <Tab label="Oprávnění" id="member-tab-3" aria-controls="member-tabpanel-3"/>
                        )}
                    </Tabs>
                </Box>

                <TabPanel value={tabValue} index={0}>
                    <Grid container spacing={2}>
                        <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                                Osobní údaje
                            </Typography>
                            <List>
                                <ListItem>
                                    <ListItemText primary="Pohlaví" secondary={member.sex === 'male' ? 'Muž' : 'Žena'}/>
                                </ListItem>
                                <ListItem>
                                    <ListItemText primary="Datum narození"
                                                  secondary={member.dateOfBirth || 'Neuvedeno'}/>
                                </ListItem>
                                <ListItem>
                                    <ListItemText primary="Národnost" secondary={member.nationality || 'Neuvedeno'}/>
                                </ListItem>
                                <ListItem>
                                    <ListItemText primary="SI čip" secondary={member.siCard || 'Neuvedeno'}/>
                                </ListItem>
                            </List>
                        </Grid>
                        <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                                Adresa
                            </Typography>
                            {member.address ? (
                                <List>
                                    <ListItem>
                                        <ListItemText primary="Ulice a číslo"
                                                      secondary={member.address.streetAndNumber}/>
                                    </ListItem>
                                    <ListItem>
                                        <ListItemText primary="Město" secondary={member.address.city}/>
                                    </ListItem>
                                    <ListItem>
                                        <ListItemText primary="PSČ" secondary={member.address.postalCode}/>
                                    </ListItem>
                                    <ListItem>
                                        <ListItemText primary="Země" secondary={member.address.country}/>
                                    </ListItem>
                                </List>
                            ) : (
                                <Typography variant="body2" color="text.secondary">
                                    Adresa není uvedena
                                </Typography>
                            )}
                        </Grid>
                    </Grid>
                </TabPanel>

                <TabPanel value={tabValue} index={1}>
                    <Grid container spacing={2}>
                        <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                                Kontaktní údaje
                            </Typography>
                            {member.contact ? (
                                <List>
                                    <ListItem>
                                        <ListItemText primary="Email" secondary={member.contact.email || 'Neuvedeno'}/>
                                    </ListItem>
                                    <ListItem>
                                        <ListItemText primary="Telefon"
                                                      secondary={member.contact.phone || 'Neuvedeno'}/>
                                    </ListItem>
                                    {member.contact.note && (
                                        <ListItem>
                                            <ListItemText primary="Poznámka" secondary={member.contact.note}/>
                                        </ListItem>
                                    )}
                                </List>
                            ) : (
                                <Typography variant="body2" color="text.secondary">
                                    Kontaktní údaje nejsou uvedeny
                                </Typography>
                            )}
                        </Grid>
                        <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                                Zákonní zástupci
                            </Typography>
                            {member.legalGuardians && member.legalGuardians.length > 0 ? (
                                member.legalGuardians.map((guardian, index) => (
                                    <Box key={index} sx={{mb: 2}}>
                                        <Typography variant="subtitle1">
                                            {guardian.firstName} {guardian.lastName}
                                            {guardian.note && ` (${guardian.note})`}
                                        </Typography>
                                        <List dense>
                                            <ListItem>
                                                <ListItemText primary="Email"
                                                              secondary={guardian.contact.email || 'Neuvedeno'}/>
                                            </ListItem>
                                            <ListItem>
                                                <ListItemText primary="Telefon"
                                                              secondary={guardian.contact.phone || 'Neuvedeno'}/>
                                            </ListItem>
                                        </List>
                                        {index < member.legalGuardians.length - 1 && <Divider/>}
                                    </Box>
                                ))
                            ) : (
                                <Typography variant="body2" color="text.secondary">
                                    Zákonní zástupci nejsou uvedeni
                                </Typography>
                            )}
                        </Grid>
                    </Grid>
                </TabPanel>

                <TabPanel value={tabValue} index={2}>
                    <Grid container spacing={2}>
                        <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                                Licence
                            </Typography>
                            {member.licences ? (
                                <List>
                                    {member.licences.ob && (
                                        <ListItem>
                                            <ListItemText primary="OB licence" secondary={member.licences.ob.licence}/>
                                        </ListItem>
                                    )}
                                    {member.licences.referee && (
                                        <ListItem>
                                            <ListItemText
                                                primary="Rozhodčí licence"
                                                secondary={`${member.licences.referee.licence} (platnost do: ${member.licences.referee.expiryDate})`}
                                            />
                                        </ListItem>
                                    )}
                                    {member.licences.trainer && (
                                        <ListItem>
                                            <ListItemText
                                                primary="Trenérská licence"
                                                secondary={`${member.licences.trainer.licence} (platnost do: ${member.licences.trainer.expiryDate})`}
                                            />
                                        </ListItem>
                                    )}
                                </List>
                            ) : (
                                <Typography variant="body2" color="text.secondary">
                                    Licence nejsou uvedeny
                                </Typography>
                            )}
                        </Grid>
                        <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                                Další informace
                            </Typography>
                            <List>
                                {member.bankAccount && (
                                    <ListItem>
                                        <ListItemText primary="Bankovní účet" secondary={member.bankAccount}/>
                                    </ListItem>
                                )}
                                {member.dietaryRestrictions && (
                                    <ListItem>
                                        <ListItemText primary="Dietní omezení" secondary={member.dietaryRestrictions}/>
                                    </ListItem>
                                )}
                                {member.drivingLicence && member.drivingLicence.length > 0 && (
                                    <ListItem>
                                        <ListItemText
                                            primary="Řidičský průkaz"
                                            secondary={member.drivingLicence.join(', ')}
                                        />
                                    </ListItem>
                                )}
                                <ListItem>
                                    <ListItemText
                                        primary="Zdravotnický kurz"
                                        secondary={member.medicCourse ? 'Ano' : 'Ne'}
                                    />
                                </ListItem>
                            </List>
                        </Grid>
                    </Grid>
                </TabPanel>

                {hasAction(member, 'members:permissions') && (
                    <TabPanel value={tabValue} index={3}>
                        <EditMemberPermissionsForm memberId={Number(memberId)}/>
                    </TabPanel>
                )}
            </Paper>
        </Box>
    );
};

export default MemberDetailPage;