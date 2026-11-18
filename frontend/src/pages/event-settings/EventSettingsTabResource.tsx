import type {ReactElement} from 'react';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {useEventSettingsTabs} from '../../hooks/useRootNavigation.ts';

// HalRouteProvider derives the HAL resource from the browser path, which under /event-settings/<tab>
// is not an API resource, so each tab is pinned to its root-API link.
export const EventSettingsTabResource = ({rel, children}: { rel: string; children: ReactElement }): ReactElement | null => {
    const {data: tabs} = useEventSettingsTabs();
    const tab = tabs?.find(i => i.rel === rel);
    if (!tab) return null;
    return <HalRouteProvider routeLink={{href: tab.href}}>{children}</HalRouteProvider>;
};
