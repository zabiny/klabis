import React, {type ReactElement, useState} from "react";
import {HalFormsForm} from "../components/HalFormsForm";
import {type HalFormsTemplate} from "../api";
import {Box, Grid, Tab, Tabs} from "@mui/material";
import {ErrorBoundary} from 'react-error-boundary';
import {HalNavigatorPage} from "../components/HalNavigator";
import {klabisFieldsFactory} from "../components/KlabisFieldsFactory";
import {JsonPreview} from "../components/JsonPreview";

const demoTemplate: HalFormsTemplate = {
    title: "Ukazkovy formular",
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
            name: "birthdate",
            type: "date",
            prompt: "Narozeniny"
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
            type: "checkboxGroup",
            multiple: true,
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
            type: "radioGroup",
            options: {
                inline: [
                    {value: "male", prompt: "Muž"},
                    {value: "female", prompt: "Žena"},
                    {value: "other", prompt: "Jiné"},
                ]
            },
            required: true,
        },
        {
            name: "enabled",
            prompt: "Zapnuty",
            type: "checkbox"
        },
        {
            name: "active",
            prompt: "Aktivni",
            type: "boolean"
        }
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
    active: true,
    enabled: true,
    birthdate: "2020-10-01"
};

function ExampleHalForm(): ReactElement {

    const resource = {...demoData, _templates: {default: demoTemplate}};
    const [submitted, setSubmitted] = useState();

    return (
        <Grid container spacing={2}>
            <Grid xs={6} padding={2}>
                <HalFormsForm
                    key={`exampleForm`} data={resource} template={resource?._templates.default}
                    onSubmit={setSubmitted}/>
                {submitted && <JsonPreview data={submitted} label={"Submitted"}/>}
            </Grid>
            <Grid xs={4} padding={2}>
                <JsonPreview data={resource} label={"Zdrojova data HAL+FORMS"}/>
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
    const [tabValue, setTabValue] = useState(0);

    const handleChange = (_event: React.SyntheticEvent, newValue: number) => {
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