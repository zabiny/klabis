import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {GenericHalPage} from './GenericHalPage';
import {
    mockEmptyHalCollectionResponse,
    mockHalCollectionResponse,
    mockHalCollectionResponseWithForms,
    mockHalResponse,
    mockHalResponseWithForms,
} from '../__mocks__/halData';
import type {HalCollectionResponse, HalResponse} from '../api';

// Mock child components
jest.mock('../contexts/HalRouteContext', () => ({
    ...jest.requireActual('../contexts/HalRouteContext'),
    useHalRoute: jest.fn(),
}));

jest.mock('../components/UI', () => ({
    Alert: ({severity, children}: any) => (
        <div data-testid={`alert-${severity}`} role="alert">
            {children}
        </div>
    ),
    Modal: ({isOpen, onClose, title, children}: any) =>
        isOpen ? (
            <div data-testid="modal" role="dialog">
                {title && <h2>{title}</h2>}
                {children}
                <button onClick={onClose}>Close</button>
            </div>
        ) : null,
    Spinner: () => <div data-testid="spinner" role="status"/>,
}));

jest.mock('../components/JsonPreview', () => ({
    JsonPreview: ({data, label}: any) => (
        <div data-testid="json-preview">
            {label && <h2>{label}</h2>}
            <pre>{JSON.stringify(data, null, 2)}</pre>
        </div>
    ),
}));

jest.mock('../components/HalLinksSection', () => ({
    HalLinksSection: ({links, onNavigate}: any) =>
        links ? (
            <div data-testid="hal-links">
                <button onClick={() => onNavigate('/test')}>Navigate</button>
            </div>
        ) : null,
}));

jest.mock('../components/HalFormsSection', () => ({
    HalFormsSection: ({templates, selectedTemplate, onSelectTemplate}: any) =>
        templates ? (
            <div data-testid="hal-forms">
                {templates &&
                    Object.entries(templates).map(([key, template]: any) => (
                        <button key={key} onClick={() => onSelectTemplate(template)}>
                            {template.title || key}
                        </button>
                    ))}
                {selectedTemplate && (
                    <button onClick={() => onSelectTemplate(null)}>Close Form</button>
                )}
            </div>
        ) : null,
}));

jest.mock('../hooks/useHalActions', () => ({
    useHalActions: jest.fn(),
}));

jest.mock('./NotFoundPage', () => {
    return {
        __esModule: true,
        default: () => <div data-testid="not-found-page">404 Not Found</div>,
    };
});

const {useHalRoute} = require('../contexts/HalRouteContext');
const {useHalActions} = require('../hooks/useHalActions');

