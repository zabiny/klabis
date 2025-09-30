import {useState} from 'react';
import {
    Alert,
    Box,
    Button,
    Card,
    CardActions,
    CardContent,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Grid,
    InputAdornment,
    Paper,
    TextField,
    Typography,
} from '@mui/material';
import {
    Add as AddIcon,
    Delete as DeleteIcon,
    Edit as EditIcon,
    Email as EmailIcon,
    Search as SearchIcon,
} from '@mui/icons-material';
import {useKlabisApiMutation, useKlabisApiQuery} from "../api";

const GroupsPage = () => {
    const [searchTerm, setSearchTerm] = useState('');
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);

    // Fetch groups data
    // TODO: correct API to Groups
    const {data, isLoading, error} = useKlabisApiQuery("get", "/grants");
    const deleteGroupMutation = useKlabisApiMutation("put", "/members/{memberId}/changeGrantsForm");

    const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setSearchTerm(event.target.value);
    };

    const handleDeleteClick = (groupId: number) => {
        setSelectedGroupId(groupId);
        setDeleteDialogOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (selectedGroupId) {
            try {
                await deleteGroupMutation.mutateAsync();
                setDeleteDialogOpen(false);
                // Ideally, we would refetch the groups data here
            } catch (error) {
                console.error('Failed to delete group:', error);
            }
        }
    };

    const handleDeleteCancel = () => {
        setDeleteDialogOpen(false);
        setSelectedGroupId(null);
    };

    // Filter groups based on search term
    const filteredGroups = data?.items.filter(
        (group) =>
            group.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (group.description && group.description.toLowerCase().includes(searchTerm.toLowerCase()))
    );

    return (
        <Box>
            <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3}}>
                <Typography variant="h4" component="h1">
                    Skupiny členů
                </Typography>
                <Button variant="contained" color="primary" startIcon={<AddIcon/>}>
                    Nová skupina
                </Button>
            </Box>

            <Box sx={{mb: 3}}>
                <TextField
                    fullWidth
                    variant="outlined"
                    placeholder="Hledat skupinu..."
                    value={searchTerm}
                    onChange={handleSearchChange}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <SearchIcon/>
                            </InputAdornment>
                        ),
                    }}
                />
            </Box>

            {isLoading ? (
                <Box sx={{display: 'flex', justifyContent: 'center', mt: 4}}>
                    <CircularProgress/>
                </Box>
            ) : error ? (
                <Alert severity="error">
                    Nepodařilo se načíst seznam skupin. Zkuste to prosím později.
                </Alert>
            ) : (
                <Grid container spacing={3}>
                    {filteredGroups && filteredGroups.length > 0 ? (
                        filteredGroups.map((group) => (
                            <Grid item xs={12} sm={6} md={4} key={group.id}>
                                <Card>
                                    <CardContent>
                                        <Typography variant="h6" component="h2" gutterBottom>
                                            {group.name}
                                        </Typography>
                                        {group.description && (
                                            <Typography variant="body2" color="text.secondary" paragraph>
                                                {group.description}
                                            </Typography>
                                        )}
                                        {group.address && (
                                            <Box sx={{display: 'flex', alignItems: 'center', mt: 1}}>
                                                <EmailIcon fontSize="small" sx={{mr: 1}}/>
                                                <Typography variant="body2">{group.address}</Typography>
                                            </Box>
                                        )}
                                    </CardContent>
                                    <CardActions>
                                        <Button size="small" startIcon={<EditIcon/>}>
                                            Upravit
                                        </Button>
                                        <Button
                                            size="small"
                                            color="error"
                                            startIcon={<DeleteIcon/>}
                                            onClick={() => handleDeleteClick(group.id)}
                                        >
                                            Smazat
                                        </Button>
                                    </CardActions>
                                </Card>
                            </Grid>
                        ))
                    ) : (
                        <Grid item xs={12}>
                            <Paper sx={{p: 3, textAlign: 'center'}}>
                                <Typography variant="body1">
                                    {searchTerm
                                        ? 'Nebyly nalezeny žádné skupiny odpovídající vašemu vyhledávání.'
                                        : 'Zatím nebyly vytvořeny žádné skupiny.'}
                                </Typography>
                            </Paper>
                        </Grid>
                    )}
                </Grid>
            )}

            {/* Delete confirmation dialog */}
            <Dialog open={deleteDialogOpen} onClose={handleDeleteCancel}>
                <DialogTitle>Smazat skupinu</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Opravdu chcete smazat tuto skupinu? Tato akce je nevratná.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleDeleteCancel}>Zrušit</Button>
                    <Button onClick={handleDeleteConfirm} color="error" autoFocus>
                        Smazat
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default GroupsPage;