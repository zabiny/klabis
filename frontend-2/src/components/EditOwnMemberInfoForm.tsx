import {useState} from 'react';
import {Alert, Button, Checkbox, FormControlLabel, Grid, Paper, Stack, TextField, Typography,} from '@mui/material';
import {type EditMyDetailsForm, useGetEditMyDetailsForm, useUpdateMyDetails} from '../api/membersApi.ts';

interface EditMemberFormUIProps {
    formData: EditMyDetailsForm;
    onSubmit: (formData: EditMyDetailsForm) => void;
    successMessage: string | null
    failureMessage: string | null
    disabled?: boolean;
}

const FormLoadingUI = () => <Typography>Načítání...</Typography>;

const EditMemberFormUI = ({
                              formData, onSubmit, successMessage, failureMessage, disabled = false,
                          }: EditMemberFormUIProps) => {
    const [formState, setFormState] = useState<EditMyDetailsForm>({
        nationality: formData.nationality ?? '',
        address: {
            streetAndNumber: formData.address?.streetAndNumber ?? '',
            city: formData.address?.city ?? '',
            postalCode: formData.address?.postalCode ?? '',
            country: formData.address?.country ?? '',
        },
        contact: {
            email: formData.contact?.email ?? '',
            phone: formData.contact?.phone ?? '',
            note: formData.contact?.note ?? '',
        },
        identityCard: {
            number: formData.identityCard?.number ?? '',
            expiryDate: formData.identityCard?.expiryDate ?? '',
        },
        bankAccount: formData.bankAccount ?? '',
        dietaryRestrictions: formData.dietaryRestrictions ?? '',
        medicCourse: formData.medicCourse ?? false,
        siCard: formData.siCard ?? undefined,
        drivingLicence: formData.drivingLicence ?? [],
    });

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

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        onSubmit(formState);
    }

    const handleReset = () => {
        setFormState(formData);
    };

    return (
        <Paper sx={{p: 3}}>
            <Typography variant="h5" gutterBottom>
                Úprava osobních údajů
            </Typography>

            {successMessage && (
                <Alert severity="success" sx={{mb: 2}}>{successMessage}</Alert>
            )}

            {failureMessage && (
                <Alert severity="error" sx={{mb: 2}}>{failureMessage}</Alert>
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
                                disabled={disabled}
                            >
                                Uložit změny
                            </Button>
                            <Button
                                variant="outlined"
                                color="secondary"
                                onClick={handleReset}
                                disabled={disabled}
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

interface EditMemberFormProps {
    memberId: number;
}

const EditMemberForm = ({memberId}: EditMemberFormProps) => {
    const {data: formData, isLoading} = useGetEditMyDetailsForm(memberId);

    const mutation = useUpdateMyDetails(memberId);

    const handleSubmit = async (e: EditMyDetailsForm) => {
        try {
            await mutation.mutateAsync(e);
        } catch (error) {
            console.error('Chyba při ukládání:', error);
        }
    };

    if (isLoading) {
        return <FormLoadingUI/>;
    }

    return (
        <EditMemberFormUI
            formData={formData?.data ?? {} as EditMyDetailsForm}
            onSubmit={handleSubmit}
            successMessage={mutation.isSuccess ? 'Změny byly úspěšně uloženy' : null}
            failureMessage={mutation.isError ? 'Při ukládání došlo k chybě' : null}
            disabled={mutation.isPending}
        />
    );
};

export default EditMemberForm;