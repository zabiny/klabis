import {type HalResponse, type Link, type NavigationTarget} from "../../api";
import {type ReactElement, useState} from "react";
import {Alert, Button, Checkbox, FormLabel, Grid, Stack} from "@mui/material";
import {ErrorBoundary} from "react-error-boundary";
import {type HalFormFieldFactory} from "../HalFormsForm";
import {type Navigation, useNavigation} from "../../hooks/useNavigation";
import {JsonPreview} from "../JsonPreview";
import {isHalFormsTemplate, isHalResponse} from "../HalFormsForm/utils";
import {HalNavigatorContext, toHref, useSimpleFetch} from "./hooks";
import {HalCollectionContent} from "./halCollectionContent";
import {HalEditableItemContent, HalItemContent} from "./halItemContent";


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
