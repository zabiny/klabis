import React, {type ReactElement, useState} from "react";
import {HalFormsForm} from "../components/HalNavigator2/halforms";
import {type HalFormsTemplate} from "../api";
import {ErrorBoundary} from 'react-error-boundary';
import {JsonPreview} from "../components/JsonPreview";
import {HalFormsFormField} from "../components/HalNavigator2/halforms/HalFormsForm.tsx";

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

function ExampleAutoHalForm(): ReactElement {

    const resource = {...demoData, _templates: {default: demoTemplate}};
    const [submitted, setSubmitted] = useState<Record<string, unknown>>();

    return (
        <div className="grid grid-cols-12 gap-2">
            <div className="col-span-6 p-2">
                <HalFormsForm
                    key={`exampleForm`} data={resource} template={resource?._templates.default}
                    onSubmit={async (data) => setSubmitted(data)}/>
                {submitted && <JsonPreview data={submitted} label={"Submitted"}/>}
            </div>
            <div className="col-span-4 p-2">
                <JsonPreview data={resource} label={"Zdrojova data HAL+FORMS"}/>
            </div>
        </div>
    );
}

function ExampleCustomHalForm(): ReactElement {

    const resource = {...demoData, _templates: {default: demoTemplate}};
    const [submitted, setSubmitted] = useState<Record<string, unknown>>();

    return (
        <div className="grid grid-cols-12 gap-2">
            <div className="col-span-6 p-2">
                <HalFormsForm
                    key={`exampleForm`} data={resource} template={resource?._templates.default}
                    onSubmit={async (data) => setSubmitted(data)}>
                    <div>
                        <h2 className="text-lg font-semibold mb-4">Vyplnte udaje noveho uzivatele:</h2>
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <div><HalFormsFormField fieldName={"firstName"}/></div>
                                <div><HalFormsFormField fieldName={"bio"}/></div>
                            </div>
                            <div className="space-y-2">
                                <div><HalFormsFormField fieldName={'hobbies'}/></div>
                                <div><HalFormsFormField fieldName={'neznamy'}/></div>
                            </div>
                        </div>
                        <div className="flex gap-2 mt-4">
                            <HalFormsFormField fieldName={'submit'}/>
                            <HalFormsFormField fieldName={'cancel'}/>
                        </div>
                    </div>
                </HalFormsForm>
                {submitted && <JsonPreview data={submitted} label={"Submitted"}/>}
            </div>
            <div className="col-span-4 p-2">
                <JsonPreview data={resource} label={"Zdrojova data HAL+FORMS"}/>
            </div>
        </div>
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
            {value === index && <div className="p-3">{children}</div>}
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

    const handleChange = (newValue: number) => {
        setTabValue(newValue);
    };

    const tabs = [
        {label: "Example Automatic HAL Form", component: <ExampleAutoHalForm/>},
        {label: "Example Customized HAL Form", component: <ExampleCustomHalForm/>}
    ];

    return (
        <div className="w-full">
            <div className="border-b border-gray-300 dark:border-gray-600">
                <div className="flex gap-0">
                    {tabs.map((tab, index) => (
                        <button
                            key={index}
                            onClick={() => handleChange(index)}
                            {...a11yProps(index)}
                            className={`px-4 py-2 font-medium transition-colors ${
                                tabValue === index
                                    ? 'border-b-2 border-red-600 text-red-600 dark:text-red-400'
                                    : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-300'
                            }`}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>
            </div>
            <ErrorBoundary fallback={"Neco se pokazilo"} resetKeys={[tabValue]}>
                {tabs.map((tab, index) => (
                    <CustomTabPanel key={index} value={tabValue} index={index}>
                        {tab.component}
                    </CustomTabPanel>
                ))}
            </ErrorBoundary>
        </div>
    )
}


export {
    SandplacePage
};