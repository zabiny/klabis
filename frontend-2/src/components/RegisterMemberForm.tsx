import React, {useState} from 'react';
import {Alert, Button, Dialog, DialogContent, Grid, MenuItem, Paper, Stack, TextField, Typography} from '@mui/material';
import {type MemberRegistrationForm, useRegisterMember} from '../api/membersApi.ts';

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

    // Ovládání pro více zákonných zástupců
    const handleGuardianChange = (idx: number, path: string, value: any) => {
        setFormState(prev => {
            const guardians = prev.guardians ? [...prev.guardians] : [];
            const keys = path.split('.');
            let obj = guardians[idx];
            if (!obj) {
                guardians[idx] = {firstName: '', lastName: '', contact: {}};
                obj = guardians[idx];
            }
            let ref = obj;
            for (let i = 0; i < keys.length - 1; i++) {
                if (!(keys[i] in ref)) ref[keys[i]] = {};
                ref = ref[keys[i]];
            }
            ref[keys[keys.length - 1]] = value;
            return {...prev, guardians};
        });
    };
    const handleAddGuardian = () => {
        setFormState(prev => ({
            ...prev,
            guardians: [
                ...(prev.guardians || []),
                {firstName: '', lastName: '', contact: {email: '', phone: '', note: ''}, note: ''}
            ]
        }));
    };
    const handleRemoveGuardian = (idx: number) => {
        setFormState(prev => ({
            ...prev,
            guardians: (prev.guardians || []).filter((_, i) => i !== idx)
        }));
    };

    const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        onSubmit(formState);
    };

    const handleReset = () => {
        setFormState(formData);
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
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Ulice a číslo"
                            required
                            value={formState.address.streetAndNumber}
                            onChange={e => handleInputChange('address.streetAndNumber', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth
                            label="Město"
                            required
                            value={formState.address.city}
                            onChange={e => handleInputChange('address.city', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={3}>
                        <TextField
                            fullWidth
                            label="PSČ"
                            required
                            value={formState.address.postalCode}
                            onChange={e => handleInputChange('address.postalCode', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={3}>
                        <TextField
                            fullWidth
                            label="Země"
                            required
                            value={formState.address.country}
                            onChange={e => handleInputChange('address.country', e.target.value)}
                        />
                    </Grid>

                    {/* Kontaktní údaje */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>Kontaktní údaje</Typography>
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField
                            fullWidth
                            label="Email"
                            value={formState.contact?.email ?? ''}
                            onChange={e => handleInputChange('contact.email', e.target.value)}
                            type="email"
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField
                            fullWidth
                            label="Telefon"
                            value={formState.contact?.phone ?? ''}
                            onChange={e => handleInputChange('contact.phone', e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField
                            fullWidth
                            label="Poznámka"
                            value={formState.contact?.note ?? ''}
                            onChange={e => handleInputChange('contact.note', e.target.value)}
                        />
                    </Grid>

                    {/* Právní zástupci */}
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>Zákonní zástupci</Typography>
                        <Stack spacing={2}>
                            {(formState.guardians || []).map((g, idx) => (
                                <Paper
                                    key={idx}
                                    variant="outlined"
                                    sx={{p: 2, bgcolor: "#f9f9f9", position: "relative"}}
                                >
                                    <Grid container spacing={2}>
                                        <Grid item xs={12} sm={6}>
                                            <TextField
                                                fullWidth
                                                label="Jméno"
                                                value={g.firstName || ''}
                                                onChange={e =>
                                                    handleGuardianChange(idx, 'firstName', e.target.value)
                                                }
                                                required
                                            />
                                        </Grid>
                                        <Grid item xs={12} sm={6}>
                                            <TextField
                                                fullWidth
                                                label="Příjmení"
                                                value={g.lastName || ''}
                                                onChange={e =>
                                                    handleGuardianChange(idx, 'lastName', e.target.value)
                                                }
                                                required
                                            />
                                        </Grid>
                                        <Grid item xs={12} sm={4}>
                                            <TextField
                                                fullWidth
                                                label="Email"
                                                value={g.contact?.email || ''}
                                                onChange={e =>
                                                    handleGuardianChange(idx, 'contact.email', e.target.value)
                                                }
                                                type="email"
                                            />
                                        </Grid>
                                        <Grid item xs={12} sm={4}>
                                            <TextField
                                                fullWidth
                                                label="Telefon"
                                                value={g.contact?.phone || ''}
                                                onChange={e =>
                                                    handleGuardianChange(idx, 'contact.phone', e.target.value)
                                                }
                                            />
                                        </Grid>
                                        <Grid item xs={12} sm={4}>
                                            <TextField
                                                fullWidth
                                                label="Poznámka"
                                                value={g.contact?.note || ''}
                                                onChange={e =>
                                                    handleGuardianChange(idx, 'contact.note', e.target.value)
                                                }
                                            />
                                        </Grid>
                                        <Grid item xs={12}>
                                            <TextField
                                                fullWidth
                                                label="Poznámka k zástupci"
                                                value={g.note || ''}
                                                onChange={e =>
                                                    handleGuardianChange(idx, 'note', e.target.value)
                                                }
                                            />
                                        </Grid>
                                    </Grid>
                                    <Button
                                        sx={{position: "absolute", top: 10, right: 10}}
                                        size="small"
                                        variant="outlined"
                                        color="secondary"
                                        onClick={() => handleRemoveGuardian(idx)}
                                    >
                                        Odebrat
                                    </Button>
                                </Paper>
                            ))}
                            <Button
                                variant="outlined"
                                onClick={handleAddGuardian}
                                disabled={disabled}
                            >
                                Přidat zástupce
                            </Button>
                        </Stack>
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
    console.log('Is opened: ' + JSON.stringify(open));
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