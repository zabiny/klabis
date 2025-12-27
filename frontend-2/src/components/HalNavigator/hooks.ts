import type {Navigation} from "../../hooks/useNavigation";
import type {HalResourceLinks, HalResponse, NavigationTarget} from "../../api";
import {isTemplateTarget} from "../../api";
import type {HalFormFieldFactory} from "../HalFormsForm";
import {isLink} from "../../api/klabisJsonUtils";
import {isString} from "formik";
import {createContext, useContext} from "react";
import {useQuery, type UseQueryResult} from "@tanstack/react-query";
import {authorizedFetch} from "../../api/authorizedFetch";


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
    try {
        const res = await authorizedFetch(url, {
            headers: {
                Accept: "application/prs.hal-forms+json,application/hal+json",
            },
        });
        return res.json();
    } catch (error) {
        if (error instanceof Error) {
            const statusMatch = error.message.match(/HTTP (\d+)/);
            const status = statusMatch ? parseInt(statusMatch[1], 10) : 0;
            throw new FetchError(error.message, status, error.message, error.message);
        }
        throw error;
    }
}

export function toHref(source: NavigationTarget | HalResourceLinks): string {
    if (Array.isArray(source)) {
        return toHref(source[0])
    } else if (isTemplateTarget(source)) {
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

export function toURLPath(item: NavigationTarget | HalResourceLinks): string {
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
    fieldsFactory?: HalFormFieldFactory
}

export const HalNavigatorContext = createContext<HalNavigatorContextData | null>(null);

export const useHalExplorerNavigation = (): Navigation<NavigationTarget> => {
    const ctx = useContext(HalNavigatorContext);
    if (!ctx) {
        throw new Error("HalNavigatorContext not provided");
    }
    return ctx.navigation;
}

export const useHalNavigator = (): HalNavigatorContextData => {
    const ctx = useContext(HalNavigatorContext);
    if (!ctx) {
        throw new Error("HalNavigatorContext not provided");
    }
    return ctx;
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

    // Query Key Convention: ['hal-navigation', resourceUrl]
    // Stale Time: 5 minutes - HAL responses rarely change
    // Cache Time (gcTime): 5 minutes - keep for quick navigation back
    // Retry: 0 - don't retry; handle responses (success or error) gracefully without retrying
    return useQuery<NavigationTargetResponse<HalResponse | string>>({
        queryKey: ['hal-navigation', resourceUrl],
        queryFn: async (): Promise<NavigationTargetResponse<HalResponse | string>> => {
            const res = await authorizedFetch(
                resourceUrl,
                {
                    headers: {
                        Accept: "application/prs.hal-forms+json,application/hal+json",
                    },
                },
                false // Don't throw - handle error responses gracefully
            );

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
        },
        staleTime: 5 * 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 0,
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

