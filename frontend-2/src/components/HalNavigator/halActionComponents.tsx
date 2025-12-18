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
        return (l.title || l.name || rel) as string;
    };
    return (
        <Stack direction={"row"} spacing={2}>
            {Object.entries(links)
                .filter(([rel, _link]) => !COLLECTION_LINK_RELS.includes(rel) || showPagingNavigation)
                .map(([rel, link]) => {
                    if (rel === "self") return null;
                    const singleLink = Array.isArray(link) ? link[0] : link;
                    return (
                        <Button
                            key={rel}
                            variant="text"
                            onClick={() => onClick(singleLink)}
                            aria-label={`Přejít na ${getLabel(rel, singleLink)}`}
                        >
                            {getLabel(rel, singleLink)}
                        </Button>
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
                const singleLink = Array.isArray(link) ? link[0] : link;
                return (
                    <Button
                        key={rel}
                        onClick={() => onClick(singleLink)}
                        aria-label={`${getLabel(rel, singleLink)} - akce`}
                    >
                        {getLabel(rel, singleLink)}
                    </Button>
                );
            })}
        </Stack>
    );
}