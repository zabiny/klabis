import React, {type ReactElement, useCallback, useEffect, useMemo, useState} from "react";
import {UserManager} from "oidc-client-ts";
import {HalFormsForm} from "../components/HalFormsForm";
import {type HalFormsResponse, type HalFormsTemplate, type HalResponse, type Link, type TemplateTarget} from "../api";
import {Alert, Box, Button, Checkbox, FormLabel, Grid, Link as MuiLink, Stack, Tab, Tabs} from "@mui/material";
import {ErrorBoundary} from 'react-error-boundary';
import {klabisAuthUserManager} from "../api/klabisUserManager";
import {isHalFormsResponse, isHalResponse} from "../components/HalFormsForm/utils";
import {isLink} from "../api/klabisJsonUtils";
import {submitHalFormsData} from "../api/hateoas";

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
        console.warn(await res.json());
        throw new Error(`HTTP ${res.status}`);
    }
    return res.json();
}

const useNavigation = <T, >(initial?: T): {
    current: T | undefined;
    navigate: (resource: T) => void;
    back: () => void;
    isFirst: boolean,
    isLast: boolean,
    reset: () => void;
} => {
    const [navigation, setNavigation] = useState<Array<T>>(initial && [initial] || []);

    const navigate = useCallback((resource: T): void => {
        setNavigation(prev => [...prev, resource]);
    }, []);

    const back = useCallback((): void => {
        setNavigation(prev => {
            if (prev.length < 2) return prev;
            // remove the last item from the navigation stack
            return prev.slice(0, -1);
        });
    }, []);

    const reset = useCallback(() => {
        setNavigation(prev => [prev[0]]);
    }, [])

    const current = useMemo(() => navigation && navigation[navigation.length - 1] || initial || undefined, [navigation, initial]);

    const isFirst = navigation.length == 1;
    const isLast = true;    // doesn't keep forward (yet)

    return {current, navigate, back, isFirst, isLast, reset};
};

