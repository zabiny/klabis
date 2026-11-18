import {render, screen} from '@testing-library/react';
import {MemoryRouter, Outlet, Route, Routes, useLocation} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {eventSettingsRoutes} from './eventSettingsRoutes';

vi.mock('../events/EventTypesPage.tsx', () => ({EventTypesPage: () => <div>event-types-page</div>}));
vi.mock('../events/CategoryPresetsPage.tsx', () => ({CategoryPresetsPage: () => <div>category-presets-page</div>}));
vi.mock('../GenericHalPage.tsx', () => ({GenericHalPage: () => <div>disciplines-page</div>}));

const Location = () => <div data-testid="location">{useLocation().pathname}</div>;

const renderAt = (path: string) =>
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/" element={<><Location/><Outlet/></>}>
                    {eventSettingsRoutes}
                </Route>
            </Routes>
        </MemoryRouter>
    );

describe('eventSettingsRoutes', () => {
    it.each([
        ['/event-settings/disciplines', 'disciplines-page'],
        ['/event-settings/event-types', 'event-types-page'],
        ['/event-settings/category-presets', 'category-presets-page'],
    ])('renders tab content at %s', (path, text) => {
        renderAt(path);

        expect(screen.getByText(text)).toBeInTheDocument();
        expect(screen.getByTestId('location')).toHaveTextContent(path);
    });

    it.each([
        ['/disciplines', '/event-settings/disciplines'],
        ['/event-types', '/event-settings/event-types'],
        ['/category-presets', '/event-settings/category-presets'],
    ])('redirects legacy address %s to %s', (legacy, target) => {
        renderAt(legacy);

        expect(screen.getByTestId('location')).toHaveTextContent(target);
    });
});
