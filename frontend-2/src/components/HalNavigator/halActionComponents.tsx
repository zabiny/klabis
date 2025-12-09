import type {NavigationTarget} from "../../api";
import {type ReactElement} from "react";
import {Button, Link as MuiLink, Stack} from "@mui/material";

const COLLECTION_LINK_RELS = ["prev", "next", "last", "first"];

export function HalLinksUi({links, onClick, showPagingNavigation = true}: {
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

export function HalActionsUi({links, onClick}: {
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