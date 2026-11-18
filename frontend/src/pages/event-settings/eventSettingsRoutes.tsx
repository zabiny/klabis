import {Navigate, Outlet, Route} from 'react-router-dom';
import {GenericHalPage} from '../GenericHalPage.tsx';
import {EventTypesPage} from '../events/EventTypesPage.tsx';
import {CategoryPresetsPage} from '../events/CategoryPresetsPage.tsx';

const BASE = '/event-settings';

export const eventSettingsRoutes = (
    <>
        <Route path={BASE} element={<Outlet/>}>
            <Route path="disciplines" element={<GenericHalPage/>}/>
            <Route path="event-types" element={<EventTypesPage/>}/>
            <Route path="category-presets" element={<CategoryPresetsPage/>}/>
        </Route>
        <Route path="/disciplines" element={<Navigate to={`${BASE}/disciplines`} replace/>}/>
        <Route path="/event-types" element={<Navigate to={`${BASE}/event-types`} replace/>}/>
        <Route path="/category-presets" element={<Navigate to={`${BASE}/category-presets`} replace/>}/>
    </>
);
