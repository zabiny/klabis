import {
    type EntityModel,
    type HalFormsResponse,
    type HalFormsTemplate,
    type HalResponse,
    isFormTarget,
    type Link,
    type NavigationTarget,
    type PageMetadata,
    type TemplateTarget
} from "../../api";
import React, {type ReactElement, ReactNode, useCallback, useState} from "react";
import {Alert, Box, Button, Checkbox, FormLabel, Grid, Link as MuiLink, Stack, Typography} from "@mui/material";
import {ErrorBoundary} from "react-error-boundary";
import {type HalFormFieldFactory, HalFormsForm} from "../HalFormsForm";
import {type Navigation, useNavigation} from "../../hooks/useNavigation";
import {JsonPreview} from "../JsonPreview";
import {getDefaultTemplate, isHalFormsTemplate, isHalResponse} from "../HalFormsForm/utils";
import {isFormValidationError, submitHalFormsData} from "../../api/hateoas";
import {TableCell} from "../KlabisTable";
import EventType from "../events/EventType";
import {Public} from "@mui/icons-material";
import MemberName from "../members/MemberName";
import {
    HalNavigatorContext,
    toHref,
    toURLPath,
    useHalExplorerNavigation,
    useResponseBody,
    useSimpleFetch
} from "./hooks";
import {HalNavigatorTable} from "./halNavigatorTable";
import {HalActionsUi, HalLinksUi} from "./halActionComponents";


function omitMetadataAttributes<T extends { _links?: any }>(obj: T): Omit<T, '_links'> {
    const {_links, ...rest} = obj;
    return rest;
}

const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('cs-CZ').format(date);
};

