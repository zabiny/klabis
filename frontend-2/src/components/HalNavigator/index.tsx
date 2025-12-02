import {
    type HalFormsResponse,
    type HalFormsTemplate,
    type HalResponse,
    isFormTarget,
    isTemplateTarget,
    type Link,
    type TemplateTarget
} from "../../api";
import {type ReactElement, useCallback, useEffect, useState} from "react";
import {Alert, Button, Checkbox, FormLabel, Grid, Link as MuiLink, Stack} from "@mui/material";
import {ErrorBoundary} from "react-error-boundary";
import {type HalFormFieldFactory, HalFormsForm} from "../HalFormsForm";
import {UserManager} from "oidc-client-ts";
import {klabisAuthUserManager} from "../../api/klabisUserManager";
import {type Navigation, useNavigation} from "../../hooks/useNavigation";
import {JsonPreview} from "../JsonPreview";
import {getDefaultTemplate, isHalFormsResponse, isHalFormsTemplate} from "../HalFormsForm/utils";
import {isFormValidationError, submitHalFormsData} from "../../api/hateoas";
import {isLink} from "../../api/klabisJsonUtils";


const userManager: UserManager = klabisAuthUserManager;

// Generic HAL fetcher
async function fetchResource(url: string) {
    const user = await userManager.getUser();
    const res = await fetch(url, {
        headers: {
            Accept: "application/prs.hal-forms+json,application/hal+json",
            "Authorization": `Bearer ${user?.access_token}`
        },
    });
    if (!res.ok) {
        if (res.body) {
            const bodyText = await res.text();
            console.warn(bodyText ? `Response body: ${bodyText}` : 'No response body');
        }
        throw new Error(`HTTP ${res.status}`);
    }
    return res.json();
}

function HalLinksUi({links, onClick}: {
    links: Record<string, NavigationTarget>,
    onClick: (link: NavigationTarget) => void
}): ReactElement {
    return (
        <Stack direction={"row"} spacing={2}>
            {Object.entries(links).map(([rel, link]) => {
                if (rel === "self") return null;
                const singleLink = Array.isArray(link) ? link[0] : link;
                return (
                    <MuiLink key={rel}
                             onClick={() => onClick(singleLink)}>{singleLink.title || singleLink.name || rel}</MuiLink>
                );
            })}
        </Stack>
    );
}

function omitMetadataAttributes<T extends { _links?: any }>(obj: T): Omit<T, '_links'> {
    const {_links, ...rest} = obj;
    return rest;
}

const COLLECTION_LINK_RELS = ["prev", "next", "last", "first"];

