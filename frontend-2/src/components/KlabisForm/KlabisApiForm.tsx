import {type KlabisApiFormProperties} from "./KlabisForm.types";
import {Alert, Typography} from "@mui/material";
import {useKlabisApiMutation, useKlabisApiQuery} from "../../api";

const FormLoadingUI = () => <Typography>Načítání...</Typography>;

export const KlabisApiForm = ({
                                  apiPath, pathParams, form: FormComponent, onSuccess = () => {
    }
                              }: KlabisApiFormProperties) => {
    const {
        data: formData,
        isLoading,
        isError: isLoadingError,
        error: loadingError
    } = useKlabisApiQuery("get", apiPath, {params: {path: {...pathParams}}});   // TODO: more precise pathParams type ... it should somehow be possible..

    // TODO: need way how to reset loaded data what were changed (property with key which shall be reset after it's updated?)
    const mutation = useKlabisApiMutation("put", apiPath);

    if (isLoading) return (<FormLoadingUI/>);
    if (mutation.isError || isLoadingError) return (
        <p>Error:{loadingError?.message || mutation.error?.message || ''}</p>);
    if (formData === null) return (<p>Form API returned no data</p>);

    const handleSubmit = async (formData: unknown) => {
        await mutation.mutateAsync({body: formData, pathParams: pathParams});
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