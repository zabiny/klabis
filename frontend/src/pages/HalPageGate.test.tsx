import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {HalPageGate} from './HalPageGate';
import {useHalPageData} from '../hooks/useHalPageData';
import {ApiError} from '../api/apiError';
import type {HalResponse} from '../api';

vi.mock('../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

type PageData = ReturnType<typeof useHalPageData>;

const pageData = (overrides: Partial<PageData> = {}): PageData => ({
    resourceData: null,
    isLoading: false,
    error: null,
    queryState: 'success',
    navigateToResource: vi.fn(),
    getResourceLink: vi.fn(() => null),
    refetch: vi.fn(),
    ...overrides,
});

const mockPageData = (overrides: Partial<PageData> = {}) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData(overrides));
};

const renderGate = (ui: React.ReactNode) =>
    render(<MemoryRouter>{ui}</MemoryRouter>);

const isLoadingVisual = (container: HTMLElement): boolean =>
    container.querySelector('.animate-pulse') !== null;

describe('HalPageGate', () => {
    it('renders the default Skeleton while loading', () => {
        mockPageData({isLoading: true, queryState: 'pending'});

        const {container} = renderGate(
            <HalPageGate>{() => <div>obsah</div>}</HalPageGate>,
        );

        expect(isLoadingVisual(container)).toBe(true);
        expect(screen.queryByText('obsah')).not.toBeInTheDocument();
    });

    it('renders the loading prop instead of the default Skeleton', () => {
        mockPageData({isLoading: true, queryState: 'pending'});

        const {container} = renderGate(
            <HalPageGate loading={<div data-testid="custom-loading">Načítání…</div>}>
                {() => <div>obsah</div>}
            </HalPageGate>,
        );

        expect(screen.getByTestId('custom-loading')).toBeInTheDocument();
        expect(isLoadingVisual(container)).toBe(false);
    });

    it('renders the not-found page for an error with status 404', () => {
        mockPageData({error: new ApiError(404), queryState: 'error'});

        renderGate(<HalPageGate>{() => <div>obsah</div>}</HalPageGate>);

        expect(screen.getByText('Stránka nenalezena')).toBeInTheDocument();
        expect(screen.queryByText('obsah')).not.toBeInTheDocument();
    });

    it('renders the forbidden page for an error with status 403', () => {
        mockPageData({error: new ApiError(403), queryState: 'error'});

        renderGate(<HalPageGate>{() => <div>obsah</div>}</HalPageGate>);

        expect(screen.getByText('Přístup odepřen')).toBeInTheDocument();
    });

    it('renders the forbidden page for an error with status 401', () => {
        mockPageData({error: new ApiError(401), queryState: 'error'});

        renderGate(<HalPageGate>{() => <div>obsah</div>}</HalPageGate>);

        expect(screen.getByText('Přístup odepřen')).toBeInTheDocument();
    });

    it('renders the generic error card for any other error', () => {
        mockPageData({error: new ApiError(500), queryState: 'error'});

        renderGate(<HalPageGate>{() => <div>obsah</div>}</HalPageGate>);

        expect(screen.getByText('Něco se pokazilo')).toBeInTheDocument();
        expect(screen.getByText('Došlo k neočekávané chybě. Zkuste to prosím znovu.')).toBeInTheDocument();
        expect(screen.queryByText('obsah')).not.toBeInTheDocument();
    });

    it('renders the loading view when the resource is missing and there is no error', () => {
        mockPageData({resourceData: null, error: null, queryState: 'success'});

        const {container} = renderGate(
            <HalPageGate>{() => <div>obsah</div>}</HalPageGate>,
        );

        expect(isLoadingVisual(container)).toBe(true);
        expect(screen.queryByText('obsah')).not.toBeInTheDocument();
    });

    it('renders nothing while the query is idle', () => {
        mockPageData({queryState: 'idle'});

        const {container} = renderGate(
            <HalPageGate loading={<div data-testid="custom-loading">Načítání…</div>}>
                {() => <div>obsah</div>}
            </HalPageGate>,
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('passes the non-null resource to the child', () => {
        const resource: HalResponse = {name: 'Test Item'};
        mockPageData({resourceData: resource});

        const child = vi.fn((data: HalResponse) => <div>jméno: {String(data.name)}</div>);
        renderGate(<HalPageGate>{child}</HalPageGate>);

        expect(child).toHaveBeenCalledTimes(1);
        expect(child.mock.calls[0][0]).toBe(resource);
        expect(screen.getByText('jméno: Test Item')).toBeInTheDocument();
    });

    it('passes a resource of the typed generic to the child', () => {
        interface MemberDetail extends HalResponse {
            firstName: string;
        }

        mockPageData({resourceData: {firstName: 'Jan'}});

        const child = vi.fn((member: MemberDetail) => <div>jméno: {member.firstName}</div>);
        renderGate(<HalPageGate<MemberDetail>>{child}</HalPageGate>);

        expect(screen.getByText('jméno: Jan')).toBeInTheDocument();
    });
});
