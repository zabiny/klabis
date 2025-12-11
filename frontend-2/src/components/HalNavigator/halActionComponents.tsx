import type {HalFormsTemplate, Link, NavigationTarget} from "../../api";
import {type ReactElement} from "react";
import {Button, Link as MuiLink, Stack} from "@mui/material";

const COLLECTION_LINK_RELS = ["prev", "next", "last", "first"];

export function HalLinksUi({links, onClick, showPagingNavigation = true}: {
    links: Record<string, Link | Link[]>,
    onClick: (link: NavigationTarget) => void,
    showPagingNavigation?: boolean
}): ReactElement {
    const getLabel = (rel: string, l: Link): string => {
        const anyLink = l as unknown as { title?: string; name?: string };
        return anyLink.title || anyLink.name || rel;
    };
    return (
        <Stack direction={"row"} spacing={2}>
            {Object.entries(links)
                .filter(([rel, _link]) => !COLLECTION_LINK_RELS.includes(rel) || showPagingNavigation)
                .map(([rel, link]) => {
                    if (rel === "self") return null;
                    const singleLink = Array.isArray(link) ? link[0] : link;
                    return (
                        <MuiLink key={rel}
                                 onClick={() => onClick(singleLink)}>{getLabel(rel, singleLink)}</MuiLink>
                    );
                })}
        </Stack>
    );
}

export function HalActionsUi({links, onClick}: {
    links: Record<string, HalFormsTemplate | NavigationTarget>,
    onClick: (link: NavigationTarget | HalFormsTemplate) => void
}): ReactElement {
    const getLabel = (rel: string, l: HalFormsTemplate | NavigationTarget): string => {
        const obj = l as unknown as { title?: string; name?: string };
        return obj?.title || obj?.name || rel;
    };
    return (
        <Stack direction={"row"} spacing={2}>
            {Object.entries(links).map(([rel, link]) => {
                if (rel === "self") return null;
                const singleLink = Array.isArray(link) ? (link as Link[])[0] : link;
                return (
                    <Button key={rel}
                            onClick={() => onClick(singleLink)}>{getLabel(rel, singleLink as any)}</Button>
                );
            })}
        </Stack>
    );
}