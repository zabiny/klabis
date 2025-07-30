import {Box, Typography, Grid, Paper, Button} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import {People as PeopleIcon, Event as EventIcon, Group as GroupIcon} from '@mui/icons-material';

const HomePage = () => {
    return (
        <Box>
            <Typography variant="h4" component="h1" gutterBottom>
                Vítejte v členské sekci
            </Typography>
            <Typography variant="body1" paragraph>
                Vítejte v členské sekci klubu orientačního běhu. Zde můžete spravovat své údaje, prohlížet akce a
                skupiny.
            </Typography>

            <Grid container spacing={3} sx={{mt: 2}}>
                <Grid item xs={12} md={4}>
                    <Paper sx={{p: 3, height: '100%', display: 'flex', flexDirection: 'column'}}>
                        <Box sx={{display: 'flex', alignItems: 'center', mb: 2}}>
                            <PeopleIcon color="primary" sx={{mr: 1, fontSize: 30}}/>
                            <Typography variant="h6">Členové</Typography>
                        </Box>
                        <Typography variant="body2" paragraph sx={{flexGrow: 1}}>
                            Prohlížejte a spravujte členy klubu, jejich údaje a oprávnění.
                        </Typography>
                        <Button
                            component={RouterLink}
                            to="/members"
                            variant="contained"
                            color="primary"
                            fullWidth
                        >
                            Zobrazit členy
                        </Button>
                    </Paper>
                </Grid>

                <Grid item xs={12} md={4}>
                    <Paper sx={{p: 3, height: '100%', display: 'flex', flexDirection: 'column'}}>
                        <Box sx={{display: 'flex', alignItems: 'center', mb: 2}}>
                            <EventIcon color="primary" sx={{mr: 1, fontSize: 30}}/>
                            <Typography variant="h6">Akce</Typography>
                        </Box>
                        <Typography variant="body2" paragraph sx={{flexGrow: 1}}>
                            Prohlížejte nadcházející akce, závody a tréninky.
                        </Typography>
                        <Button
                            component={RouterLink}
                            to="/events"
                            variant="contained"
                            color="primary"
                            fullWidth
                        >
                            Zobrazit akce
                        </Button>
                    </Paper>
                </Grid>

                <Grid item xs={12} md={4}>
                    <Paper sx={{p: 3, height: '100%', display: 'flex', flexDirection: 'column'}}>
                        <Box sx={{display: 'flex', alignItems: 'center', mb: 2}}>
                            <GroupIcon color="primary" sx={{mr: 1, fontSize: 30}}/>
                            <Typography variant="h6">Skupiny</Typography>
                        </Box>
                        <Typography variant="body2" paragraph sx={{flexGrow: 1}}>
                            Prohlížejte a spravujte skupiny členů, tréninkové skupiny a další.
                        </Typography>
                        <Button
                            component={RouterLink}
                            to="/groups"
                            variant="contained"
                            color="primary"
                            fullWidth
                        >
                            Zobrazit skupiny
                        </Button>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default HomePage;