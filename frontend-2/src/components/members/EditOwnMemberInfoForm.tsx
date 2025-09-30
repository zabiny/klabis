import {Alert, Button, FormControlLabel, Grid, Paper, Stack, Typography,} from '@mui/material';
import {Checkbox, TextField} from 'formik-mui';
import {Field, Form, Formik, type FormikHelpers} from "formik";
import * as Yup from 'yup';
import {type KlabisFormProperties} from "../KlabisForm";
import {type EditMyDetailsForm, useKlabisApiMutation, useKlabisApiQuery} from "../../api";

const FormLoadingUI = () => <Typography>Načítání...</Typography>;

export const EditMemberFormUI = ({
                                     formData, onSubmit
                                 }: KlabisFormProperties<EditMyDetailsForm>) => {

    const validationSchema = Yup.object().shape(
        {
            nationality: Yup.string().required().min(2).max(2),
            address: Yup.object().shape({
                postalCode: Yup.string().required().min(5).max(5)
            })
        }
    );

    const formikSubmit = async (values: EditMyDetailsForm, helpers: FormikHelpers<EditMyDetailsForm>): Promise<void> => {
        await onSubmit(values);
        helpers.setSubmitting(false);
    }

    return (
        <Paper sx={{p: 3}}>
            <Typography variant="h5" gutterBottom>
                Úprava osobních údajů
            </Typography>

            <Formik<EditMyDetailsForm> initialValues={formData} onSubmit={formikSubmit}
                                       validationSchema={validationSchema}>
                {formik =>
                    <Form>
                        <Grid container spacing={3}>
                            <Grid item xs={12}>
                                <Field name="nationality" label="Národnost" fullWidth component={TextField}/>
                            </Grid>

                            <Grid item xs={12}>
                                <Typography variant="h6" gutterBottom>
                                    Adresa
                                </Typography>
                            </Grid>
                            <Grid item xs={12}>
                                <Field name="address.streetAndNumber" fullWidth label="Ulice" component={TextField}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <Field name="address.city" label="Národnost" fullWidth component={TextField}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <Field name="address.postalCode" label="PSČ" fullWidth component={TextField}/>
                            </Grid>


                            {/* Kontakt */}
                            <Grid item xs={12}>
                                <Typography variant="h6" gutterBottom>
                                    Kontaktní údaje
                                </Typography>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <Field name="contact.email" label="Email" fullWidth component={TextField}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <Field name="contact.phone" label="Telefon" fullWidth component={TextField}/>
                            </Grid>

                            {/* Ostatní údaje */}
                            <Grid item xs={12}>
                                <Typography variant="h6" gutterBottom>
                                    Další údaje
                                </Typography>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <Field name="siCard" label="Číslo SI čipu" fullWidth component={TextField}/>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <Field name="bankAccount" label="Bankovní účet" fullWidth component={TextField}/>
                            </Grid>
                            <Grid item xs={12}>
                                <Field name="dietaryRestrictions" label="Dietní omezení" fullWidth rows="2"
                                       component={TextField}/>
                            </Grid>
                            <Grid item xs={12}>
                                <FormControlLabel
                                    control={
                                        <Field type="checkbox" name="medicCourse" component={Checkbox}/>
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
        const {
            data: formData,
            isLoading
        } = useKlabisApiQuery("get", "/members/{memberId}/editOwnMemberInfoForm", {params: {path: {memberId: memberId}}});

        const mutation = useKlabisApiMutation("put", "/members/{memberId}/editOwnMemberInfoForm");

    const handleSubmit = async (e: EditMyDetailsForm) => {
        try {
            await mutation.mutateAsync({body: e, params: {path: {memberId: memberId}}});
        } catch (error) {
            console.error('Chyba při ukládání:', error);
        }
    };

    if (isLoading) {
        return <FormLoadingUI/>;
    }

    return (
        <>
            {mutation.isSuccess && (
                <Alert severity="success" sx={{mb: 2}}>Změny byly úspěšně uloženy</Alert>
            )}

            {mutation.isError && (
                <Alert severity="error" sx={{mb: 2}}>Při ukládání došlo k chybě</Alert>
            )}


            <EditMemberFormUI
                formData={formData?.data}
                onSubmit={handleSubmit}
            />
        </>
    );
    }
;

export default EditMemberForm;