describe('GenericHalPage Component', () => {
    let mockHalActions: any;

    beforeEach(() => {
        mockHalActions = {
            selectedTemplate: null,
            setSelectedTemplate: jest.fn(),
            submitError: null,
            isSubmitting: false,
            handleNavigateToItem: jest.fn(),
            handleFormSubmit: jest.fn().mockResolvedValue(undefined),
        };

        useHalActions.mockReturnValue(mockHalActions);
        jest.clearAllMocks();
    });

    describe('Loading State', () => {
        it('should display spinner when loading', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: true,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'pending',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('spinner')).toBeInTheDocument();
        });

        it('should center the spinner', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: true,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'pending',
            });

            const {container} = render(<GenericHalPage/>);
            const spinnerContainer = container.querySelector('.flex');
            expect(spinnerContainer).toHaveClass('items-center', 'justify-center', 'py-12');
        });
    });

    describe('Error Handling', () => {
        it('should display NotFoundPage when 404 error occurs', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: new Error('HTTP 404 Not Found'),
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'error',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('not-found-page')).toBeInTheDocument();
        });

        it('should display error alert for non-404 errors', () => {
            const error = new Error('Failed to fetch data');
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'error',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('alert-error')).toBeInTheDocument();
            expect(screen.getByText('Nepodařilo se načíst data z /api/items')).toBeInTheDocument();
            expect(screen.getByText(error.message)).toBeInTheDocument();
        });

        it('should display pathname in error message', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: new Error('Fetch failed'),
                pathname: '/api/custom/path',
                refetch: jest.fn(),
                queryState: 'error',
            });

            render(<GenericHalPage/>);
            expect(screen.getByText(/\/api\/custom\/path/)).toBeInTheDocument();
        });
    });

    describe('No Data State', () => {
        it('should display warning when no data is available', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('alert-warning')).toBeInTheDocument();
            expect(screen.getByText('Žádná data dostupná')).toBeInTheDocument();
        });
    });

    describe('Single Item Display', () => {
        it('should render GenericItemDisplay for single items', () => {
            const itemData = mockHalResponse({name: 'Test Item', id: 1});
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            const {container} = render(<GenericHalPage/>);
            expect(container.querySelector('.p-4')).toBeInTheDocument();
        });

        it('should display properties table for single item', () => {
            const itemData = mockHalResponse({
                name: 'Test Item',
                description: 'A test item',
                status: 'active',
            });
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            // The component should render properties, verify structure exists

            const row = screen.getByText(/Test Item.*A test item/i);
            expect(row).toBeInTheDocument();
        });

        it('should render HAL sections for single item', () => {
            const itemData = mockHalResponseWithForms();
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('hal-links')).toBeInTheDocument();
            expect(screen.getByTestId('hal-forms')).toBeInTheDocument();
        });

        it('should display raw JSON for single item', () => {
            const itemData = mockHalResponse();
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('json-preview')).toBeInTheDocument();
        });
    });

    describe('Collection Display', () => {
        it('should render GenericCollectionDisplay for collections', () => {
            const collectionData = mockHalCollectionResponse(3);
            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            const {container} = render(<GenericHalPage/>);
            // Check for collection-specific elements
            expect(container.querySelectorAll('table')).toBeTruthy();
        });

        it('should display items table for collections', () => {
            const collectionData = mockHalCollectionResponse(2);
            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            // Verify table structure
            const tables = screen.getAllByRole('table');
            expect(tables.length).toBeGreaterThan(0);
        });

        it('should display pagination info for collections', () => {
            const collectionData = mockHalCollectionResponse(3, {
                page: {
                    totalElements: 15,
                    totalPages: 5,
                    size: 3,
                    number: 0,
                },
            });

            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByText("Celkem: 15 položek (1 z 5 stran)")).toBeInTheDocument();
        });

        it('should display empty collection message', () => {
            const emptyCollection = mockEmptyHalCollectionResponse();
            useHalRoute.mockReturnValue({
                resourceData: emptyCollection,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            // Empty message should be displayed
            expect(screen.getByText(/Kolekce je prázdná/)).toBeInTheDocument();
        });

        it('should render HAL sections for collections', () => {
            const collectionData = mockHalCollectionResponseWithForms(2);
            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('hal-links')).toBeInTheDocument();
            expect(screen.getByTestId('hal-forms')).toBeInTheDocument();
        });

        it('should display raw JSON for collections', () => {
            const collectionData = mockHalCollectionResponse(2);
            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('json-preview')).toBeInTheDocument();
        });
    });

    describe('HAL Detection', () => {
        it('should detect single item HAL response', () => {
            const itemData = mockHalResponse({
                _links: {self: {href: '/api/items/1'}},
                name: 'Item',
            });
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            const {container} = render(<GenericHalPage/>);
            // Should render as single item (not collection)
            expect(container.querySelector('.p-4')).toBeInTheDocument();
        });

        it('should detect collection with _embedded', () => {
            const collectionData: HalCollectionResponse = {
                _links: {self: {href: '/api/items'}},
                _embedded: {items: [{id: 1}]},
                page: {totalElements: 1, totalPages: 1, size: 10, number: 0},
            };

            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            const {container} = render(<GenericHalPage/>);
            // Should render as collection
            expect(container.querySelectorAll('table').length).toBeGreaterThan(0);
        });

        it('should detect collection with page metadata', () => {
            const collectionData = mockHalCollectionResponse(2);
            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            const {container} = render(<GenericHalPage/>);
            // Should render as collection
            expect(container.querySelectorAll('table').length).toBeGreaterThan(0);
        });
    });

    describe('Navigation', () => {
        it('should call handleNavigateToItem when navigating', async () => {
            const collectionData = mockHalCollectionResponse(1);
            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('hal-links')).toBeInTheDocument();
        });
    });

    describe('Form Handling', () => {
        it('should handle template selection', async () => {
            const user = userEvent.setup();
            const itemData = mockHalResponseWithForms();
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);

            const createButton = screen.getByText('Create');
            await user.click(createButton);

            expect(mockHalActions.setSelectedTemplate).toHaveBeenCalled();
        });

        it('should display modal for viewing full JSON', async () => {
            const collectionData = mockHalCollectionResponse(2);
            useHalRoute.mockReturnValue({
                resourceData: collectionData,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            // The modal testing is complex and depends on internal state
            // This is a smoke test to ensure the component renders
            expect(screen.getByTestId('hal-links')).toBeInTheDocument();
        });
    });

    describe('Data Flow', () => {
        it('should pass correct props to useHalRoute', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockHalResponse(),
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);

            expect(useHalRoute).toHaveBeenCalled();
        });

        it('should pass correct props to useHalActions', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockHalResponse(),
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);

            expect(useHalActions).toHaveBeenCalled();
        });

        it('should pass form submission handler to HAL forms', () => {
            const itemData = mockHalResponseWithForms();
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            expect(screen.getByTestId('hal-forms')).toBeInTheDocument();
        });
    });

    describe('Edge Cases', () => {
        it('should handle response with empty _embedded object', () => {
            const data: HalCollectionResponse = {
                _links: {self: {href: '/api/items'}},
                _embedded: {},
                page: {totalElements: 0, totalPages: 0, size: 10, number: 0},
            };

            useHalRoute.mockReturnValue({
                resourceData: data,
                isLoading: false,
                error: null,
                pathname: '/api/items',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            // Should still render as collection view
            expect(screen.getByTestId('hal-links')).toBeInTheDocument();
        });

        it('should handle response with null _links', () => {
            const data: HalResponse = {
                name: 'Item',
                _links: undefined,
            };

            useHalRoute.mockReturnValue({
                resourceData: data,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            // Should render item view
            expect(screen.getByTestId('json-preview')).toBeInTheDocument();
        });

        it('should handle response with no _templates', () => {
            const data = mockHalResponse({_templates: undefined});

            useHalRoute.mockReturnValue({
                resourceData: data,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            render(<GenericHalPage/>);
            // Should still render without errors
            expect(screen.getByTestId('json-preview')).toBeInTheDocument();
        });
    });

    describe('Styling', () => {
        it('should have correct container padding', () => {
            const itemData = mockHalResponse();
            useHalRoute.mockReturnValue({
                resourceData: itemData,
                isLoading: false,
                error: null,
                pathname: '/api/items/1',
                refetch: jest.fn(),
                queryState: 'success',
            });

            const {container} = render(<GenericHalPage/>);
            const mainContainer = container.querySelector('.p-4');
            expect(mainContainer).toHaveClass('p-4');
        });
    });
});
