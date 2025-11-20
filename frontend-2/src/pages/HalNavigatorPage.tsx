import React, {ReactElement, ReactNode, useCallback, useEffect, useMemo, useState} from "react";
import {UserManager} from "oidc-client-ts";
import {HalFormsForm} from "../components/HalFormsForm";
import {type HalFormsTemplate, HalResponse, Link} from "../api";
import {Alert, Box, Button, Grid, Stack, Tab, Tabs} from "@mui/material";
import {ErrorBoundary} from 'react-error-boundary';
import {klabisAuthUserManager} from "../api/klabisUserManager";
import {isHalResponse, isKlabisFormResponse} from "../components/HalFormsForm/utils";
import {isLink} from "../api/klabisJsonUtils";

const userManager: UserManager = klabisAuthUserManager;

// Generic HAL fetcher
async function fetchResource(url) {
    const user = await userManager.getUser();
    const res = await fetch(url, {
        headers: {
            Accept: "application/prs.hal-forms+json,application/hal+json",
            "Authorization": `Bearer ${user?.access_token}`
        },
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
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

function JsonPreview({data, label = "Data"}) {
    return <div><h2>{label}</h2>
        <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>;
}

function HalLinksUi({links, onClick}: { links: object, onClick: (link: Link) => void }): ReactElement {
    return (
        <div className="flex flex-wrap gap-2">
            {Object.entries(links).map(([rel, link]) => {
                if (rel === "self") return null;
                const simpleLink = Array.isArray(link) ? link[0] : link;
                return (
                    <Button
                        key={rel}
                        className="px-3 py-1 bg-blue-500 text-white rounded shadow hover:bg-blue-600"
                        onClick={() => onClick(simpleLink)}
                    >
                        {rel}
                    </Button>
                );
            })}
        </div>
    );
}

function HalNavigatorContent({current, navigate}: {
    current: HalResponse,
    navigate: (target: Link) => Promise<void>
}): ReactElement {

    if (isKlabisFormResponse(current) && current._templates?.default) {
        return (<>
            <HalFormsForm data={current} template={current._templates.default} onSubmit={console.log}/>
            <JsonPreview label={"GET form data response"} data={current}/>
        </>);
    } else if (isHalResponse(current) && current._embedded) {
        return (<>
            {Object.entries(current._embedded).map(([rel, items]) => (
                <div key={rel}>
                    <h2 className="font-semibold">{rel}</h2>
                    <ul className="list-disc list-inside">
                        {(Array.isArray(items) ? items : [items]).map((item, idx) => (
                            <li key={idx}>
                                {item.name || item.title || JSON.stringify(item)}
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
            ))}
        </>);
    } else {
        return (<>
            <div className="p-3 border rounded bg-gray-50">
                <pre className="text-sm">{JSON.stringify(current, null, 2)}</pre>
            </div>
            </>
        );
    }
}

interface HalNavigatorState {
    resource: Link,
    template?: HalFormsTemplate
}

function toLink(item: Link | string): Link {
    if (isLink(item)) {
        return item;
    } else {
        return {href: item};
    }
}

const useSimpleFetch = (resource?: Link): { data?: HalResponse, isLoading: boolean, error?: Error } => {
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState();
    const [error, setError] = useState();

    const loadData = useCallback(async (link: Link) => {
        setLoading(true);
        setError(undefined);
        setData(undefined);
        try {
            const response = await fetchResource(link.href);
            setData(response);
        } catch (e) {
            console.error(e);
            setError(e);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (resource) {
            loadData(resource);
        }
    }, [resource]);

    return {isLoading: loading, data, error};
}

function HalNavigatorPage({startUrl}: { startUrl: Link | string }) {
    const initState = useMemo(() => toLink(startUrl), [startUrl]);

    const {current, navigate, back, isFirst, reset} = useNavigation<HalNavigatorState>({resource: initState});
    const {data, isLoading, error} = useSimpleFetch(current?.resource);

    const resource = data;

    const renderNavigation = (): ReactElement => {
        return (<Stack direction={"row"}>
            <Button onClick={reset}>Restart</Button>
            <Button disabled={isFirst} onClick={back}>Zpět</Button>
        </Stack>);
    }

    const renderFallback = (): ReactNode => {
        return <Grid>
            <div>Nejde vyrenderovat HAL FORMS form:</div>
            <JsonPreview data={resource}/>
        </Grid>;
    }
    const links = resource?._links || {};

    return (
        <div className="p-4 space-y-4">
            {renderNavigation()}
            <HalLinksUi links={links} onClick={link => navigate({resource: link})}/>

            <ErrorBoundary fallback={renderFallback()} resetKeys={[current]}>
                {isLoading && <Alert severity={"warning"}>Loading...</Alert>}
                {error && <Alert severity={"error"}>Error: {JSON.stringify(error, null, 2)}</Alert>}
                {resource && <HalNavigatorContent current={resource}
                                                  navigate={async (link) => await navigate({resource: link})}/>}
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
    email: "david@example.com",
    age: 30,
    country: "cz",
    hobbies: ["games"],
    gender: "male",
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