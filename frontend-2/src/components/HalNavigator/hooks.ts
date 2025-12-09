import {Navigation} from "../../hooks/useNavigation";
import {HalResponse, isTemplateTarget, type NavigationTarget} from "../../api";
import {isLink} from "../../api/klabisJsonUtils";
import {isString} from "formik";
import {createContext, useContext} from "react";
import {useQuery, UseQueryResult} from "@tanstack/react-query";
import {UserManager} from "oidc-client-ts";
import {klabisAuthUserManager} from "../../api/klabisUserManager";
import {isHalFormsTemplate} from "../HalFormsForm/utils";

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
export async function fetchResource(url: string | URL) {
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

export function toHref(source: NavigationTarget): string {
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

export function toURLPath(item: NavigationTarget): string {
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

interface HalNavigatorContextData {
    navigation: Navigation<NavigationTarget>
}

export const HalNavigatorContext = createContext<HalNavigatorContextData>(null);

export const useHalExplorerNavigation = (): Navigation<NavigationTarget> => {
    const {navigation} = useContext(HalNavigatorContext);

    return navigation;
}

type ResponseData<T> = {
    body: T,
    contentType: string,
    responseStatus: number
}

export const useNavigationTargetResponse = (target?: NavigationTarget): UseQueryResult<ResponseData<HalResponse>, Error> => {
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

export const useResponseBody = (): HalResponse | undefined => {
    const result = useNavigationTargetResponse();

    if (result.isSuccess && !result.isLoading) {
        return result.data.body;
    } else {
        return undefined;
    }
}

interface SimpleFetchOptions {
    ignoredErrorStatues?: number[],
    responseForError?: HalResponse
}

export const useSimpleFetch = (resource: NavigationTarget, options?: SimpleFetchOptions): {
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