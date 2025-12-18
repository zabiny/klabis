import {
    type HalFormsTemplate,
    type HalResponse,
    isFormTarget,
    type NavigationTarget,
    type TemplateTarget
} from "../../api";
import type {Navigation} from "../../hooks/useNavigation";
import {type ReactElement, useCallback, useState, useEffect} from "react";
import {HalActionsUi, HalLinksUi} from "./halActionComponents";
import {type HalFormFieldFactory, HalFormsForm} from "../HalFormsForm";
import {isHalFormsTemplate} from "../HalFormsForm/utils";
import {toHref} from "./hooks";
import {isFormValidationError, submitHalFormsData} from "../../api/hateoas";
import {Alert, Snackbar, CircularProgress, Box} from "@mui/material";
import {JsonPreview} from "../JsonPreview";


export function HalEditableItemContent({
                                           initData, fieldsFactory, navigation
                                       }: {
    initData: HalResponse,
    navigation: Navigation<NavigationTarget>,
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {

    const {current, back} = navigation;

    if (isHalFormsTemplate(current)) {
        return <HalFormsContent initData={initData} submitApi={current} fieldsFactory={fieldsFactory}
                                initTemplate={current} afterSubmit={() => back()}
                                onCancel={() => back()}/>;
    } else {
        return <HalItemContent data={initData} navigation={navigation}/>;
    }
}

function HalItemContent({data, navigation}: {
    data: HalResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {
    return (
        <>
            {data._links && <HalLinksUi links={data._links} onClick={link => navigation.navigate(link)}/>}
            <table>
                <thead>
                <tr>
                    <th>Attribut</th>
                    <th>Hodnota</th>
                </tr>
                </thead>
                <tbody>
                {Object.entries(data)
                    .filter(v => !['_embedded', '_links', '_templates'].includes(v[0]))
                    .map(([attrName, value]) => {
                        return <tr key={attrName}>
                            <td>{attrName}</td>
                            <td>{JSON.stringify(value)}</td>
                        </tr>;
                    })
                }
                </tbody>
            </table>
            {data._templates && <HalActionsUi links={data._templates as Record<string, HalFormsTemplate>}
                                              onClick={link => navigation.navigate(link)}/>}
        </>);
}


function HalFormsContent({
                             submitApi, initTemplate, initData, fieldsFactory, onCancel, afterSubmit = () => {
    }
                         }: {
    submitApi: NavigationTarget,
    initTemplate: HalFormsTemplate,
    initData: HalResponse,
    afterSubmit?: () => void,
    onCancel?: () => void,
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {
    const [error, setError] = useState<Error>();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitSuccess, setSubmitSuccess] = useState(false);
    const [isNavigating, setIsNavigating] = useState(false);

    useEffect(() => {
        if (submitSuccess) {
            const timer = setTimeout(() => {
                setIsNavigating(true);
                try {
                    afterSubmit();
                } catch (ex) {
                    console.error(ex);
                }
            }, 1500);

            return () => clearTimeout(timer);
        }
    }, [submitSuccess, afterSubmit]);

    const activeTemplate = initTemplate;
    const submit = useCallback(async (formData: Record<string, unknown>) => {
        setError(undefined);
        setIsSubmitting(true);

        const submitTarget: TemplateTarget = isFormTarget(activeTemplate) && activeTemplate || {
            target: toHref(submitApi),
            method: activeTemplate.method || "POST"
        }

        try {
            await submitHalFormsData(submitTarget, formData);
            setSubmitSuccess(true);
        } catch (e) {
            setError((e instanceof Error) ? e : new Error(String(e)));
        } finally {
            setIsSubmitting(false);
        }
    }, [submitApi, activeTemplate]);

    const validationErrors = isFormValidationError(error) ? error.validationErrors : {};
    const hasValidationErrors = Object.keys(validationErrors).length > 0;
    const genericError = error && !isFormValidationError(error);

    return (<>
        {/* Success message */}
        <Snackbar
            open={submitSuccess && !isNavigating}
            autoHideDuration={6000}
            onClose={() => setSubmitSuccess(false)}
            aria-label="Zpráva o úspěšném uložení"
        >
            <Alert
                severity="success"
                sx={{width: '100%'}}
                role="status"
                aria-live="polite"
                aria-label="Změny byly úspěšně uloženy"
            >
                Úspěšně uloženo
            </Alert>
        </Snackbar>

        {/* Navigation feedback */}
        {isNavigating && (
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    py: 2,
                    color: 'info.main'
                }}
                role="status"
                aria-live="polite"
                aria-label="Probíhá přesměrování na další stránku"
            >
                <CircularProgress size={20} aria-hidden="true"/>
                <span>Přesměrovávám...</span>
            </Box>
        )}

        {/* Validation errors consolidated at top */}
        {hasValidationErrors && (
            <Alert
                severity="error"
                sx={{mb: 2}}
                role="alert"
                aria-live="assertive"
                aria-label="Formulář obsahuje chyby, prosím opravte jednotlivá pole"
            >
                <strong>Formulář obsahuje chyby:</strong>
                <ul style={{margin: '0.5rem 0', paddingLeft: '1.5rem'}}>
                    {Object.entries(validationErrors).map(([field, message]) => (
                        <li key={field}>{field}: {String(message)}</li>
                    ))}
                </ul>
            </Alert>
        )}

        {/* Generic error message */}
        {genericError && (
            <Alert
                severity="error"
                sx={{mb: 2}}
                role="alert"
                aria-live="assertive"
                aria-label="Chyba při ukládání"
            >
                <strong>Nepodařilo se uložit změny. Zkuste to prosím znovu.</strong>
                <div style={{fontSize: '0.875rem', marginTop: '0.5rem'}}>
                    {error?.message}
                </div>
            </Alert>
        )}

        {/* Form */}
        <Box sx={{opacity: isNavigating ? 0.5 : 1, pointerEvents: isNavigating ? 'none' : 'auto'}}>
            <HalFormsForm
                data={initData}
                template={activeTemplate}
                onSubmit={submit}
                fieldsFactory={fieldsFactory}
                onCancel={!isSubmitting && !isNavigating ? onCancel : undefined}
                isSubmitting={isSubmitting}
            />
        </Box>

        {/* Debug info */}
        {isFormValidationError(error) && <JsonPreview data={error.formData} label={"Odeslana data"}/>}
    </>);
}