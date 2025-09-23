import React from 'react';
import {Alert, Button, Checkbox, FormControlLabel, Grid, Paper, Stack, TextField, Typography,} from '@mui/material';
import {type EditMyDetailsForm, useGetEditMyDetailsForm, useUpdateMyDetails} from '../api/membersApi.ts';
import {Form, Formik, type FormikHandlers} from "formik";
import * as Yup from 'yup';

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

    /// https://formik.org/docs/examples/with-material-ui
    interface FormikMuiProps {
        name: string,
        value: any,
        onChange: any,
        onBlur: any,
        error: boolean,
        helperText?: string,
        checked: boolean
    }

    // TODO: shall we use Formik MUI integration instead? - https://stackworx.github.io/formik-mui/docs/api/mui/
    const getMuiProps = (fieldName: string, formik: FormikHandlers): FormikMuiProps => {
        const meta = formik.getFieldMeta(fieldName);

        return {
            name: fieldName,
            value: meta.value,
            onChange: formik.handleChange,
            onBlur: formik.handleBlur,
            error: meta.touched && !!meta.error,
            helperText: meta.touched && meta.error || undefined,
            checked: !!meta.value || false
        };
    }

    const validationSchema = Yup.object().shape(
        {
            nationality: Yup.string().required().min(2).max(2),
            address: Yup.object().shape({
                postalCode: Yup.string().required().min(5).max(5)
            })
        }
    );

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

            <Formik initialValues={formData} onSubmit={onSubmit} validationSchema={validationSchema}>
                {formik =>
                    <Form>
                        <Grid container spacing={3}>
                            <Grid item xs={12}>
                                <TextField fullWidth required
                                           label="Narodnost" {...getMuiProps('nationality', formik)}/>
                            </Grid>

                            <Grid item xs={12}>
                                <Typography variant="h6" gutterBottom>
                                    Adresa
                                </Typography>
                            </Grid>
                            <Grid item xs={12}>
                                <TextField fullWidth required
                                           label="Ulice" {...getMuiProps('address.streetAndNumber', formik)}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth required label="Město" {...getMuiProps('address.city', formik)}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth required
                                           label="PSČ" {...getMuiProps('address.postalCode', formik)}/>
                            </Grid>


                            {/* Kontakt */}
                            <Grid item xs={12}>
                                <Typography variant="h6" gutterBottom>
                                    Kontaktní údaje
                                </Typography>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth required label="Email" {...getMuiProps('contact.email', formik)}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth required
                                           label="Telefon" {...getMuiProps('contact.phone', formik)}/>
                            </Grid>

                            {/* Ostatní údaje */}
                            <Grid item xs={12}>
                                <Typography variant="h6" gutterBottom>
                                    Další údaje
                                </Typography>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth required label="Číslo SI čipu" {...getMuiProps('siCard', formik)}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth required
                                           label="Bankovní účet" {...getMuiProps('bankAccount', formik)}/>
                            </Grid>
                            <Grid item xs={12}>
                                <TextField fullWidth required
                                           label="Dietní omezení" {...getMuiProps('dietaryRestrictions', formik)}
                                           rows={2}/>
                            </Grid>
                            <Grid item xs={12}>
                                <FormControlLabel
                                    control={
                                        <Checkbox {...getMuiProps('medicCourse', formik)}/>
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
                                        disabled={formik.isSubmitting}>Uložit změny</Button>
                                    <Button
                                        type="reset"
                                        variant="outlined"
                                        color="secondary"
                                        disabled={formik.isSubmitting}>Obnovit</Button>
                                </Stack>
                            </Grid>
                        </Grid>
                    </Form>
                }
            </Formik>
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