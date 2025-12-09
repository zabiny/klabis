import {
    EntityModel,
    type HalFormsResponse,
    type HalFormsTemplate,
    type HalResponse,
    isFormTarget,
    isTemplateTarget,
    type Link,
    type NavigationTarget,
    type PageMetadata,
    type TemplateTarget
} from "../../api";
import React, {createContext, type ReactElement, useCallback, useContext, useState} from "react";
import {Alert, Box, Button, Checkbox, FormLabel, Grid, Link as MuiLink, Stack, Typography} from "@mui/material";
import {ErrorBoundary} from "react-error-boundary";
import {type HalFormFieldFactory, HalFormsForm} from "../HalFormsForm";
import {UserManager} from "oidc-client-ts";
import {klabisAuthUserManager} from "../../api/klabisUserManager";
import {type Navigation, useNavigation} from "../../hooks/useNavigation";
import {JsonPreview} from "../JsonPreview";
import {getDefaultTemplate, isHalFormsTemplate, isHalResponse} from "../HalFormsForm/utils";
import {isFormValidationError, submitHalFormsData} from "../../api/hateoas";
import {isLink} from "../../api/klabisJsonUtils";
import {isString} from "formik";
import {type FetchTableDataCallback, KlabisTable, TableCell} from "../KlabisTable";
import EventType from "../events/EventType";
import {Public} from "@mui/icons-material";
import MemberName from "../members/MemberName";
import {useQuery, UseQueryResult} from "@tanstack/react-query";


const userManager: UserManager = klabisAuthUserManager;

class FetchError extends Error {
    public responseBody?: string;
    public responseStatus: number;
    public responseStatusText: string;

    constructor(message: string, responseStatus: number, responseStatusText: string, responseBody?: string) {
        super(message);
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
        this.responseStatusText = responseStatusText;
    }

}

// Generic HAL fetcher
async function fetchResource(url: string | URL) {
    const user = await userManager.getUser();
    const res = await fetch(url, {
        headers: {
            Accept: "application/prs.hal-forms+json,application/hal+json",
            "Authorization": `Bearer ${user?.access_token}`
        },
    });
    if (!res.ok) {
        let bodyText: string | undefined = undefined;
        if (res.body) {
            bodyText = await res.text();
            console.warn(bodyText ? `Response body: ${bodyText}` : 'No response body');
        }
        throw new FetchError(`HTTP ${res.status}`, res.status, res.statusText, bodyText);
    }
    return res.json();
}

const COLLECTION_LINK_RELS = ["prev", "next", "last", "first"];


