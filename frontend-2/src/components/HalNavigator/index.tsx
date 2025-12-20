import type {HalCollectionResponse, HalResponse, Link, NavigationTarget} from '../../api'
import type {ReactElement} from 'react'
import {useEffect, useState} from 'react'
import {Alert, Button, Skeleton, Spinner} from '../UI'
import {ErrorBoundary} from 'react-error-boundary'
import type {HalFormFieldFactory} from '../HalFormsForm'
import type {Navigation} from '../../hooks/useNavigation'
import {useNavigation} from '../../hooks/useNavigation'
import {JsonPreview} from '../JsonPreview'
import {isHalFormsTemplate, isHalResponse} from '../HalFormsForm/utils'
import {
    HalNavigatorContext,
    type NavigationTargetResponse,
    toHref,
    useHalExplorerNavigation,
    useNavigationTargetResponse
} from './hooks'
import {HalCollectionContent} from './halCollectionContent'
import {HalEditableItemContent} from './halItemContent'


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


function renderContent(
    item: NavigationTargetResponse<unknown>,
    navigation: Navigation<NavigationTarget>,
    fieldsFactory?: HalFormFieldFactory
): ReactElement {
    if (item.navigationTarget !== navigation.current) {
        console.warn('Difference between navigation target and navigation current... ')
        item = {...item, navigationTarget: navigation.current}
    }

    if (isItemNavigationTargetResponse(item)) {
        const initData: HalResponse =
            !item || !item.body || isErrorNavigationTargetResponse(item) ? {_embedded: []} : (item.body as HalResponse)
        return (
            <HalEditableItemContent initData={initData} navigation={navigation} fieldsFactory={fieldsFactory}/>
        )
    } else if (isCollectionNavigationTargetResponse(item)) {
        return <HalCollectionContent data={item.body || {_embedded: [], page: {}}} navigation={navigation}/>
    } else if (isErrorNavigationTargetResponse(item)) {
        return (
            <Alert severity="error">
                <div className="space-y-2">
                    <p>Nepovedlo se nacist data {toHref(item.navigationTarget)}:</p>
                    <p>Response status {item.responseStatus} ({item.contentType})</p>
                    <p>{item.body}</p>
                </div>
            </Alert>
        )
    } else {
        return <JsonPreview data={item.body} label={`Nepodporovany format dat - ${item?.contentType}`}/>
    }
}

function LoadingSkeletonContent(): ReactElement {
    return (
        <div className="grid grid-cols-12 gap-2">
            <div className="col-span-7 p-2">
                <div className="flex flex-col gap-2">
                    {/* Skeleton for heading */}
                    <Skeleton width="40%" height={40}/>
                    {/* Skeleton for table rows */}
                    <Skeleton height={40}/>
                    <Skeleton height={40}/>
                    <Skeleton height={40}/>
                    <Skeleton height={40}/>
                </div>
            </div>
            <div className="col-span-5 p-2">
                <Skeleton height={300}/>
            </div>
        </div>
    )
}

function LoadingOverlay(): ReactElement {
    const [showSlowIndicator, setShowSlowIndicator] = useState(false)

    useEffect(() => {
        const timer = setTimeout(() => {
            setShowSlowIndicator(true)
        }, 2000)

        return () => clearTimeout(timer)
    }, [])

    return (
        <div
            className="flex flex-col items-center justify-center gap-2 py-4"
            role="status"
            aria-live="polite"
            aria-label="Stav načítání"
            aria-busy="true"
        >
            <Spinner aria-hidden="true"/>
            <div className="text-sm text-gray-600 dark:text-gray-400">Načítání...</div>
            {showSlowIndicator && (
                <div className="text-xs text-yellow-600 dark:text-yellow-500">Probíhá načítání, prosím čekejte...</div>
            )}
        </div>
    )
}

