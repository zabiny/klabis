import {useState} from 'react';
import {Alert, Button, Checkbox, FormControlLabel, Grid, Paper, Stack, TextField, Typography,} from '@mui/material';
import type {EditMyDetailsForm} from '../api/membersApi';
import {useGetEditMyDetailsForm, useUpdateMyDetails} from '../api/membersApi';

interface EditMemberFormProps {
    memberId: number;
}

const EditMemberForm = ({memberId}: EditMemberFormProps) => {
    const {data: formData, isLoading} = useGetEditMyDetailsForm(memberId);

    const mutation = useUpdateMyDetails(memberId);

    const [formState, setFormState] = useState<EditMyDetailsForm>({
        nationality: formData?.data.nationality ?? '',
        address: {
            streetAndNumber: formData?.data.address.streetAndNumber ?? '',
            city: formData?.data.address.city ?? '',
            postalCode: formData?.data.address.postalCode ?? '',
            country: formData?.data.address.country ?? '',
        },
        contact: {
            email: formData?.data.contact?.email ?? '',
            phone: formData?.data.contact?.phone ?? '',
            note: formData?.data.contact?.note ?? '',
        },
        identityCard: {
            number: formData?.data.identityCard?.number ?? '',
            expiryDate: formData?.data.identityCard?.expiryDate ?? '',
        },
        bankAccount: formData?.data.bankAccount ?? '',
        dietaryRestrictions: formData?.data.dietaryRestrictions ?? '',
        medicCourse: formData?.data.medicCourse ?? false,
        siCard: formData?.data.siCard,
        drivingLicence: formData?.data.drivingLicence ?? [],
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await mutation.mutateAsync(formState);
        } catch (error) {
            console.error('Chyba při ukládání:', error);
        }
    };

    const handleInputChange = (path: string, value: any) => {
        setFormState((prev) => {
            const newState = {...prev};
            const keys = path.split('.');
            let current: any = newState;

            for (let i = 0; i < keys.length - 1; i++) {
                current = current[keys[i]];
            }
            current[keys[keys.length - 1]] = value;
            return newState;
        });
    };

    if (isLoading) {
        return <Typography>Načítání...</Typography>;
    }

    return (
        <Paper sx={{p: 3}}>
            <Typography variant="h5" gutterBottom>
                Úprava osobních údajů
            </Typography>

            {mutation.isSuccess && (
                <Alert severity="success" sx={{mb: 2}}>
                    Změny byly úspěšně uloženy
                </Alert>
            )}

            {mutation.isError && (
                <Alert severity="error" sx={{mb: 2}}>
                    Při ukládání došlo k chybě
                </Alert>
            )}

            <form onSubmit={handleSubmit}>
                <Grid container spacing={3}>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Národnost"
                            value={formState.nationality}
                            onChange={(e) => handleInputChange('nationality', e.target.value)}
                            required
                        />
                    </Grid>

                    {/* Adresa */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>
                            Adresa
                        </Typography>
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Ulice a číslo"
                            value={formState.address.streetAndNumber}
                            onChange={(e) => handleInputChange('address.streetAndNumber', e.target.value)}
                            required
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Město"
                            value={formState.address.city}
                            onChange={(e) => handleInputChange('address.city', e.target.value)}
                            required
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="PSČ"
                            value={formState.address.postalCode}
                            onChange={(e) => handleInputChange('address.postalCode', e.target.value)}
                            required
                        />
                    </Grid>

                    {/* Kontakt */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>
                            Kontaktní údaje
                        </Typography>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Email"
                            type="email"
                            value={formState.contact?.email ?? ''}
                            onChange={(e) => handleInputChange('contact.email', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Telefon"
                            value={formState.contact?.phone ?? ''}
                            onChange={(e) => handleInputChange('contact.phone', e.target.value)}
                        />
                    </Grid>

                    {/* Ostatní údaje */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>
                            Další údaje
                        </Typography>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Číslo SI čipu"
                            value={formState.siCard ?? ''}
                            onChange={(e) => handleInputChange('siCard', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Bankovní účet"
                            value={formState.bankAccount ?? ''}
                            onChange={(e) => handleInputChange('bankAccount', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Dietní omezení"
                            multiline
                            rows={2}
                            value={formState.dietaryRestrictions ?? ''}
                            onChange={(e) => handleInputChange('dietaryRestrictions', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <FormControlLabel
                            control={
                                <Checkbox
                                    checked={formState.medicCourse ?? false}
                                    onChange={(e) => handleInputChange('medicCourse', e.target.checked)}
                                />
                            }
                            label="Zdravotnický kurz"
                        />
                    </Grid>

                    <Grid item xs={12}>
                        <Stack direction="row" spacing={2}>
                            <Button
                                type="submit"
                                variant="contained"
                                color="primary"
                                disabled={mutation.isLoading}
                            >
                                Uložit změny
                            </Button>
                            <Button
                                variant="outlined"
                                color="secondary"
                                onClick={() => setFormState(formData?.data ?? {} as EditMyDetailsForm)}
                            >
                                Obnovit
                            </Button>
                        </Stack>
                    </Grid>
                </Grid>
            </form>
        </Paper>
    );
};

export default EditMemberForm;