function HalCollectionContent({navigation}: {
    data: HalResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {

    const data = useResponseBody();

    const renderCollectionContent = (relName: string, items: Record<string, unknown>[], paging?: PageMetadata): ReactNode => {

        const resourceUrlPath = toURLPath(navigation.current);

        const navigateToEntityModel = (item: EntityModel<unknown>): void => {
            if (item._links.self) {
                navigation.navigate(item._links.self);
            } else {
                alert(`Missing "self" link in entity model ${JSON.stringify(item)}`)
            }
        }

        switch (resourceUrlPath) {
            case '/members':
                return (<Box>
                        <Typography variant="h4" component="h1" gutterBottom>
                            Adresář
                        </Typography>

                        <HalLinksUi links={data._links} onClick={navigation.navigate} showPagingNavigation={false}/>

                        <HalNavigatorTable<EntityModel<{
                            id: number,
                            firstName: string,
                            lastName: string,
                            registrationNumber: string
                        }>>
                            embeddedName={'membersApiResponseList'}
                            onRowClick={navigateToEntityModel}
                            defaultOrderBy="lastName"
                            defaultOrderDirection="asc"
                        >
                            <TableCell sortable column="firstName">Jméno</TableCell>
                            <TableCell sortable column="lastName">Příjmení</TableCell>
                            <TableCell sortable column="registrationNumber">Registrační číslo</TableCell>
                            {/*<TableCell column="sex">Pohlaví</TableCell>*/}
                            {/*<TableCell sortable column="dateOfBirth">Datum narození</TableCell>*/}
                            {/*<TableCell column="nationality">Národnost</TableCell>*/}
                            {/*<TableCell column="_links"*/}
                            {/*           dataRender={props => (<HalLinksUi value={props.value}/>)}>Akce</TableCell>*/}
                        </HalNavigatorTable>
                    </Box>
                );
            case '/events':
                return (<Box>
                    <Typography variant="h4" component="h1" gutterBottom>
                        Závody
                    </Typography>

                    <HalLinksUi links={data._links} onClick={navigation.navigate} showPagingNavigation={false}/>

                    <HalNavigatorTable<EntityModel<{ date: string, name: string, id: number, location: string }>>
                        embeddedName={'eventResponseList'} defaultOrderBy={"date"}
                        defaultOrderDirection={'desc'}
                        onRowClick={navigateToEntityModel}>
                        <TableCell sortable column={"date"}
                                   dataRender={({value}) => formatDate(value)}>Datum</TableCell>
                        <TableCell sortable column={"name"}>Název</TableCell>
                        <TableCell sortable column={"location"}>Místo</TableCell>
                        <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                        <TableCell column={"type"}
                                   dataRender={({value}) => <EventType eventType={value}/>}>Typ</TableCell>
                        <TableCell column={"web"}
                                   dataRender={({value}) => <MuiLink hidden={!value}
                                                                     href={value}><Public/></MuiLink>}>Web</TableCell>
                        <TableCell sortable column={"registrationDeadline"} dataRender={({value}) => formatDate(value)}>Uzávěrka
                            přihlášek</TableCell>
                        <TableCell column={"coordinator"} dataRender={({value}) => value ?
                            <MemberName memberId={value}/> : <>--</>}>Vedoucí</TableCell>
                    </HalNavigatorTable>
                </Box>);
            default:
                return (<GenericHalCollectionContent label={relName} items={items}/>);
        }
    }

    return (
        <>
            {data?._embedded && Object.entries(data._embedded).map(([rel, items]) => renderCollectionContent(rel, items, data?.page))}

            {data?._templates && <HalActionsUi links={data._templates} onClick={link => navigation.navigate(link)}/>}

        </>)

}

const GenericHalCollectionContent = ({label, items}: { label: string, items: Record<string, unknown> }): ReactNode => {
    const navigation = useHalExplorerNavigation();
    const responseBody = useResponseBody();

    return (<div key={label}>
            <Typography variant="h4" component="h1" gutterBottom>{label}</Typography>

            <HalLinksUi links={responseBody._links} onClick={navigation.navigate} showPagingNavigation={true}/>

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
    );
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
    const {data, isLoading, error} = useSimpleFetch(api, {ignoredErrorStatues: [405, 404]});
    const [showSource, setShowSource] = useState(true);
    if (isLoading) {
        return <Alert severity={"info"}>Nahravam data {toHref(api)}</Alert>;
    }

    if (error) {
        return <Alert severity={"error"}>Nepovedlo se nacist data {toHref(api)}: {error.message}</Alert>;
    }

    function renderContent(item: any): ReactElement {
        if (isHalFormsTemplate(navigation.current)) {
            return <HalEditableItemContent initData={item} navigation={navigation} fieldsFactory={fieldsFactory}/>;
        } else if (isCollectionContent(item)) {
            return <HalCollectionContent data={item} navigation={navigation}/>;
        } else if (isHalResponse(item)) {
            return <HalItemContent data={item} navigation={navigation}/>;
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
            {showSource && <JsonPreview data={navigation.current} label={"Current navigation target"}/>}
            {showSource && <JsonPreview data={data} label={"Response data"}/>}
        </Grid>
    </Grid>);
}

export function HalNavigatorPage({
                                     startUrl,
                                     fieldsFactory
                                 }: {
    startUrl: Link | string,
    fieldsFactory?: HalFormFieldFactory
}) {
    const originalNavigation = useNavigation<NavigationTarget>(startUrl);
    const navigation: Navigation<NavigationTarget> = {
        ...originalNavigation,
        navigate: (target) => {
            // if template target doesn't have 'target' URL, add it before navigating to such target.
            if (isHalFormsTemplate(target) && !target.target) {
                target = {
                    ...target,
                    target: toHref(originalNavigation.current),
                };
            }
            originalNavigation.navigate(target);
        }
    };

    const renderNavigation = (): ReactElement => {
        return (<Stack direction={"row"}>
            <Button onClick={navigation.reset}>Restart</Button>
            <Button disabled={navigation.isFirst} onClick={navigation.back}>Zpět</Button>
            <h3>{toHref(navigation.current)}</h3>
        </Stack>);
    }

    return (
        <div className="p-4 space-y-4">

            {renderNavigation()}

            <HalNavigatorContext value={{navigation: navigation}}>
                <ErrorBoundary
                    fallback={<JsonPreview data={navigation.current} label={"Nejde vyrenderovat HAL FORMS form"}/>}
                    resetKeys={[navigation.current]}>
                    <HalNavigatorContent api={navigation.current}
                                         fieldsFactory={fieldsFactory}
                                         navigation={navigation}
                    />
                </ErrorBoundary>
            </HalNavigatorContext>

        </div>
    );
}
