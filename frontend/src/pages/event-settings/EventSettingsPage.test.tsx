import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {EventSettingsPage} from './EventSettingsPage';
import {useRootNavigation, type NavigationItem} from '../../hooks/useRootNavigation';

vi.mock('../../hooks/useRootNavigation', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../hooks/useRootNavigation')>()),
    useRootNavigation: vi.fn(),
}));

const tab = (rel: string, href: string): NavigationItem => ({
    rel, href, label: rel, section: 'event-settings-tab',
});

const ALL_TABS = [
    tab('disciplines', '/disciplines'),
    tab('event-types', '/event-types'),
    tab('category-presets', '/category-presets'),
];

const mockNavigation = (items: NavigationItem[] | undefined, isLoading = false) => {
    vi.mocked(useRootNavigation).mockReturnValue({data: items, isLoading} as never);
};

const Location = () => <div data-testid="location">{useLocation().pathname}</div>;

const renderAt = (path: string) =>
    render(
        <MemoryRouter initialEntries={[path]}>
            <Location/>
            <Routes>
                <Route path="/event-settings" element={<EventSettingsPage/>}>
                    <Route path="disciplines" element={<div>disciplines-content</div>}/>
                    <Route path="event-types" element={<div>event-types-content</div>}/>
                    <Route path="category-presets" element={<div>category-presets-content</div>}/>
                </Route>
            </Routes>
        </MemoryRouter>
    );

describe('EventSettingsPage', () => {
    beforeEach(() => {
        mockNavigation(ALL_TABS);
    });

    it('shows a tab for each available link', () => {
        renderAt('/event-settings/event-types');

        expect(screen.getByRole('tab', {name: 'Disciplíny'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Typy akcí'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Šablony kategorií'})).toBeInTheDocument();
    });

    it('marks the tab from the URL as selected and renders its content', () => {
        renderAt('/event-settings/category-presets');

        expect(screen.getByRole('tab', {name: 'Šablony kategorií'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('tab', {name: 'Disciplíny'})).toHaveAttribute('aria-selected', 'false');
        expect(screen.getByText('category-presets-content')).toBeInTheDocument();
    });

    it('hides tabs without a root link', () => {
        mockNavigation([tab('event-types', '/event-types'), tab('category-presets', '/category-presets')]);

        renderAt('/event-settings/event-types');

        expect(screen.queryByRole('tab', {name: 'Disciplíny'})).not.toBeInTheDocument();
        expect(screen.getAllByRole('tab')).toHaveLength(2);
    });

    it('redirects from an unavailable tab to the first available one', () => {
        mockNavigation([tab('event-types', '/event-types'), tab('category-presets', '/category-presets')]);

        renderAt('/event-settings/disciplines');

        expect(screen.getByTestId('location')).toHaveTextContent('/event-settings/event-types');
        expect(screen.getByText('event-types-content')).toBeInTheDocument();
    });

    it('redirects the bare address to the first available tab', () => {
        renderAt('/event-settings');

        expect(screen.getByTestId('location')).toHaveTextContent('/event-settings/disciplines');
    });

    it('navigates to another tab address on click', async () => {
        renderAt('/event-settings/disciplines');

        await userEvent.click(screen.getByRole('tab', {name: 'Typy akcí'}));

        expect(await screen.findByText('event-types-content')).toBeInTheDocument();
    });

    it('shows ForbiddenPage and no tabs when no tab is available', () => {
        mockNavigation([]);

        renderAt('/event-settings/disciplines');

        expect(screen.getByText('Přístup odepřen')).toBeInTheDocument();
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    });

    it('renders nothing definitive while navigation is loading', () => {
        mockNavigation(undefined, true);

        renderAt('/event-settings/disciplines');

        expect(screen.queryByText('Přístup odepřen')).not.toBeInTheDocument();
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    });
});
