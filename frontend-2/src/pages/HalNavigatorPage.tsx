import React, {ReactElement, ReactNode, useCallback, useEffect, useState} from "react";
import {UserManager} from "oidc-client-ts";
import {HalFormsForm} from "../components/HalFormsForm";
import {type HalFormsTemplate, HalResponse} from "../api";
import {Alert, Box, Button, Grid, Stack, Tab, Tabs} from "@mui/material";
import {ErrorBoundary} from 'react-error-boundary';
import {HalFormsFormController} from "../components/HalFormsForm/HalFormsForm";
import {klabisAuthUserManager} from "../api/klabisUserManager";
import {isKlabisFormResponse} from "../components/HalFormsForm/utils";

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

const useNavigation = <T, >(): {
    current: T | null;
    navigate: (resource: T) => void;
    back: () => void;
    isFirst: boolean,
    isLast: boolean,
    reset: () => void;
} => {
    const [navigation, setNavigation] = useState<Array<T>>([]);

    const navigate = useCallback((resource: T): void => {
        console.error('navigate')
        setNavigation(prev => [...prev, resource]);
    }, []);

    const back = useCallback((): void => {
        console.error('back')
        setNavigation(prev => {
            if (prev.length === 1) return prev;
            // remove the last item from the navigation stack
            return prev.slice(0, -1);
        });
    }, []);

    const reset = useCallback(() => {
        console.error('reset')
        setNavigation(prev => [prev[0]]);
    }, [])

    const current = navigation && navigation[navigation.length - 1] || null;

    const isFirst = navigation.length == 1;
    const isLast = true;    // doesn't keep forward (yet)

    console.log(navigation);

    return {current, navigate, back, isFirst, isLast, reset};
};

function JsonPreview({data, label = "Data"}) {
    return <div><h2>{label}</h2>
        <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>;
}

function HalNavigatorPage({startUrl}) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const {current, navigate, back, isFirst, reset} = useNavigation<HalResponse>();

    const load = useCallback(async (url) => {
        setLoading(true);
        setError(null);
        try {
            const data = await fetchResource(url);
            navigate(data);
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    }, [navigate]);

    useEffect(() => {
        load(startUrl)
    }, [startUrl, load]);

    const resource = current;

    const renderNavigation = (): ReactElement => {
        return (<Stack direction={"row"}>
            <Button onClick={e => reset()}>Restart</Button>
            <Button disabled={isFirst} onClick={e => back()}>Zpět</Button>
        </Stack>);
    }

    if (loading) return <p>Loading…</p>;
    if (error) return <p>
        {renderNavigation()}
        <Alert severity={"error"}>Error: {error}</Alert>
    </p>;
    if (!resource) return null;

    const links = resource._links || {};
    const embedded = resource._embedded || {};

    const renderFallback = (): ReactNode => {
        return <Grid>
            <div>Nejde vyrenderovat HAL FORMS form:</div>
            <JsonPreview data={resource}/>
        </Grid>;
    }

    const renderContent = (): ReactElement => {
        if (isKlabisFormResponse(current)) {
            return <ErrorBoundary fallback={renderFallback()} resetKeys={[current]}>
                <HalFormsForm data={resource} template={resource._templates.default} onSubmit={console.log}/>
                <JsonPreview label={"GET form data response"} data={resource}/>
            </ErrorBoundary>;
        } else {
            return (
                <div className="p-3 border rounded bg-gray-50">
                    <pre className="text-sm">{JSON.stringify(resource, null, 2)}</pre>
                </div>
            );
        }
    }

    return (
        <div className="p-4 space-y-4">
            {renderNavigation()}

            {/* Render actions based on links */}
            <div className="flex flex-wrap gap-2">
                {Object.entries(links).map(([rel, link]) => {
                    if (rel === "self") return null;
                    const href = Array.isArray(link) ? link[0].href : link.href;
                    return (
                        <Button
                            key={rel}
                            className="px-3 py-1 bg-blue-500 text-white rounded shadow hover:bg-blue-600"
                            onClick={() => load(href)}
                        >
                            {rel}
                        </Button>
                    );
                })}
            </div>

            {/* Render embedded collections/entities */}
            {Object.entries(embedded).map(([rel, items]) => (
                <div key={rel}>
                    <h2 className="font-semibold">{rel}</h2>
                    <ul className="list-disc list-inside">
                        {(Array.isArray(items) ? items : [items]).map((item, idx) => (
                            <li key={idx}>
                                {item.name || item.title || JSON.stringify(item)}
                                {item._links?.self && (
                                    <Button
                                        className="ml-2 px-2 py-0.5 text-sm bg-gray-300 rounded"
                                        onClick={() => load(item._links.self.href)}
                                    >
                                        Open
                                    </Button>
                                )}
                            </li>
                        ))}
                    </ul>
                </div>
            ))}

            {/* Display resource properties */}
            {renderContent()}


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
    const formsApi = {href: 'http://localhost:3000/api/events/1/registrationForms/1'};
    const [tabValue, setTabValue] = useState(1);

    const handleChange = (event: React.SyntheticEvent, newValue: number) => {
        setTabValue(newValue);
    };

    return (
        <Box sx={{width: '100%'}}>
            <Box sx={{borderBottom: 1, borderColor: 'divider'}}>
                <Tabs value={tabValue} onChange={handleChange} aria-label="basic tabs example">
                    <Tab label="HAL Explorer" {...a11yProps(0)} />
                    <Tab label="HAL Form z Klabis API" {...a11yProps(1)} />
                    <Tab label="Example HAL Form" {...a11yProps(1)} />
                </Tabs>
            </Box>
            <ErrorBoundary fallback={"Neco se pokazilo"} resetKeys={[tabValue]} onError={console.error}>
                <CustomTabPanel value={tabValue} index={0}>
                    <HalNavigatorPage startUrl={"/api"}/>
                </CustomTabPanel>
                <CustomTabPanel value={tabValue} index={1}>
                    <HalFormsFormController api={formsApi}/>
                </CustomTabPanel>
                <CustomTabPanel index={2} value={tabValue}>
                    <ExampleHalForm/>
                </CustomTabPanel>
            </ErrorBoundary>
        </Box>
    )
}


export {
    SandplacePage
};