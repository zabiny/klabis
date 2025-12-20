import {
    type HalFormsTemplate,
    type HalResponse,
    isFormTarget,
    type NavigationTarget,
    type TemplateTarget
} from "../../api";
import type {Navigation} from "../../hooks/useNavigation";
import {type ReactElement, useCallback, useEffect, useState} from "react";
import {HalActionsUi, HalLinksUi} from "./halActionComponents";
import {type HalFormFieldFactory, HalFormsForm} from "../HalFormsForm";
import {isHalFormsTemplate} from "../HalFormsForm/utils";
import {toHref} from "./hooks";
import {isFormValidationError, submitHalFormsData} from "../../api/hateoas";
import {Alert, Spinner} from "../UI";
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
        {submitSuccess && !isNavigating && (
            <Alert
                severity="success"
                className="mb-4"
            >
                Úspěšně uloženo
            </Alert>
        )}

        {/* Navigation feedback */}
        {isNavigating && (
            <div
                className="flex items-center gap-2 py-2 text-blue-600 dark:text-blue-400 mb-4"
                role="status"
                aria-live="polite"
                aria-label="Probíhá přesměrování na další stránku"
            >
                <Spinner size="sm" aria-hidden="true"/>
                <span>Přesměrovávám...</span>
            </div>
        )}

        {/* Validation errors consolidated at top */}
        {hasValidationErrors && (
            <Alert
                severity="error"
                className="mb-4"
            >
                <div>
                    <strong>Formulář obsahuje chyby:</strong>
                    <ul className="my-2 ml-6 list-disc">
                        {Object.entries(validationErrors).map(([field, message]) => (
                            <li key={field}>{field}: {String(message)}</li>
                        ))}
                    </ul>
                </div>
            </Alert>
        )}

        {/* Generic error message */}
        {genericError && (
            <Alert
                severity="error"
                className="mb-4"
            >
                <div>
                    <strong>Nepodařilo se uložit změny. Zkuste to prosím znovu.</strong>
                    <div className="text-sm mt-1">
                        {error?.message}
                    </div>
                </div>
            </Alert>
        )}

        {/* Form */}
        <div className={isNavigating ? 'opacity-50 pointer-events-none' : ''}>
            <HalFormsForm
                data={initData}
                template={activeTemplate}
                onSubmit={submit}
                fieldsFactory={fieldsFactory}
                onCancel={!isSubmitting && !isNavigating ? onCancel : undefined}
                isSubmitting={isSubmitting}
            />
        </div>

        {/* Debug info */}
        {isFormValidationError(error) && <JsonPreview data={error.formData} label={"Odeslana data"}/>}
    </>);
}