import type {ReactElement} from 'react';
import {Navigate, Route} from 'react-router-dom';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {useRootNavigation} from '../../hooks/useRootNavigation.ts';
import {EventTypesPage} from '../events/EventTypesPage.tsx';
import {CategoryPresetsPage} from '../events/CategoryPresetsPage.tsx';
import {DisciplinesPage} from './DisciplinesPage.tsx';
import {EVENT_SETTINGS_BASE, EventSettingsPage} from './EventSettingsPage.tsx';

// HalRouteProvider derives the HAL resource from the browser path, which under /event-settings/<tab>
// is not an API resource, so each tab is pinned to its root-API link.
const TabResource = ({rel, children}: { rel: string; children: ReactElement }): ReactElement | null => {
    const {data: items} = useRootNavigation();
    const item = items?.find(i => i.rel === rel);
    if (!item) return null;
    return <HalRouteProvider routeLink={{href: item.href}}>{children}</HalRouteProvider>;
};

export const eventSettingsRoutes = (
    <>
        <Route path={EVENT_SETTINGS_BASE} element={<EventSettingsPage/>}>
            <Route path="disciplines"
                   element={<TabResource rel="disciplines"><DisciplinesPage/></TabResource>}/>
            <Route path="event-types"
                   element={<TabResource rel="event-types"><EventTypesPage/></TabResource>}/>
            <Route path="category-presets"
                   element={<TabResource rel="category-presets"><CategoryPresetsPage/></TabResource>}/>
        </Route>
        <Route path="/disciplines" element={<Navigate to={`${EVENT_SETTINGS_BASE}/disciplines`} replace/>}/>
        <Route path="/event-types" element={<Navigate to={`${EVENT_SETTINGS_BASE}/event-types`} replace/>}/>
        <Route path="/category-presets" element={<Navigate to={`${EVENT_SETTINGS_BASE}/category-presets`} replace/>}/>
    </>
);
