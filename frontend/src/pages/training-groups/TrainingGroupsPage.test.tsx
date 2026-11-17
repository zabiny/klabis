import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {makePageData} from '../../__mocks__/pageData';
import {TrainingGroupsPage} from './TrainingGroupsPage';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../contexts/halFormContext.ts', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: undefined, error: null}),
}));

const renderPage = (resourceData: HalResponse) => {
    vi.mocked(useHalPageData).mockReturnValue(makePageData({
        resourceData,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/training-groups'}),
    }));
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/training-groups']}>
                <TrainingGroupsPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('TrainingGroupsPage', () => {
    it('renders the group list for a loaded resource', () => {
        renderPage({_links: {self: {href: 'http://localhost/api/training-groups'}}});

        expect(screen.getByRole('heading', {name: 'Tréninkové skupiny', level: 1})).toBeInTheDocument();
        expect(screen.getByText('Název')).toBeInTheDocument();
    });
});
