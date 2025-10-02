import React, {ReactElement, ReactNode, useEffect, useState} from "react";
import {UserManager} from "oidc-client-ts";
import {klabisAuthUserManager} from "../api/setup";
import {HalFormsForm, type HalFormsResponse, HalFormsTemplate} from "../components/HalFormsForm";
import {Alert, Button, Grid} from "@mui/material";


const isHalFormsData = (item: any): item is HalFormsResponse => {
    return item._templates !== undefined && item._links !== undefined;
}

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

function HalNavigatorPage({startUrl}) {
    const [resource, setResource] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const load = async (url) => {
        setLoading(true);
        setError(null);
        try {
            const data = await fetchResource(url);
            setResource(data);
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    };

    const restart = (): void => {
        load(startUrl);
    }

    useEffect(() => {
        restart();
    }, [startUrl]);

    if (loading) return <p>Loading…</p>;
    if (error) return <p style={{color: "red"}}>Error: {error}<br/>
        <Button onClick={e => restart()}>Restart</Button>
    </p>;
    if (!resource) return null;

    const links = resource._links || {};
    const embedded = resource._embedded || {};

    return (
        <div className="p-4 space-y-4">
            <Button onClick={e => restart()}>Restart</Button>
            {/* Display resource properties */}
            <div className="p-3 border rounded bg-gray-50">
                <pre className="text-sm">{JSON.stringify(resource, null, 2)}</pre>
            </div>

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
            prompt: "Země",
            required: true,
            options: [
                {value: "cz", prompt: "Česká republika"},
                {value: "sk", prompt: "Slovensko"},
                {value: "pl", prompt: "Polsko"},
            ],
        },
        {
            name: "hobbies",
            prompt: "Koníčky",
            multiple: true,
            options: [
                {value: "orienteering", prompt: "Orienťák"},
                {value: "games", prompt: "Hry"},
                {value: "travel", prompt: "Cestování"},
            ],
            required: true,
        },
        {
            name: "gender",
            prompt: "Pohlaví",
            type: "radio",
            options: [
                {value: "male", prompt: "Muž"},
                {value: "female", prompt: "Žena"},
                {value: "other", prompt: "Jiné"},
            ],
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

    const [resource, setResource] = useState<HalFormsResponse>();
    const [isLoading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null)
    const [showExample, setShowExample] = useState(false);
    const URL = 'http://localhost:3000/api/events/1/registrationForms/1';

    const load = async (url: string) => {
        try {
            const data = await fetchResource(url);
            setResource(data);
            if (!isHalFormsData(data)) {
                setError(`Returned data are not HAL+FORMS`)
            }
        } catch (e) {
            setError(JSON.stringify(e, null, 2));
        } finally {
            setLoading(false);
        }
    };

    const reload = async () => {
        return load(URL);
    };

    useEffect(() => {
        reload()
    }, []);

    function showExampleData() {
        setShowExample(prev => {
            if (!prev) {
                setResource({_templates: {default: demoTemplate}, content: demoData});
            } else {
                reload();
            }
            return !prev;
        });
    }

    function renderStatus(): ReactNode {
        return (<>
            <Button
                onClick={e => showExampleData()}>{showExample ? "Prepni na Klabis data" : "Prepni na statickou ukazku"}</Button>
            {!showExample &&
                <Button onClick={e => reload()}>Reload</Button>}
            <div>
                <pre>{JSON.stringify(resource, null, 2)}</pre>
            </div>
        </>);
    }

    function renderForm() {
        if (showExample) {
            return <HalFormsForm data={demoData} template={demoTemplate} onSubmit={data => console.table(data)}/>;
        }
        if (isLoading) {
            return <Alert severity={"info"}>Loading form data</Alert>;
        }
        if (!error) {
            return <HalFormsForm data={resource?.content} template={resource?._templates.default}/>;
        } else {
            return <Alert severity={"error"}>{error}</Alert>;
        }
    }

    return (<Grid container spacing={2}>
        <Grid item xs={12}>{URL}</Grid>
        <Grid item xs={5}>{renderStatus()}</Grid>
        <Grid item xs={6}>{renderForm()}</Grid>
    </Grid>);
}

function SandplacePage(): ReactElement {
    return (<><h1 className="text-xl font-bold">HAL Form</h1>
            <ExampleHalForm/>

            <h1 className="text-xl font-bold">HAL Navigator</h1>
            <HalNavigatorPage startUrl={"/api"}/></>
    )
}


export {SandplacePage};