import type {HalResourceLinks} from "../../api";
import {isLink} from "../../api/klabisJsonUtils";
import {isString} from "formik";

// Generic HAL fetcher
export function toHref(source: HalResourceLinks): string {
    if (Array.isArray(source)) {
        return toHref(source[0])
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
