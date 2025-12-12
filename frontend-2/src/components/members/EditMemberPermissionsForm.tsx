import {useState} from 'react';
import {Alert, Button, Checkbox, FormControlLabel, FormGroup, Grid, Paper, Stack, Typography,} from '@mui/material';
import {
    type GetAllGrantsResponse,
    type GetMemberGrantsResponse,
    useKlabisApiMutation,
    useKlabisApiQuery
} from "../../api";

type GrantType = string;

interface EditMemberPermissionsFormUIProps {
    allGrants: GetAllGrantsResponse;
    memberGrants: GetMemberGrantsResponse;
    onSubmit: (formData: GetMemberGrantsResponse) => void;
    successMessage: string | null;
    failureMessage: string | null;
    disabled?: boolean;
}

const FormLoadingUI = () => <Typography>Načítání...</Typography>;

const EditMemberPermissionsFormUI = ({
                                         allGrants,
                                         memberGrants,
                                         onSubmit,
                                         successMessage,
                                         failureMessage,
                                         disabled = false,
                                     }: EditMemberPermissionsFormUIProps) => {
    const [selectedGrants, setSelectedGrants] = useState<string[]>(memberGrants.grants || []);

    const handleGrantChange = (grant: GrantType, checked: boolean) => {
        setSelectedGrants(prev => {
            if (checked) {
                return [...prev.filter(g => g !== grant), grant];
            } else {
                return prev.filter(g => g !== grant);
            }
        });
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        onSubmit({
            ...memberGrants,
            grants: selectedGrants
        });
    };

    const handleReset = () => {
        setSelectedGrants(memberGrants.grants || []);
    };

    return (
        <Paper sx={{p: 3}}>
            {successMessage && (
                <Alert severity="success" sx={{mb: 2}}>{successMessage}</Alert>
            )}

            {failureMessage && (
                <Alert severity="error" sx={{mb: 2}}>{failureMessage}</Alert>
            )}

            <form onSubmit={handleSubmit}>
                <Grid container spacing={3}>
                    <Grid item xs={12}>
                        <Typography variant="h6" gutterBottom>
                            Dostupná oprávnění
                        </Typography>
                        <FormGroup>
                            {allGrants.grants?.map((grantDetail) => (
                                <FormControlLabel
                                    key={grantDetail.grant}
                                    control={
                                        <Checkbox
                                            checked={grantDetail.grant && selectedGrants.includes(grantDetail.grant)}
                                            onChange={(e) => handleGrantChange(grantDetail.grant, e.target.checked)}
                                            disabled={disabled}
                                        />
                                    }
                                    label={
                                        <div>
                                            <Typography variant="body1" component="span">
                                                {grantDetail.grant}
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary" component="div">
                                                {grantDetail.description}
                                            </Typography>
                                        </div>
                                    }
                                />
                            ))}
                        </FormGroup>
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

interface EditMemberPermissionsFormProps {
    userId: number;
}

const EditMemberPermissionsForm = ({userId}: EditMemberPermissionsFormProps) => {
    const {data: allGrants, isLoading: isLoadingGrants} = useKlabisApiQuery("get", "/grants");
    const {
        data: memberGrants,
        isLoading: isLoadingMemberGrants
    } = useKlabisApiQuery("get", "/users/{userId}/changeGrantsForm", {params: {path: {userId: userId}}});

    const mutation = useKlabisApiMutation("put", "/users/{userId}/changeGrantsForm");

    const handleSubmit = async (formData: GetMemberGrantsResponse) => {
        try {
            // TODO: invalidate related queries
            await mutation.mutateAsync({body: formData, params: {path: {userId: userId}}});
        } catch (error) {
            console.error('Chyba při ukládání oprávnění:', error);
        }
    };

    if (isLoadingGrants || isLoadingMemberGrants) {
        return <FormLoadingUI/>;
    }

    if (!allGrants || !memberGrants) {
        return <Typography>Nepodařilo se načíst data</Typography>;
    }

    return (
        <EditMemberPermissionsFormUI
            allGrants={allGrants}
            memberGrants={memberGrants}
            onSubmit={handleSubmit}
            successMessage={mutation.isSuccess ? 'Oprávnění byla úspěšně aktualizována' : null}
            failureMessage={mutation.isError ? 'Při ukládání oprávnění došlo k chybě' : null}
            disabled={mutation.isPending}
        />
    );
};

export default EditMemberPermissionsForm;
