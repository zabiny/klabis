import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {HalPageGate} from './HalPageGate';
import {useHalPageData} from '../hooks/useHalPageData';
import {ApiError} from '../api/apiError';
import {makePageData} from '../__mocks__/pageData';
import type {HalResponse} from '../api';

vi.mock('../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

const mockPageData = (overrides: Parameters<typeof makePageData>[0] = {}) => {
    vi.mocked(useHalPageData).mockReturnValue(makePageData(overrides));
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

    it('delegates the error to ErrorPage instead of rendering its children', () => {
        mockPageData({error: new ApiError(404), queryState: 'error'});

        renderGate(<HalPageGate>{() => <div>obsah</div>}</HalPageGate>);

        expect(screen.getByText('Stránka nenalezena')).toBeInTheDocument();
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
