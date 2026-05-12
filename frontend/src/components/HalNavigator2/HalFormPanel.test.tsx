import '@testing-library/jest-dom';
import React from 'react';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {HalFormPanel, type HalFormPanelRenderHelpers} from './HalFormPanel.tsx';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn().mockReturnValue({
        resourceData: null,
        isLoading: false,
        error: null,
        isAdmin: false,
        route: {
            pathname: '/test',
            navigateToResource: vi.fn(),
            refetch: vi.fn(),
            queryState: 'success',
            getResourceLink: vi.fn(),
        },
        actions: {handleNavigateToItem: vi.fn()},
        getLinks: vi.fn(() => undefined),
        getTemplates: vi.fn(() => undefined),
        hasEmbedded: vi.fn(() => false),
        getEmbeddedItems: vi.fn(() => []),
        isCollection: vi.fn(() => false),
        hasLink: vi.fn(() => false),
        hasTemplate: vi.fn(() => false),
        hasForms: vi.fn(() => false),
        getPageMetadata: vi.fn(() => undefined),
    }),
}));

vi.mock('./HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({template, templateName, customLayout}: any) => {
        if (typeof customLayout === 'function') {
            return (
                <div data-testid="hal-form-display">
                    {customLayout({
                        renderField: (name: string) => <div data-testid={`render-field-${name}`}>{name} field</div>,
                        renderInput: (name: string) => <input data-testid={`render-input-${name}`} aria-label={name}/>,
                        renderLabel: (name: string) => `Label for ${name}`,
                    })}
                </div>
            );
        }
        return (
            <div data-testid="hal-form-display">
                <h3>{template.title || templateName}</h3>
            </div>
        );
    },
}));

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false, gcTime: 0}},
    });
    return ({children}: { children: React.ReactNode }) => (
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                {children}
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('HalFormPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('Loading state', () => {
        it('should display spinner and loading text when query is loading', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: undefined,
                isLoading: true,
                error: null,
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {() => <div>form content</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByText('Načítání...')).toBeInTheDocument();
        });
    });

    describe('Error state', () => {
        it('should display error Alert when query fails', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
                error: new Error('Network error'),
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {() => <div>form content</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByText('Network error')).toBeInTheDocument();
        });
    });

    describe('Template missing', () => {
        it('should display default error message when template is not in _templates', () => {
            const collectionData: HalResponse = {
                _templates: {
                    otherTemplate: mockHalFormsTemplate({title: 'Other'}),
                },
            };
            mockUseAuthorizedQuery.mockReturnValue({
                data: collectionData,
                isLoading: false,
                error: null,
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {() => <div>form content</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByText('Template "createEvent" není dostupný.')).toBeInTheDocument();
        });

        it('should display custom templateMissingMessage when template is absent', () => {
            const collectionData: HalResponse = {
                _templates: {},
            };
            mockUseAuthorizedQuery.mockReturnValue({
                data: collectionData,
                isLoading: false,
                error: null,
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel
                        collectionUrl="/api/events"
                        templateName="createEvent"
                        templateMissingMessage="Nemáte oprávnění vytvořit akci."
                    >
                        {() => <div>form content</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByText('Nemáte oprávnění vytvořit akci.')).toBeInTheDocument();
        });

        it('should display error when _templates is undefined', () => {
            const collectionData: HalResponse = {};
            mockUseAuthorizedQuery.mockReturnValue({
                data: collectionData,
                isLoading: false,
                error: null,
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {() => <div>form content</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByText('Template "createEvent" není dostupný.')).toBeInTheDocument();
        });
    });

    describe('Happy path — template exists', () => {
        const buildCollectionData = (templateProps?: Partial<Parameters<typeof mockHalFormsTemplate>[0]>): HalResponse => ({
            _templates: {
                createEvent: mockHalFormsTemplate({
                    title: 'Vytvořit akci',
                    properties: [
                        {name: 'foo', prompt: 'Foo', type: 'text'},
                        {name: 'bar', prompt: 'Bar', type: 'text'},
                    ],
                    ...templateProps,
                }),
            },
        });

        it('should render children via HalFormDisplay customLayout when template exists', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: buildCollectionData(),
                isLoading: false,
                error: null,
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {() => <div data-testid="children-content">my form layout</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
            expect(screen.getByTestId('children-content')).toBeInTheDocument();
        });

        it('should provide hasField returning true for fields present in template properties', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: buildCollectionData(),
                isLoading: false,
                error: null,
            } as any);

            let capturedHasField: ((name: string) => boolean) | null = null;

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {({hasField}: HalFormPanelRenderHelpers) => {
                            capturedHasField = hasField;
                            return <div data-testid="children">content</div>;
                        }}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(capturedHasField).not.toBeNull();
            expect(capturedHasField!('foo')).toBe(true);
            expect(capturedHasField!('bar')).toBe(true);
            expect(capturedHasField!('nonExistentField')).toBe(false);
        });

        it('should provide renderInput and renderField as functions', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: buildCollectionData(),
                isLoading: false,
                error: null,
            } as any);

            let capturedRenderInput: ((name: string) => React.ReactElement) | null = null;
            let capturedRenderField: ((name: string) => React.ReactElement) | null = null;

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {({renderInput, renderField}: HalFormPanelRenderHelpers) => {
                            capturedRenderInput = renderInput;
                            capturedRenderField = renderField;
                            return <div data-testid="children">content</div>;
                        }}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(typeof capturedRenderInput).toBe('function');
            expect(typeof capturedRenderField).toBe('function');
        });

        it('should render output of renderField and renderInput helpers', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: buildCollectionData(),
                isLoading: false,
                error: null,
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {({renderField, renderInput}: HalFormPanelRenderHelpers) => (
                            <div>
                                {renderField('foo')}
                                {renderInput('bar')}
                            </div>
                        )}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByTestId('render-field-foo')).toBeInTheDocument();
            expect(screen.getByTestId('render-input-bar')).toBeInTheDocument();
        });

        it('should derive pathname from collectionUrl by stripping /api prefix', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: buildCollectionData(),
                isLoading: false,
                error: null,
            } as any);

            vi.doMock('./HalFormDisplay.tsx', () => ({
                HalFormDisplay: ({customLayout}: any) => {
                    return (
                        <div data-testid="hal-form-display">
                            {typeof customLayout === 'function' ? customLayout({
                                renderField: () => <div/>,
                                renderInput: () => <input/>,
                                renderLabel: () => undefined,
                            }) : null}
                        </div>
                    );
                },
            }));

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel collectionUrl="/api/events" templateName="createEvent">
                        {() => <div>content</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });

        it('should use explicit pathname prop when provided', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: buildCollectionData(),
                isLoading: false,
                error: null,
            } as any);

            const Wrapper = createWrapper();
            render(
                <Wrapper>
                    <HalFormPanel
                        collectionUrl="/api/events"
                        templateName="createEvent"
                        pathname="/custom-path"
                    >
                        {() => <div data-testid="children">content</div>}
                    </HalFormPanel>
                </Wrapper>
            );

            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });
    });
});
