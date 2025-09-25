import {type KlabisApiFormProperties} from "./KlabisForm.types";
import {useApiPutMutation, useApiQuery} from "../../hooks/useApi";
import {Alert, Typography} from "@mui/material";

const FormLoadingUI = () => <Typography>Načítání...</Typography>;

export const KlabisApiForm = ({
                                  apiPath, form: FormComponent, onSuccess = () => {
    }
                              }: KlabisApiFormProperties) => {
    const {data: formData, isLoading, isError: isLoadingError, error: loadingError} = useApiQuery([], apiPath);

    const mutation = useApiPutMutation(apiPath);

    if (isLoading) return (<FormLoadingUI/>);
    if (mutation.isError || isLoadingError) return (
        <p>Error:{loadingError?.message || mutation.error?.message || ''}</p>);
    if (formData === null) return (<p>Form API returned no data</p>);

    const handleSubmit = async (formData: unknown) => {
        await mutation.mutateAsync(formData);
        try {
            onSuccess()
        } catch (callbackErr) {
            console.error("Failed onSuccess callback: " + callbackErr);
        }
    };

    return <>
        {mutation.isSuccess && (
            <Alert severity="success" sx={{mb: 2}}>Změny byly úspěšně uloženy</Alert>
        )}

        {mutation.isError && (
            <Alert severity="error" sx={{mb: 2}}>Při ukládání došlo k chybě</Alert>
        )}


        <FormComponent formData={formData} onSubmit={handleSubmit}/>
    </>;
};