function HalNavigatorContent({
                                 fieldsFactory
                             }: {
    fieldsFactory?: HalFormFieldFactory
}): ReactElement {
    const navigation = useHalExplorerNavigation()
    const response = useNavigationTargetResponse()
    if (!response) {
        return (
            <>
                <LoadingOverlay/>
                <LoadingSkeletonContent/>
            </>
        )
    }

    return (
        <div className="grid grid-cols-12 gap-2">
            <div className="col-span-7 p-2">
                <ErrorBoundary
                    fallback={
                        <JsonPreview label={'Nelze vyrenderovat Hal/HalForms obsah'} data={response.navigationTarget}/>
                    }
                >
                    {renderContent(response, navigation, fieldsFactory)}
                </ErrorBoundary>
            </div>
            <NavigationTargetSourceDetails/>
        </div>
    )
}

const NavigationTargetSourceDetails = (): ReactElement => {
    const [showSource, setShowSource] = useState(true)
    const response = useNavigationTargetResponse()
    const navigation = useHalExplorerNavigation()
    const isLoading = !response

    return (
        <div className={`col-span-5 ${showSource ? 'overflow-y-auto' : 'overflow-hidden'}`}>
            <div className="flex items-center gap-2 mb-4">
                <input
                    type="checkbox"
                    id="show-source-json"
                    checked={showSource}
                    onChange={(e) => setShowSource(e.target.checked)}
                    disabled={isLoading}
                    className="w-4 h-4 rounded border-gray-300 focus:ring-2 focus:ring-primary disabled:opacity-50"
                    aria-label="Zobrazit zdrojový JSON"
                />
                <label htmlFor="show-source-json" className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    Zobraz zdrojový JSON
                </label>
            </div>
            {showSource &&
                (isLoading ? (
                    <div className="flex flex-col gap-1">
                        <Skeleton width="100%" height={20}/>
                        <Skeleton height={150}/>
                        <Skeleton width="100%" height={20}/>
                        <Skeleton height={150}/>
                    </div>
                ) : (
                    <>
                        <JsonPreview data={response?.navigationTarget} label={'Current navigation target (response)'}/>
                        <JsonPreview data={navigation.current} label={'Current navigation target (navigation)'}/>
                        <JsonPreview
                            data={response?.body}
                            label={`Response data (${response?.responseStatus} - ${response?.contentType})`}
                        />
                    </>
                ))}
        </div>
    )
}

export function HalNavigatorPage({
                                     startUrl,
                                     fieldsFactory
                                 }: {
    startUrl: Link | string
    fieldsFactory?: HalFormFieldFactory
}) {
    const originalNavigation = useNavigation<NavigationTarget>(startUrl)
    const navigation: Navigation<NavigationTarget> = {
        ...originalNavigation,
        navigate: (target) => {
            if (isHalFormsTemplate(target) && !target.target) {
                target = {
                    ...target,
                    target: toHref(originalNavigation.current)
                }
            }
            originalNavigation.navigate(target)
        }
    }

    const renderNavigation = (): ReactElement => {
        return (
            <div className="flex items-center gap-4">
                <Button onClick={navigation.reset} aria-label="Začít znovu">
                    Restart
                </Button>
                <Button
                    disabled={navigation.isFirst}
                    onClick={navigation.back}
                    aria-label={navigation.isFirst ? 'Zpět (není kam)' : 'Zpět'}
                    aria-disabled={navigation.isFirst}
                >
                    Zpět
                </Button>
                <h3 className="m-0 text-sm text-gray-600 dark:text-gray-400">{toHref(navigation.current)}</h3>
            </div>
        )
    }

    return (
        <div className="p-4 space-y-4">
            {renderNavigation()}

            <HalNavigatorContext value={{navigation: navigation}}>
                <ErrorBoundary
                    fallback={<JsonPreview data={navigation.current}
                                           label={'Nejde vyrenderovat HAL Navigator content'}/>}
                    resetKeys={[navigation.current]}
                >
                    <HalNavigatorContent fieldsFactory={fieldsFactory}/>
                </ErrorBoundary>
            </HalNavigatorContext>
        </div>
    )
}
