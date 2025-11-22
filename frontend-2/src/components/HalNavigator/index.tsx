import {
    type HalFormsResponse,
    type HalFormsTemplate,
    type HalResponse,
    type Link,
    type TemplateTarget
} from "../../api";
import {type ReactElement, useCallback, useEffect, useState} from "react";
import {Alert, Button, Checkbox, FormLabel, Grid, Link as MuiLink, Stack} from "@mui/material";
import {ErrorBoundary} from "react-error-boundary";
import {type HalFormFieldFactory, HalFormsForm} from "../HalFormsForm";
import {UserManager} from "oidc-client-ts";
import {klabisAuthUserManager} from "../../api/klabisUserManager";
import {useNavigation} from "../../hooks/useNavigation";
import {JsonPreview} from "../JsonPreview";
import {isHalFormsResponse, isHalResponse} from "../HalFormsForm/utils";
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

function HalLinksUi({links, onClick}: { links: Record<string, Link>, onClick: (link: Link) => void }): ReactElement {
    return (
        <Stack direction={"row"} spacing={2}>
            {Object.entries(links).map(([rel, link]) => {
                if (rel === "self") return null;
                const simpleLink = Array.isArray(link) ? link[0] : link;
                return (
                    <MuiLink key={rel} onClick={() => onClick(simpleLink)}>{simpleLink.name || rel}</MuiLink>
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

function HalCollectionContent({data, navigate}: {
    data: HalResponse,
    navigate: (link: Link) => void
}): ReactElement {

    // TODO: split links into collection links and other links. Display collection links "bellow" table and other links above table as actions.

    return (
        <>
            {data._links && <HalLinksUi links={data._links} onClick={navigate}/>}

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
                                            onClick={() => navigate(item._links.self)}
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

function HalItemContent({data, navigate}: {
    data: HalResponse,
    navigate: (link: Link) => void
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
            {data._links && <HalLinksUi links={data._links} onClick={navigate}/>}
        </>);
}


function HalContent({data, navigate}: {
    data: HalResponse,
    navigate: (link: Link) => void
}): ReactElement {

    if (data.page === undefined) {
        return <HalItemContent data={data} navigate={navigate}/>
    } else {
        return <HalCollectionContent data={data} navigate={navigate}/>
    }
}

function isTemplateTarget(item: any): item is TemplateTarget {
    return item && item.target;
}

function isFormTarget(item: any): item is TemplateTarget {
    return isTemplateTarget(item) && ['POST', 'PUT', 'DELETE', 'PATCH'].indexOf(item.method) !== -1;
}

type NavigationTarget = Link | TemplateTarget | string;


function HalFormsContent({
                             submitApi, initTemplate, initData, fieldsFactory, afterSubmit = () => {
    }
                         }: {
    submitApi: NavigationTarget,
    initTemplate?: HalFormsTemplate,
    initData: HalFormsResponse,
    afterSubmit?: () => void,
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {
    const [error, setError] = useState<Error>();

    const activeTemplate = initTemplate || initData._templates.default;

    const submit = useCallback(async (formData: Record<string, any>) => {
        const target: TemplateTarget = {target: toHref(submitApi), method: activeTemplate.method || "POST"};
        try {
            await submitHalFormsData(target, formData);
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
        <HalFormsForm data={initData} template={activeTemplate} onSubmit={submit} fieldsFactory={fieldsFactory}/>
        {error && <Alert severity={"error"}>{error.message}</Alert>}
        {isFormValidationError(error) && Object.entries(error.validationErrors).map((entry, message) => <Alert
            severity={"error"}>{entry[0]}:&nbsp;{entry[1]}</Alert>)}
    </>);
}

function HalNavigatorContent({
                                 api, navigate, fieldsFactory, navigateBack = () => {
    }
                             }: {
    api: NavigationTarget,
    navigate: (target: NavigationTarget) => Promise<void>,
    navigateBack?: () => void,
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {
    const {data, isLoading, error} = useSimpleFetch(api);
    const [showSource, setShowSource] = useState(true);
    if (isLoading) {
        return <Alert severity={"info"}>Nahravam data {toLink(api).href}</Alert>;
    }

    if (error) {
        console.table(error.stack);
        return <Alert severity={"error"}>Nepovedlo se nacist data {toLink(api).href}: {error.message}</Alert>;
    }

    function renderContent(): ReactElement {
        let content;
        if (isHalFormsResponse(data) && data.page === undefined) { // TODO: GET /members problem - vraci Members with template for registerNewMember. We check `.page` as all our lists are paged now, so it's able to distinguish Collection resource from HalForms. But we should have bettern distinguishment.
            content =
                <HalFormsContent submitApi={api} afterSubmit={navigateBack} initData={data}
                                 fieldsFactory={fieldsFactory}/>;
        } else if (isHalResponse(data)) {
            content = <HalContent data={data} navigate={navigate}/>;
        } else {
            content = <JsonPreview data={data} label={"Neznamy format dat (ocekavam HAL+FORMS nebo HAL)"}/>
        }
        return content;
    }

    return (<Grid container spacing={2} sx={{
        justifyContent: "space-between",
        alignItems: "baseline",
    }}>
        <Grid padding={2} xs={7}>
            <ErrorBoundary fallback={<JsonPreview label={"Nelze vyrenderovat Hal/HalForms obsah"} data={api}/>}>
                {renderContent()}
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
    const {current: state, navigate, back, isFirst, reset} = useNavigation<NavigationTarget>(startUrl);

    const renderNavigation = (): ReactElement => {
        console.log(startUrl);
        return (<Stack direction={"row"}>
            <Button onClick={reset}>Restart</Button>
            <Button disabled={isFirst} onClick={back}>Zpět</Button>
            <h3>{toHref(state)}</h3>
        </Stack>);
    }

    return (
        <div className="p-4 space-y-4">

            {renderNavigation()}

            <ErrorBoundary fallback={<JsonPreview data={state} label={"Nejde vyrenderovat HAL FORMS form"}/>}
                           resetKeys={[state]}>
                <HalNavigatorContent api={state}
                                     fieldsFactory={fieldsFactory}
                                     navigate={async (link) => await navigate(link)}
                                     navigateBack={back}
                />
            </ErrorBoundary>

        </div>
    );
}