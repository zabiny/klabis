import React, {type ReactElement, useState} from "react";
import {HalFormsForm} from "../components/HalFormsForm";
import {type HalFormsTemplate} from "../api";
import {Box, Grid, Tab, Tabs} from "@mui/material";
import {ErrorBoundary} from 'react-error-boundary';
import {HalNavigatorPage} from "../components/HalNavigator";
import {klabisFieldsFactory} from "../components/KlabisFieldsFactory";

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
            <ErrorBoundary fallback={"Neco se pokazilo"} resetKeys={[tabValue]}>
                <CustomTabPanel value={tabValue} index={0}>
                    <HalNavigatorPage startUrl={"/api"} fieldsFactory={klabisFieldsFactory}/>
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