function HalCollectionContent({data, navigation}: {
    data: HalResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {

    // TODO: split links into collection links and other links. Display collection links "bellow" table and other links above table as actions.

    return (
        <>
            {data._links && <HalLinksUi links={data._links} onClick={link => navigation.navigate(link)}/>}

            {data._embedded && Object.entries(data._embedded).map(([rel, items]) => (
                    <div key={rel}>
                        <h2 className="font-semibold">{rel}</h2>
                        <ul className="list-disc list-inside">
                            {(Array.isArray(items) ? items : [items]).map((item, idx) => (
                                <li key={idx}>
                                    {JSON.stringify(omitMetadataAttributes(item))}
                                    {item._links?.self && (
                                        <Button
                                            className="ml-2 px-2 py-0.5 text-sm bg-gray-300 rounded"
                                            onClick={() => navigation.navigate(item._links.self)}
                                        >
                                            Open
                                        </Button>
                                    )}
                                </li>
                            ))}
                        </ul>
                    </div>
                )
            )
            }</>)

}

function HalItemContent({data, navigation}: {
    data: HalResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {
    return (
        <>
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
            {data._links && <HalLinksUi links={data._links} onClick={link => navigation.navigate(link)}/>}
            {data._templates && <HalLinksUi links={data._templates} onClick={link => navigation.navigate(link)}/>}
        </>);
}


function HalContent({data, navigation}: {
    data: HalResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {

    if (data.page === undefined) {
        return <HalItemContent data={data} navigation={navigation}/>
    } else {
        return <HalCollectionContent data={data} navigation={navigation}/>
    }
}

type NavigationTarget = Link | TemplateTarget | string;

function HalEditableItemContent({
                                    initData, fieldsFactory, navigation
                                }: {
    initData: HalFormsResponse,
    navigation: Navigation<NavigationTarget>,
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {

    if (isHalFormsTemplate(navigation.current)) {
        return <HalFormsContent initData={initData} submitApi={navigation.current} fieldsFactory={fieldsFactory}
                                initTemplate={navigation.current} afterSubmit={() => navigation.back()}
                                onCancel={() => navigation.back()}/>;
    } else {
        return <HalItemContent data={initData} navigation={navigation}/>;
    }
}

function HalFormsContent({
                             submitApi, initTemplate, initData, fieldsFactory, onCancel, afterSubmit = () => {
    }
                         }: {
    submitApi: NavigationTarget,
    initTemplate?: HalFormsTemplate,
    initData: HalFormsResponse,
    afterSubmit?: () => void,
    onCancel?: () => void,
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {
    const [error, setError] = useState<Error>();

    const activeTemplate = initTemplate || getDefaultTemplate(initData);
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
        {isFormValidationError(error) && Object.entries(error.validationErrors).map((entry, message) => <Alert
            severity={"error"}>{entry[0]}:&nbsp;{entry[1]}</Alert>)}
        {isFormValidationError(error) && <JsonPreview data={error.formData} label={"Odeslana data"}/>}
    </>);
}

function isCollectionContent(data: HalResponse): boolean {
    return (data?.page !== undefined);
}

function isSingleItemContent(data: HalResponse): boolean {
    return !isCollectionContent(data);
}

function HalNavigatorContent({
                                 fieldsFactory, navigation
                             }: {
    navigation: Navigation<NavigationTarget>
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {
    const api = navigation.current;
    const {data, isLoading, error} = useSimpleFetch(api);
    const [showSource, setShowSource] = useState(true);
    if (isLoading) {
        return <Alert severity={"info"}>Nahravam data {toLink(api).href}</Alert>;
    }

    if (error) {
        console.table(error.stack);
        return <Alert severity={"error"}>Nepovedlo se nacist data {toLink(api).href}: {error.message}</Alert>;
    }

    function renderContent(item: any): ReactElement {
        if (isCollectionContent(item) || !isHalFormsResponse(item)) {
            return <HalContent data={item} navigation={navigation}/>;
        } else if (isHalFormsResponse(item)) {
            return <HalEditableItemContent initData={item} navigation={navigation} fieldsFactory={fieldsFactory}/>
        } else {
            return <JsonPreview data={item} label={"Neznamy format dat (ocekavam HAL+FORMS nebo HAL)"}/>
        }
    }

    return (<Grid container spacing={2} sx={{
        justifyContent: "space-between",
        alignItems: "baseline",
    }}>
        <Grid padding={2} xs={7}>
            <ErrorBoundary fallback={<JsonPreview label={"Nelze vyrenderovat Hal/HalForms obsah"} data={api}/>}>
                {renderContent(data)}
            </ErrorBoundary>
        </Grid>
        <Grid overflow={showSource ? "scroll" : "none"} xs={5}>
            <FormLabel>Zobraz zdrojovy JSON:<Checkbox checked={showSource}
                                                      onChange={(event, checked) => setShowSource(checked)}>Zdrojovy
                JSON</Checkbox></FormLabel>
            {showSource && <JsonPreview data={data} label={"Response data"}/>}
        </Grid>
    </Grid>);
}

function toLink(item: NavigationTarget): Link {
    if (isLink(item)) {
        return item;
    } else if (isTemplateTarget(item)) {
        return {href: item.target};
    } else {
        return {href: item};
    }
}

const useSimpleFetch = (resource: NavigationTarget): {
    data?: HalResponse,
    isLoading: boolean,
    error?: Error
} => {
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState();
    const [error, setError] = useState<Error>();

    const loadData = useCallback(async (link: NavigationTarget) => {
        setLoading(true);
        setError(undefined);
        setData(undefined);
        try {
            const response = await fetchResource(toHref(link));
            setData(response);
        } catch (e) {
            if (e instanceof Error) {
                setError(e);
            } else {
                setError(new Error("Unknown error " + e))
            }
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (resource) {
            loadData(resource);
        }
    }, [resource, loadData]);

    return {isLoading: loading, data, error};
}

function toHref(source: NavigationTarget): string {
    if (isTemplateTarget(source)) {
        if (!source.target) {
            throw new Error("Chybi hodnota target attributu v TemplateTarget instanci (" + JSON.stringify(source) + ")")
        }
        return source.target;
    } else if (isLink(source)) {
        if (!source.href) {
            throw new Error("Chybi hodnota href attributu v Link instanci (" + JSON.stringify(source) + ")")
        }
        return source.href
    } else {
        return source;
    }
}


export function HalNavigatorPage({
                                     startUrl,
                                     fieldsFactory
                                 }: {
    startUrl: Link | string,
    fieldsFactory?: HalFormFieldFactory
}) {
    const navigation = useNavigation<NavigationTarget>(startUrl);

    const renderNavigation = (): ReactElement => {
        console.log(startUrl);
        return (<Stack direction={"row"}>
            <Button onClick={navigation.reset}>Restart</Button>
            <Button disabled={navigation.isFirst} onClick={navigation.back}>Zpět</Button>
            <h3>{toHref(navigation.current)}</h3>
        </Stack>);
    }

    return (
        <div className="p-4 space-y-4">

            {renderNavigation()}

            <ErrorBoundary
                fallback={<JsonPreview data={navigation.current} label={"Nejde vyrenderovat HAL FORMS form"}/>}
                resetKeys={[navigation.current]}>
                <HalNavigatorContent api={navigation.current}
                                     fieldsFactory={fieldsFactory}
                                     navigation={navigation}
                />
            </ErrorBoundary>

        </div>
    );
}