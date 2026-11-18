import {Navigate, Route} from 'react-router-dom';
import {EVENT_SETTINGS_TAB_RELS} from '../../hooks/useRootNavigation.ts';
import {EventTypesPage} from '../events/EventTypesPage.tsx';
import {CategoryPresetsPage} from '../events/CategoryPresetsPage.tsx';
import {DisciplinesPage} from './DisciplinesPage.tsx';
import {EventSettingsPage} from './EventSettingsPage.tsx';
import {EventSettingsTabResource} from './EventSettingsTabResource.tsx';
import {EVENT_SETTINGS_BASE} from './eventSettingsBase.ts';

const TAB_PAGES = {
    'disciplines': <DisciplinesPage/>,
    'event-types': <EventTypesPage/>,
    'category-presets': <CategoryPresetsPage/>,
} as const;

export const eventSettingsRoutes = (
    <>
        <Route path={EVENT_SETTINGS_BASE} element={<EventSettingsPage/>}>
            {EVENT_SETTINGS_TAB_RELS.map(rel => (
                <Route key={rel} path={rel}
                       element={<EventSettingsTabResource rel={rel}>{TAB_PAGES[rel]}</EventSettingsTabResource>}/>
            ))}
        </Route>
        {EVENT_SETTINGS_TAB_RELS.map(rel => (
            <Route key={`legacy-${rel}`} path={`/${rel}`} element={<Navigate to={`${EVENT_SETTINGS_BASE}/${rel}`} replace/>}/>
        ))}
    </>
);
