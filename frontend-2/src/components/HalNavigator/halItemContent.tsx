import {
    type HalFormsTemplate,
    type HalResponse,
    isFormTarget,
    type NavigationTarget,
    type TemplateTarget
} from "../../api";
import type {Navigation} from "../../hooks/useNavigation";
import {type ReactElement, useCallback, useState} from "react";
import {HalActionsUi, HalLinksUi} from "./halActionComponents";
import {type HalFormFieldFactory, HalFormsForm} from "../HalFormsForm";
import {isHalFormsTemplate} from "../HalFormsForm/utils";
import {toHref} from "./hooks";
import {isFormValidationError, submitHalFormsData} from "../../api/hateoas";
import {Alert} from "@mui/material";
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
                    .filter(v => ['_embedded', '_links', '_templates'].indexOf(v[0]) === -1)
                    .map(([attrName, value]) => {
                        return <tr key={attrName}>
                            <td>{attrName}</td>
                            <td>{JSON.stringify(value)}</td>
                        </tr>;
                    })
                }
                </tbody>
            </table>
            {data._templates && <HalActionsUi links={data._templates} onClick={link => navigation.navigate(link)}/>}
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

    const activeTemplate = initTemplate;
    const submitTarget: TemplateTarget = isFormTarget(activeTemplate) && activeTemplate || {
        target: toHref(submitApi),
        method: activeTemplate.method || "POST"
    }

    const submit = useCallback(async (formData: Record<string, any>) => {
        try {
            await submitHalFormsData(submitTarget, formData);
            try {
                afterSubmit();
            } catch (ex) {
                console.error(ex);
            }
        } catch (e) {
            setError(e);
        }
    }, [submitApi, afterSubmit]);

    return (<>
        <HalFormsForm data={initData} template={activeTemplate} onSubmit={submit} fieldsFactory={fieldsFactory}
                      onCancel={onCancel}/>
        {error && <Alert severity={"error"}>{error.message}</Alert>}
        {isFormValidationError(error) && Object.entries(error.validationErrors).map((entry) => <Alert
            severity={"error"}>{entry[0]}:&nbsp;{entry[1]}</Alert>)}
        {isFormValidationError(error) && <JsonPreview data={error.formData} label={"Odeslana data"}/>}
    </>);
}