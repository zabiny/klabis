import React, {useState} from 'react';
import {Alert, Button, Dialog, DialogContent, Grid, MenuItem, Paper, Stack, TextField, Typography} from '@mui/material';
import {type MemberRegistrationForm, useRegisterMember} from '../../api/membersApi.ts';

import AddressFields from './AddressFields';
import ContactFields from './ContactFields';
import GuardiansFields from './GuardiansFields';

// UI komponenta pro formulář registrace nového člena
interface RegisterMemberFormUIProps {
    formData: MemberRegistrationForm;
    onSubmit: (formData: MemberRegistrationForm) => void;
    successMessage: string | null
    failureMessage: string | null
    disabled?: boolean;
}

const FormLoadingUI = () => <Typography>Načítání...</Typography>;

const RegisterMemberFormUI = ({
                                  formData,
                                  onSubmit,
                                  successMessage,
                                  failureMessage,
                                  disabled = false,
                              }: RegisterMemberFormUIProps) => {
    // Inicializační stav pro formulář
    const [formState, setFormState] = useState<MemberRegistrationForm>({
        firstName: formData.firstName ?? '',
        lastName: formData.lastName ?? '',
        sex: formData.sex ?? 'male',
        dateOfBirth: formData.dateOfBirth ?? '',
        birthCertificateNumber: formData.birthCertificateNumber ?? '',
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
        guardians: formData.guardians ?? [],
        siCard: formData.siCard ?? undefined,
        bankAccount: formData.bankAccount ?? '',
        registrationNumber: formData.registrationNumber ?? '',
        orisId: formData.orisId ?? undefined,
    });

    // Pomocná funkce pro jednoduché nested setování hodnot
    const handleInputChange = (path: string, value: any) => {
        setFormState(prev => {
            const newState: any = {...prev};
            const keys = path.split('.');
            let obj = newState;
            for (let i = 0; i < keys.length - 1; i++) {
                if (!(keys[i] in obj)) obj[keys[i]] = {};
                obj = obj[keys[i]];
            }
            obj[keys[keys.length - 1]] = value;
            return newState;
        });
    };

    const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        onSubmit(formState);
    };

    return (
        <Paper sx={{p: 3}}>
            <Typography variant="h5" gutterBottom>
                Registrace nového člena
            </Typography>

            {successMessage && <Alert severity="success" sx={{mb: 2}}>{successMessage}</Alert>}

            {failureMessage && <Alert severity="error" sx={{mb: 2}}>{failureMessage}</Alert>}

            <form onSubmit={handleSubmit}>
                <Grid container spacing={3}>

                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Jméno"
                            required
                            value={formState.firstName}
                            onChange={e => handleInputChange('firstName', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Příjmení"
                            required
                            value={formState.lastName}
                            onChange={e => handleInputChange('lastName', e.target.value)}
                        />
                    </Grid>

                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Datum narození"
                            type="date"
                            required
                            InputLabelProps={{shrink: true}}
                            value={formState.dateOfBirth}
                            onChange={e => handleInputChange('dateOfBirth', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            select
                            fullWidth
                            label="Pohlaví"
                            required
                            value={formState.sex}
                            onChange={e => handleInputChange('sex', e.target.value)}
                        >
                            <MenuItem value="male">Muž</MenuItem>
                            <MenuItem value="female">Žena</MenuItem>
                        </TextField>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Číslo rodného listu"
                            value={formState.birthCertificateNumber || ''}
                            onChange={e => handleInputChange('birthCertificateNumber', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Národnost"
                            required
                            value={formState.nationality}
                            onChange={e => handleInputChange('nationality', e.target.value)}
                        />
                    </Grid>
                    {/* Adresa */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>Adresa</Typography>
                    </Grid>
                    <AddressFields
                        value={formState.address}
                        onChange={address => setFormState(prev => ({...prev, address}))}
                    />

                    {/* Kontaktní údaje */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>Kontaktní údaje</Typography>
                    </Grid>
                    <ContactFields
                        value={formState.contact ?? {email: '', phone: '', note: ''}}
                        onChange={contact => setFormState(prev => ({...prev, contact}))}
                    />

                    {/* Právní zástupci */}
                    <Grid item xs={12}>
                        <GuardiansFields
                            value={formState.guardians || []}
                            onChange={guardians => setFormState(prev => ({...prev, guardians}))}
                            disabled={disabled}
                        />
                    </Grid>

                    {/* Ostatní údaje */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>Další údaje</Typography>
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField
                            fullWidth
                            label="Číslo SI čipu"
                            value={formState.siCard ?? ''}
                            onChange={e => handleInputChange('siCard', e.target.value)}
                            type="number"
                            InputProps={{
                                inputProps: {min: 0}
                            }}
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField
                            fullWidth
                            label="Bankovní účet"
                            value={formState.bankAccount ?? ''}
                            onChange={e => handleInputChange('bankAccount', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField
                            fullWidth
                            label="Registrační číslo"
                            value={formState.registrationNumber ?? ''}
                            onChange={e => handleInputChange('registrationNumber', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField
                            fullWidth
                            label="ORIS ID"
                            value={formState.orisId ?? ''}
                            onChange={e => handleInputChange('orisId', e.target.value)}
                            type="number"
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
                                Registrovat člena
                            </Button>
                        </Stack>
                    </Grid>
                </Grid>
            </form>
        </Paper>
    );
};

// Hlavní komponenta s načítáním dat a mutation
const RegisterMemberForm = () => {
    const {data, isLoading} = {data: {} as MemberRegistrationForm, isLoading: false};
    const mutation = useRegisterMember();

    const [submitted, setSubmitted] = useState(false);

    const handleSubmit = async (v: MemberRegistrationForm) => {
        try {
            await mutation.mutateAsync(v);
            setSubmitted(true);
        } catch (e) {
            // Chyba se zobrazí alertem
        }
    };

    if (isLoading || !data) {
        return <FormLoadingUI/>;
    }

    return (
        <RegisterMemberFormUI
            formData={data}
            onSubmit={handleSubmit}
            successMessage={mutation.isSuccess && submitted ? "Registrace byla úspěšná!" : null}
            failureMessage={mutation.isError ? "Během registrace došlo k chybě." : null}
            disabled={mutation.isPending}
        />
    );
};

interface DialogProps {
    open: boolean,
    onClose: () => void,
    onSuccess: () => void
}

export const RegisterMemberFormDialog = ({open, onClose, onSuccess}: DialogProps) => {
    return (<Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogContent>
            <RegisterMemberForm/>
        </DialogContent>
        {/*<DialogActions>*/}
        {/*    <Button onClick={onClose} color="primary">*/}
        {/*        Zavřít*/}
        {/*    </Button>*/}
        {/*</DialogActions>*/}
    </Dialog>)
}

export default RegisterMemberFormDialog;