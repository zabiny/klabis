import {Navigation} from "../../hooks/useNavigation";
import {HalResponse, isTemplateTarget, type NavigationTarget} from "../../api";
import {isLink} from "../../api/klabisJsonUtils";
import {isString} from "formik";
import {createContext, useContext} from "react";
import {useQuery, UseQueryResult} from "@tanstack/react-query";
import {UserManager} from "oidc-client-ts";
import {klabisAuthUserManager} from "../../api/klabisUserManager";

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

export type NavigationTargetResponse<T> = {
    body?: T,
    contentType: string,
    responseStatus: number,
    isSuccess: boolean,
    navigationTarget: NavigationTarget
}

const useQueryNavigationTargetResponse = (target: NavigationTarget): UseQueryResult<NavigationTargetResponse<HalResponse | string>, Error> => {
    const resourceUrl = toHref(target);

    return useQuery<NavigationTargetResponse<HalResponse>>({
        queryKey: [resourceUrl], queryFn: async (context): Promise<NavigationTargetResponse<HalResponse>> => {
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
                }
                return {
                    contentType: res.headers.get("Content-Type") || "???",
                    responseStatus: res.status,
                    body: bodyText,
                    isSuccess: false,
                    navigationTarget: target
                }
            }
            return {
                body: await res.json(),
                contentType: res.headers.get("Content-Type") || '???',
                responseStatus: res.status,
                isSuccess: true,
                navigationTarget: target
            };
        }
    });
}

export const useNavigationTargetResponse = (target?: NavigationTarget): NavigationTargetResponse<HalResponse | string> | undefined => {
    const navigation = useHalExplorerNavigation();

    const navigationTarget = target || navigation.current;

    const result = useQueryNavigationTargetResponse(navigationTarget);

    if (result.isLoading) {
        return undefined;
    } else {
        return result.data;
    }
}

export const useResponseBody = (defaultBody?: HalResponse): HalResponse | undefined => {
    const result = useNavigationTargetResponse();

    if (!result) {
        return defaultBody;
    }

    if (typeof result.body === "string") {
        throw new FetchError(`Response body couldn't be fetched: returned status ${result.responseStatus} and body ${JSON.stringify(result.body)}`, result.responseStatus, '', result.body);
    } else {
        return result.body;
    }
}

