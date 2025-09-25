import {useEffect, useState} from "react";
import {type KlabisApiFormProperties, KlabisQueryFormProperties} from "./KlabisForm.types";
import {useKlabisApiClient} from "../hooks/useApi";
import {Alert, Typography} from "@mui/material";

const FormLoadingUI = () => <Typography>Načítání...</Typography>;

export const KlabisApiForm = ({
                                  apiPath, form: FormComponent, onSuccess = () => {
    }
                              }: KlabisApiFormProperties) => {
    const [data, setData] = useState<unknown | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const klabisAxios = useKlabisApiClient();

    useEffect(() => {
        let cancelled = false;
        const fetchData = async () => {
            try {
                const response = await klabisAxios.get<unknown>(apiPath);
                if (!cancelled) {
                    setData(response.data);
                }
            } catch (err) {
                if (!cancelled) {
                    setError((err as Error).message);
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        };
        fetchData();
        return () => {
            cancelled = true;
        };
    }, [apiPath, klabisAxios]);

    if (loading) return (<FormLoadingUI/>);
    if (error) return (<p>Error:
        {
            error
        }
    </p>);
    if (data === null) return (<p>Form API returned no data</p>);

    const handleSubmit = async (formData: unknown) => {
        try {
            await klabisAxios.put(apiPath, formData);
            try {
                onSuccess()
            } catch (callbackErr) {
                console.error("Failed onSuccess callback: " + callbackErr);
            }
        } catch (err) {
            setError('Submit error: ' + err);
            throw err;
        }
    };

    return <FormComponent formData={data} onSubmit={handleSubmit}/>;
};


export const KlabisQueryForm = <T, >({
                                         form: FormComponent, useGetData, useMutateData, onSuccess = () => {
    }
                                     }: KlabisQueryFormProperties<T>) => {
    const {data: formData, isLoading} = useGetData();

    const mutation = useMutateData();

    const handleSubmit = async (e: T) => {
        try {
            await mutation.mutateAsync(e);
            try {
                onSuccess();
            } catch (successErr) {
                console.error('Chyba onSuccess callback:' + successErr);
            }
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


            <FormComponent formData={formData} onSubmit={handleSubmit}/>
        </>
    );
};