function HalLinksUi({links, onClick, showPagingNavigation = true}: {
    links: Record<string, NavigationTarget>,
    onClick: (link: NavigationTarget) => void,
    showPagingNavigation: boolean
}): ReactElement {
    return (
        <Stack direction={"row"} spacing={2}>
            {Object.entries(links)
                .filter(([rel, _link]) => !COLLECTION_LINK_RELS.includes(rel) || showPagingNavigation)
                .map(([rel, link]) => {
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

function HalActionsUi({links, onClick}: {
    links: Record<string, NavigationTarget>,
    onClick: (link: NavigationTarget) => void
}): ReactElement {
    return (
        <Stack direction={"row"} spacing={2}>
            {Object.entries(links).map(([rel, link]) => {
                if (rel === "self") return null;
                const singleLink = Array.isArray(link) ? link[0] : link;
                return (
                    <Button key={rel}
                            onClick={() => onClick(singleLink)}>{singleLink.title || singleLink.name || rel}</Button>
                );
            })}
        </Stack>
    );
}

function omitMetadataAttributes<T extends { _links?: any }>(obj: T): Omit<T, '_links'> {
    const {_links, ...rest} = obj;
    return rest;
}

const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('cs-CZ').format(date);
};

function toURLPath(item: NavigationTarget): string {
    const itemHref = toHref(item);
    if (itemHref.startsWith("/")) {
        return itemHref;
    }
    try {
        return new URL(toHref(item)).pathname;
    } catch (e) {
        console.error(`failed to convert navigation item ${JSON.stringify(item)}: ${e}`)
        throw e;
    }
}

function HalCollectionContent({navigation}: {
    data: HalResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {

    const data = useResponseBody();

    const renderCollectionContent = (relName: string, items: Record<string, unknown>[], paging?: PageMetadata): React.ReactElement => {

        const resourceUrlPath = toURLPath(navigation.current);

        const navigateToEntityModel = (item: EntityModel<unknown>): void => {
            if (item._links.self) {
                navigation.navigate(item._links.self);
            } else {
                alert(`Missing "self" link in entity model ${JSON.stringify(item)}`)
            }
        }

        function tableDataFetcherFactory<T>(relName: string): FetchTableDataCallback<T> {
            return async (apiParams) => {
                const targetUrl = new URL(toHref(navigation.current));
                targetUrl.searchParams.append('page', `${apiParams.page}`)
                targetUrl.searchParams.append('size', `${apiParams.size}`)
                apiParams.sort.forEach(str => targetUrl.searchParams.append('sort', str));

                const response = await fetchResource(targetUrl);
                return {
                    // get data from given embedded relation name (should be same as initial data were)
                    data: response?._embedded?.[relName] as T[] || [],
                    page: response.page
                };
            }
        }

        switch (resourceUrlPath) {
            case '/members':
                return (<Box>
                        <Typography variant="h4" component="h1" gutterBottom>
                            Adresář
                        </Typography>

                        <HalLinksUi links={data._links} onClick={navigation.navigate} showPagingNavigation={false}/>

                        <KlabisTable<EntityModel<{
                            id: number,
                            firstName: string,
                            lastName: string,
                            registrationNumber: string
                        }>>
                            fetchData={tableDataFetcherFactory('membersApiResponseList')}    // TODO: provide real HAL/HAL-FORMS data fetching
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
                        </KlabisTable>
                    </Box>
                );
            case '/events':
                return (<Box>
                    <Typography variant="h4" component="h1" gutterBottom>
                        Závody
                    </Typography>

                    <HalLinksUi links={data._links} onClick={navigation.navigate} showPagingNavigation={false}/>

                    <KlabisTable<EntityModel<{ date: string, name: string, id: number, location: string }>>
                        fetchData={tableDataFetcherFactory('eventResponseList')} defaultOrderBy={"date"}
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
                    </KlabisTable>
                </Box>);
            default:
                return (<div key={relName}>
                        <Typography variant="h4" component="h1" gutterBottom>{relName}</Typography>

                        <HalLinksUi links={data._links} onClick={navigation.navigate} showPagingNavigation={true}/>

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
    }

    return (
        <>
            {data._embedded && Object.entries(data._embedded).map(([rel, items]) => renderCollectionContent(rel, items, data?.page))}

            {data._templates && <HalActionsUi links={data._templates} onClick={link => navigation.navigate(link)}/>}

        </>)

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
        return <Alert severity={"info"}>Nahravam data {toLink(api).href}</Alert>;
    }

    if (error) {
        return <Alert severity={"error"}>Nepovedlo se nacist data {toLink(api).href}: {error.message}</Alert>;
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

function toLink(item: NavigationTarget): Link {
    if (isLink(item)) {
        return item;
    } else if (isTemplateTarget(item)) {
        return {href: item.target};
    } else {
        return {href: item};
    }
}

interface SimpleFetchOptions {
    ignoredErrorStatues?: number[],
    responseForError?: HalResponse
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
    } else if (isString(source)) {
        return source;
    } else {
        throw new Error("Unknown NavigationTarget: " + JSON.stringify(source, null, 2))
    }
}

interface HalNavigatorContextData {
    navigation: Navigation<NavigationTarget>
}

const HalNavigatorContext = createContext<HalNavigatorContextData>(null);

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
        console.log("Render nav:" + JSON.stringify(navigation.current));
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

const useHalExplorerNavigation = (): Navigation<NavigationTarget> => {
    const {navigation} = useContext(HalNavigatorContext);

    return navigation;
}

type ResponseData<T> = {
    body: T,
    contentType: string,
    responseStatus: number
}

const useNavigationTargetResponse = (target?: NavigationTarget): UseQueryResult<ResponseData<HalResponse>, Error> => {
    const navigation = useHalExplorerNavigation();

    const resourceUrl = toHref(target || navigation.current);

    return useQuery<ResponseData<HalResponse>>({
        queryKey: [resourceUrl], queryFn: async (context): Promise<ResponseData<HalResponse>> => {
            const user = await userManager.getUser();
            const res = await fetch(resourceUrl, {
                headers: {
                    Accept: "application/prs.hal-forms+json,application/hal+json",
                    "Authorization": `Bearer ${user?.access_token}`
                },
            });
            if (!res.ok) {
                let bodyText: string | undefined = undefined;
                if (res.body) {
                    bodyText = await res.text();
                    console.warn(bodyText ? `Response body: ${bodyText}` : 'No response body');
                }
                throw new FetchError(`HTTP ${res.status}`, res.status, res.statusText, bodyText);
            }
            return {
                body: await res.json(),
                contentType: res.headers.get("Content-Type") || '??? not found ??',
                responseStatus: res.status
            };
        }
    });
}

const useResponseBody = (): HalResponse | undefined => {
    const result = useNavigationTargetResponse();

    if (result.isSuccess && !result.isLoading) {
        return result.data.body;
    } else {
        return undefined;
    }
}

const useSimpleFetch = (resource: NavigationTarget, options?: SimpleFetchOptions): {
    data?: HalResponse,
    isLoading: boolean,
    error?: Error
} => {

    const result = useNavigationTargetResponse();

    if (result.error) {
        const ignoredStatuses = options?.ignoredErrorStatues || [];
        if (isHalFormsTemplate(resource) && result.error instanceof FetchError && ignoredStatuses.indexOf(result.error.responseStatus) > -1) {
            console.warn(`HAL+FORMS API ${toHref(resource)} responded with ${result.error.responseStatus} - error will be replaced with empty object as Form resources doesn't require GET API if there are no data to be prepopulated in the form`)

            return {
                isLoading: false,
                data: options?.responseForError || {}
            }
        } else {
            return {
                isLoading: false,
                error: result.error
            }
        }
    }

    return {
        isLoading: result.isLoading,
        data: result.data?.body
    }
}