import type {HalCollectionResponse, HalResponse, Link, NavigationTarget} from "../../api";
import {type ReactElement, useEffect, useState} from "react";
import {Alert, Box, Button, Checkbox, CircularProgress, FormControlLabel, Grid, Skeleton, Stack} from "@mui/material";
import {ErrorBoundary} from "react-error-boundary";
import {type HalFormFieldFactory} from "../HalFormsForm";
import {type Navigation, useNavigation} from "../../hooks/useNavigation";
import {JsonPreview} from "../JsonPreview";
import {isHalFormsTemplate, isHalResponse} from "../HalFormsForm/utils";
import {
    HalNavigatorContext,
    type NavigationTargetResponse,
    toHref,
    useHalExplorerNavigation,
    useNavigationTargetResponse
} from "./hooks";
import {HalCollectionContent} from "./halCollectionContent";
import {HalEditableItemContent} from "./halItemContent";


function isCollectionContent(data: Record<string, unknown>): data is HalCollectionResponse {
    return (data?.page !== undefined || data?._embedded !== undefined);
}

function isNavigationTargetResponse(item: unknown): item is NavigationTargetResponse<unknown> {
    return typeof item === 'object' && item !== null && 'navigationTarget' in item;
}

function isCollectionNavigationTargetResponse(response: unknown): response is NavigationTargetResponse<HalCollectionResponse> {
    if (!isNavigationTargetResponse(response) || isErrorNavigationTargetResponse(response)) {
        return false;
    }

    if (isHalFormsTemplate(response.navigationTarget)) {
        return false;
    }

    return isHalResponse(response.body) && isCollectionContent(response.body);
}

function isItemNavigationTargetResponse(response: unknown): response is NavigationTargetResponse<HalResponse | string> {
    if (!isNavigationTargetResponse(response)) {
        return false;
    }

    // HAL+FORMS response may be error 404 or 405 (we do not require forms API to be defined for HAL+FORMS forms, so attempt to fetch data from their URI may end up with these errors)
    if (isErrorNavigationTargetResponse(response)) {
        return [405, 404].includes(response.responseStatus);
    } else if (isHalFormsTemplate(response.navigationTarget)) {
        return true;
    } else {
        return !isCollectionContent(response.body as Record<string, unknown>);
    }
}

function isErrorNavigationTargetResponse(response: unknown): response is NavigationTargetResponse<string> {
    return isNavigationTargetResponse(response) && typeof response.body === "string";
}


function renderContent(item: NavigationTargetResponse<unknown>, navigation: Navigation<NavigationTarget>, fieldsFactory?: HalFormFieldFactory): ReactElement {
    if (item.navigationTarget !== navigation.current) {
        console.warn("Difference between navigation target and navigation current... ")
        // TODO: it's currently unclear to me why response has different navigation target sometimes. It seems to happen mostly on collection resources (for example Create new event )
        item = {...item, navigationTarget: navigation.current};
    }

    if (isItemNavigationTargetResponse(item)) {
        const initData: HalResponse = (!item || !item.body || isErrorNavigationTargetResponse(item))
            ? {_embedded: []}
            : (item.body as HalResponse);
        return <HalEditableItemContent initData={initData}
                                       navigation={navigation}
                                       fieldsFactory={fieldsFactory}/>;
    } else if (isCollectionNavigationTargetResponse(item)) {
        return <HalCollectionContent data={item.body || {_embedded: [], page: {}}} navigation={navigation}/>;
    } else if (isErrorNavigationTargetResponse(item)) {
        return <Alert
            severity={"error"}
            role="alert"
            aria-live="assertive"
            aria-label="Chyba načítání dat"
        >
            Nepovedlo se nacist data {toHref(item.navigationTarget)}:<br/>
            Response status {item.responseStatus} ({item.contentType})<br/>
            {item.body}
        </Alert>;
    } else {
        return <JsonPreview data={item.body} label={`Nepodporovany format dat - ${item?.contentType}`}/>;
    }
}

function LoadingSkeletonContent(): ReactElement {
    return (
        <Grid container spacing={2} sx={{
            justifyContent: "space-between",
            alignItems: "flex-start",
        }}>
            <Grid padding={2} xs={7}>
                <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
                    {/* Skeleton for heading */}
                    <Skeleton variant="text" width="40%" height={40}/>
                    {/* Skeleton for table rows */}
                    <Skeleton variant="rectangular" height={40}/>
                    <Skeleton variant="rectangular" height={40}/>
                    <Skeleton variant="rectangular" height={40}/>
                    <Skeleton variant="rectangular" height={40}/>
                </Box>
            </Grid>
            <Grid xs={5} padding={2}>
                <Skeleton variant="rectangular" height={300}/>
            </Grid>
        </Grid>
    );
}