function JsonPreview({data, label = "Data"}: { data?: object, label?: string }) {
    return <div><h2>{label}</h2>
        <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>;
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

function HalContent({data, navigate}: {
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
        ;
}

function isTemplateTarget(item: any): item is TemplateTarget {
    return item !== undefined && item !== null && ['POST', 'PUT', 'DELETE'].indexOf(item.method) !== -1;
}

type NavigationTarget = Link | TemplateTarget | string;

function HalFormsContent({
                             submitApi, initTemplate, initData, afterSubmit = () => {
    }
                         }: {
    submitApi: NavigationTarget,
    initTemplate?: HalFormsTemplate,
    initData: HalFormsResponse,
    afterSubmit?: () => void
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
        <HalFormsForm data={initData} template={activeTemplate} onSubmit={submit}/>
        {error && <Alert severity={"error"}>{error.message}</Alert>}
    </>);
}

function HalNavigatorContent({
                                 api, navigate, navigateBack = () => {
    }
                             }: {
    api: NavigationTarget,
    navigate: (target: NavigationTarget) => Promise<void>,
    navigateBack?: () => void,
}): ReactElement {
    const {data, isLoading, error} = useSimpleFetch(api);
    const [showSource, setShowSource] = useState(false);
    if (isLoading) {
        return <Alert severity={"info"}>Nahravam data {toLink(api).href}</Alert>;
    }

    if (error) {
        console.table(error.stack);
        return <Alert severity={"error"}>Nepovedlo se nacist data {toLink(api).href}: {error.message}</Alert>;
    }

    let content;
    if (isHalFormsResponse(data) && data.page === undefined) { // TODO: GET /members problem - vraci Members with template for registerNewMember. We check `.page` as all our lists are paged now, so it's able to distinguish Collection resource from HalForms. But we should have bettern distinguishment.
        content = <HalFormsContent submitApi={api} afterSubmit={navigateBack} initData={data}/>;
    } else if (isHalResponse(data)) {
        content = <HalContent data={data} navigate={navigate}/>;
    } else {
        content = <JsonPreview data={data} label={"Neznamy format dat (ocekavam HAL+FORMS nebo HAL)"}/>
    }

    return (<Grid container spacing={2} sx={{
        justifyContent: "space-between",
        alignItems: "baseline",
    }}>
        <Grid padding={2} xs={7}>{content}</Grid>
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

function HalNavigatorPage({
                              startUrl
                          }: {
    startUrl: Link | string
}) {
    const initState = useMemo(() => toLink(startUrl), [startUrl]);
    const {current: state, navigate, back, isFirst, reset} = useNavigation<NavigationTarget>(initState);

    const renderNavigation = (): ReactElement => {
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
                                     navigate={async (link) => await navigate(link)}
                                     navigateBack={back}
                />
            </ErrorBoundary>

        </div>
    );
}


const demoTemplate: HalFormsTemplate = {
    properties: [
        {name: "firstName", prompt: "Jméno", type: "text", required: true},
        {name: "email", prompt: "Email", type: "email", required: true},
        {name: "age", prompt: "Věk", type: "number"},
        {
            name: "bio",
            prompt: "Krátké info o sobě",
            type: "textarea",
        },
        {
            name: "country",
            type: "select",
            prompt: "Země",
            required: true,
            options: {
                inline: [
                    {value: "cz", prompt: "Česká republika"},
                    {value: "sk", prompt: "Slovensko"},
                    {value: "pl", prompt: "Polsko"},
                ]
            },
        },
        {
            name: "hobbies",
            prompt: "Koníčky",
            type: "checkbox",
            options: {
                inline: [
                    {value: "orienteering", prompt: "Orienťák"},
                    {value: "games", prompt: "Hry"},
                    {value: "travel", prompt: "Cestování"},
                ]
            },
            required: true,
        },
        {
            name: "gender",
            prompt: "Pohlaví",
            type: "radio",
            options: {
                inline: [
                    {value: "male", prompt: "Muž"},
                    {value: "female", prompt: "Žena"},
                    {value: "other", prompt: "Jiné"},
                ]
            },
            required: true,
        },
    ],
};

const demoData = {
    firstName: "David",
    email:
        "david@example.com",
    age:
        30,
    country:
        "cz",
    hobbies:
        ["games"],
    gender:
        "male",
};

function ExampleHalForm(): ReactElement {

    const resource = {_templates: {default: demoTemplate}, ...demoData};

    return (
        <Grid direction={"column"} spacing={2}>
            <Grid>
                <HalFormsForm
                    key={`exampleForm`} data={resource} template={resource?._templates.default}
                    onSubmit={data => console.log(JSON.stringify(data, null, 2))}/>
            </Grid>
            <Grid>
                <pre>{JSON.stringify(resource, null, 2)}</pre>
            </Grid>
        </Grid>
    );
}

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

function CustomTabPanel(props: TabPanelProps) {
    const {children, value, index, ...other} = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`simple-tabpanel-${index}`}
            aria-labelledby={`simple-tab-${index}`}
            {...other}
        >
            {value === index && <Box sx={{p: 3}}>{children}</Box>}
        </div>
    );
}

function a11yProps(index: number) {
    return {
        id: `simple-tab-${index}`,
        'aria-controls': `simple-tabpanel-${index}`,
    };
}

function SandplacePage(): ReactElement {
    const [tabValue, setTabValue] = useState(1);

    const handleChange = (event: React.SyntheticEvent, newValue: number) => {
        setTabValue(newValue);
    };

    return (
        <Box sx={{width: '100%'}}>
            <Box sx={{borderBottom: 1, borderColor: 'divider'}}>
                <Tabs value={tabValue} onChange={handleChange} aria-label="basic tabs example">
                    <Tab label="HAL Explorer" {...a11yProps(0)} />
                    <Tab label="Example HAL Form" {...a11yProps(1)} />
                </Tabs>
            </Box>
            <ErrorBoundary fallback={"Neco se pokazilo"} resetKeys={[tabValue]} onError={console.error}>
                <CustomTabPanel value={tabValue} index={0}>
                    <HalNavigatorPage startUrl={"/api"}/>
                </CustomTabPanel>
                <CustomTabPanel index={1} value={tabValue}>
                    <ExampleHalForm/>
                </CustomTabPanel>
            </ErrorBoundary>
        </Box>
    )
}


export {
    SandplacePage
};