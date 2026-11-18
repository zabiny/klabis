import {render, screen} from '@testing-library/react';
import {MemoryRouter, Outlet, Route, Routes, useLocation} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {eventSettingsRoutes} from './eventSettingsRoutes';
import {useEventSettingsTabs, type NavigationItem} from '../../hooks/useRootNavigation';

vi.mock('../../hooks/useRootNavigation', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../hooks/useRootNavigation')>()),
    useEventSettingsTabs: vi.fn(),
}));
vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalRouteProvider: ({routeLink, children}: { routeLink: { href: string }; children: React.ReactNode }) =>
        <div data-testid="route-link" data-href={routeLink.href}>{children}</div>,
}));
vi.mock('../events/EventTypesPage.tsx', () => ({EventTypesPage: () => <div>event-types-page</div>}));
vi.mock('../events/CategoryPresetsPage.tsx', () => ({CategoryPresetsPage: () => <div>category-presets-page</div>}));
vi.mock('./DisciplinesPage.tsx', () => ({DisciplinesPage: () => <div>disciplines-page</div>}));

const items: NavigationItem[] = [
    {rel: 'disciplines', href: '/disciplines', label: 'Disciplíny', section: 'admin'},
    {rel: 'event-types', href: '/event-types', label: 'Typy akcí', section: 'admin'},
    {rel: 'category-presets', href: '/category-presets', label: 'Šablony', section: 'admin'},
];

const Location = () => <div data-testid="location">{useLocation().pathname}</div>;

const renderAt = (path: string) => {
    vi.mocked(useEventSettingsTabs).mockReturnValue({data: items, isLoading: false} as never);
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/" element={<><Location/><Outlet/></>}>
                    {eventSettingsRoutes}
                </Route>
            </Routes>
        </MemoryRouter>
    );
};

describe('eventSettingsRoutes', () => {
    it.each([
        ['/event-settings/disciplines', 'disciplines-page', '/disciplines'],
        ['/event-settings/event-types', 'event-types-page', '/event-types'],
        ['/event-settings/category-presets', 'category-presets-page', '/category-presets'],
    ])('renders tab content at %s inside its root-link HAL route', (path, text, href) => {
        renderAt(path);

        expect(screen.getByText(text)).toBeInTheDocument();
        expect(screen.getByTestId('location')).toHaveTextContent(path);
        expect(screen.getByTestId('route-link')).toHaveAttribute('data-href', href);
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