function LoadingOverlay(): ReactElement {
    const [showSlowIndicator, setShowSlowIndicator] = useState(false);

    useEffect(() => {
        const timer = setTimeout(() => {
            setShowSlowIndicator(true);
        }, 2000);

        return () => clearTimeout(timer);
    }, []);

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 2,
                py: 4
            }}
            role="status"
            aria-live="polite"
            aria-label="Stav načítání"
            aria-busy="true"
        >
            <CircularProgress aria-hidden="true"/>
            <Box sx={{fontSize: '0.875rem', color: 'text.secondary'}}>
                Načítání...
            </Box>
            {showSlowIndicator && (
                <Box sx={{fontSize: '0.75rem', color: 'warning.main'}}>
                    Probíhá načítání, prosím čekejte...
                </Box>
            )}
        </Box>
    );
}

function HalNavigatorContent({
                                 fieldsFactory
                             }: {
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {
    const navigation = useHalExplorerNavigation();
    const response = useNavigationTargetResponse();
    if (!response) {
        return (
            <>
                <LoadingOverlay/>
                <LoadingSkeletonContent/>
            </>
        );
    }

    // if (response.isSuccess || (isHalFormsTemplate(navigation.current) && [405, 404].includes(response.responseStatus))) {
    //     // this is OK - for Hal+Forms template there doesn't necessarily need to be resource defined for GET uri (usually create forms without pre-filled fields).
    //     // just continue to further code...
    // } else {
    //     return <Alert severity={"error"}>Nepovedlo se nacist
    //         data {toHref(response.navigationTarget)}: {response.responseStatus} {response.body &&
    //             <JsonPreview data={response.body}/>}</Alert>;
    // }

    return (<Grid container spacing={2} sx={{
        justifyContent: "space-between",
        alignItems: "baseline",
    }}>
        <Grid padding={2} xs={7}>
            <ErrorBoundary fallback={<JsonPreview label={"Nelze vyrenderovat Hal/HalForms obsah"}
                                                  data={response.navigationTarget}/>}>
                {renderContent(response, navigation, fieldsFactory)}
            </ErrorBoundary>
        </Grid>
        <NavigationTargetSourceDetails/>
    </Grid>);
}

const NavigationTargetSourceDetails = (): ReactElement => {
    const [showSource, setShowSource] = useState(true);
    const response = useNavigationTargetResponse();
    const navigation = useHalExplorerNavigation();
    const isLoading = !response;

    return (<Grid overflow={showSource ? "scroll" : "none"} xs={5}>
            <FormControlLabel
                control={
                    <Checkbox
                        checked={showSource}
                        onChange={(_event, checked) => setShowSource(checked)}
                        disabled={isLoading}
                        aria-label="Zobrazit zdrojový JSON"
                    />
                }
                label="Zobraz zdrojový JSON"
            />
            {showSource && (isLoading ? (
                <Box sx={{display: 'flex', flexDirection: 'column', gap: 1}}>
                    <Skeleton variant="text" width="100%" height={20}/>
                    <Skeleton variant="rectangular" height={150}/>
                    <Skeleton variant="text" width="100%" height={20}/>
                    <Skeleton variant="rectangular" height={150}/>
                </Box>
            ) : (
                <>
                    <JsonPreview data={response?.navigationTarget} label={"Current navigation target (response)"}/>
                    <JsonPreview data={navigation.current} label={"Current navigation target (navigation)"}/>
                    <JsonPreview data={response?.body}
                                 label={`Response data (${response?.responseStatus} - ${response?.contentType})`}/>
                </>
            ))}
        </Grid>
    );

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
        return (<Stack direction={"row"} alignItems="center" spacing={2}>
            <Button
                onClick={navigation.reset}
                aria-label="Začít znovu"
            >
                Restart
            </Button>
            <Button
                disabled={navigation.isFirst}
                onClick={navigation.back}
                aria-label={navigation.isFirst ? "Zpět (není kam)" : "Zpět"}
                aria-disabled={navigation.isFirst}
            >
                Zpět
            </Button>
            <h3 style={{margin: 0}}>{toHref(navigation.current)}</h3>
        </Stack>);
    }

    return (
        <div className="p-4 space-y-4">

            {renderNavigation()}

            <HalNavigatorContext value={{navigation: navigation}}>
                <ErrorBoundary
                    fallback={<JsonPreview data={navigation.current}
                                           label={"Nejde vyrenderovat HAL Navigator content"}/>}
                    resetKeys={[navigation.current]}>
                    <HalNavigatorContent fieldsFactory={fieldsFactory}/>
                </ErrorBoundary>
            </HalNavigatorContext>

        </div>
    );
}
