import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {BrowserRouter} from 'react-router-dom';
import {HalLinksSection} from './HalLinksSection.tsx';
import * as HalRouteContext from '../../contexts/HalRouteContext';
import {vi} from 'vitest';

const TestWrapper = ({children}: { children: React.ReactNode }) => (
    <BrowserRouter>
        {children}
    </BrowserRouter>
);

const renderWithRouter = (component: React.ReactElement) => {
    return render(component, {wrapper: TestWrapper});
};

describe('HalLinksSection Component', () => {
    beforeEach(() => {
        vi.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: null,
            refetch: async () => {
            },
            pathname: '/test',
            queryState: 'success',
            navigateToResource: vi.fn(),
            getResourceLink: vi.fn()
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('Rendering', () => {
        it('should not render when links are not provided (no resourceData._links)', () => {
            const {container} = renderWithRouter(
                <HalLinksSection/>
            );
            expect(container.firstChild).toBeNull();
        });

        it('should render when resourceData._links is provided automatically', () => {
            vi.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
                resourceData: {
                    _links: {
                        next: {href: '/api/items?page=1'},
                    },
                } as any,
                isLoading: false,
                error: null,
                refetch: async () => {
                },
                pathname: '/test',
                queryState: 'success',
                navigateToResource: vi.fn(),
                getResourceLink: vi.fn()
            });

            const {container} = renderWithRouter(
                <HalLinksSection/>
            );
            expect(container.querySelector('div')).toBeInTheDocument();
        });

        it('should not render when links object is empty', () => {
            const {container} = renderWithRouter(
                <HalLinksSection links={{}} onNavigate={() => {
                }}/>
            );
            expect(container.firstChild).toBeNull();
        });

        it('should render section when links are provided', () => {
            const links = {
                first: {href: '/api/items?page=0'},
                next: {href: '/api/items?page=1'},
            };
            const {container} = renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            expect(container.querySelector('div')).toBeInTheDocument();
        });

        it('should render heading with available actions text', () => {
            const links = {next: {href: '/api/items?page=1'}};
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            // The exact text depends on UI_MESSAGES.AVAILABLE_ACTIONS
            const heading = screen.getByRole('heading', {level: 3});
            expect(heading).toBeInTheDocument();
        });
    });

    describe('Link Filtering', () => {
        it('should filter out self links', () => {
            const links = {
                self: {href: '/api/items/1'},
                next: {href: '/api/items?page=1'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            // Self link should not render as a button
            expect(screen.queryByTitle('self')).not.toBeInTheDocument();
            // But next should render
            const buttons = screen.getAllByRole('button');
            expect(buttons.some((btn) => btn.getAttribute('title') === 'next')).toBe(true);
        });

        it('should render only non-self links', () => {
            const links = {
                self: {href: '/api/items/1'},
                prev: {href: '/api/items?page=0', title: 'Previous'},
                next: {href: '/api/items?page=1', title: 'Next'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            const buttons = screen.getAllByRole('button');
            expect(buttons).toHaveLength(2); // prev and next, not self
        });

        it('should render empty fragment when only self link exists', () => {
            const links = {
                self: {href: '/api/items/1'},
            };
            const {container} = renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            // Should render a fragment (empty)
            expect(container.querySelector('button')).not.toBeInTheDocument();
        });
    });

    describe('Link Rendering', () => {
        it('should render button for each link', () => {
            const links = {
                first: {href: '/api/items?page=0'},
                last: {href: '/api/items?page=10'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            const buttons = screen.getAllByRole('button');
            expect(buttons).toHaveLength(2);
        });

        it('should use link title as button text when available', () => {
            const links = {
                next: {href: '/api/items?page=1', title: 'Go to Next'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            expect(screen.getByText('Go to Next')).toBeInTheDocument();
        });

        it('should use link rel as button text when title not available', () => {
            const links = {
                next: {href: '/api/items?page=1'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            expect(screen.getByText('next')).toBeInTheDocument();
        });

        it('should handle array of links', () => {
            const links = {
                item: [
                    {href: '/api/items/1', title: 'Item 1'},
                    {href: '/api/items/2', title: 'Item 2'},
                ],
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            expect(screen.getByText('Item 1')).toBeInTheDocument();
            expect(screen.getByText('Item 2')).toBeInTheDocument();
        });
    });

    describe('Navigation', () => {
        it('should call onNavigate with href when link is clicked', async () => {
            const user = userEvent.setup();
            const mockOnNavigate = vi.fn();
            const links = {
                next: {href: '/api/items?page=1', title: 'Next'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={mockOnNavigate}/>
            );
            const button = screen.getByText('Next');
            await user.click(button);
            expect(mockOnNavigate).toHaveBeenCalledWith('/api/items?page=1');
        });

        it('should call onNavigate with correct href for array links', async () => {
            const user = userEvent.setup();
            const mockOnNavigate = vi.fn();
            const links = {
                item: [
                    {href: '/api/items/1', title: 'Item 1'},
                    {href: '/api/items/2', title: 'Item 2'},
                ],
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={mockOnNavigate}/>
            );
            const button2 = screen.getByText('Item 2');
            await user.click(button2);
            expect(mockOnNavigate).toHaveBeenCalledWith('/api/items/2');
        });

        it('should handle multiple navigations', async () => {
            const user = userEvent.setup();
            const mockOnNavigate = vi.fn();
            const links = {
                first: {href: '/api/items?page=0'},
                last: {href: '/api/items?page=10'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={mockOnNavigate}/>
            );
            const buttons = screen.getAllByRole('button');
            await user.click(buttons[0]);
            await user.click(buttons[1]);
            expect(mockOnNavigate).toHaveBeenCalledTimes(2);
            expect(mockOnNavigate).toHaveBeenNthCalledWith(1, '/api/items?page=0');
            expect(mockOnNavigate).toHaveBeenNthCalledWith(2, '/api/items?page=10');
        });
    });

    describe('Accessibility', () => {
        it('should have title attribute on buttons', () => {
            const links = {
                next: {href: '/api/items?page=1'},
                prev: {href: '/api/items?page=0'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            const buttons = screen.getAllByRole('button');
            buttons.forEach((btn) => {
                expect(btn).toHaveAttribute('title');
            });
        });

        it('should have proper button styling', () => {
            const links = {
                next: {href: '/api/items?page=1'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            const button = screen.getByText('next');
            expect(button).toHaveClass('px-3', 'py-1', 'bg-blue-600', 'text-white');
        });
    });

    describe('Styling', () => {
        it('should have container styling', () => {
            const links = {
                next: {href: '/api/items?page=1'},
            };
            const {container} = renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            const section = container.querySelector('div');
            expect(section).toHaveClass('mt-4', 'p-4', 'border', 'rounded', 'bg-blue-50');
        });

        it('should have button hover effects', () => {
            const links = {
                next: {href: '/api/items?page=1'},
            };
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={() => {
                }}/>
            );
            const button = screen.getByText('next');
            expect(button).toHaveClass('hover:bg-blue-700');
        });
    });

    describe('Edge Cases', () => {
        it('should handle links with no href', () => {
            const links = {
                broken: {title: 'Broken Link'} as any,
            };
            const mockOnNavigate = vi.fn();
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={mockOnNavigate}/>
            );
            expect(screen.getByText('Broken Link')).toBeInTheDocument();
        });

        it('should handle links with complex hrefs', async () => {
            const user = userEvent.setup();
            const links = {
                next: {
                    href: '/api/items?page=1&sort=name&filter=active',
                    title: 'Next',
                },
            };
            const mockOnNavigate = vi.fn();
            renderWithRouter(
                <HalLinksSection links={links} onNavigate={mockOnNavigate}/>
            );
            const button = screen.getByText('Next');
            await user.click(button);
            // Verify the full href is passed
            expect(mockOnNavigate).toHaveBeenCalledWith('/api/items?page=1&sort=name&filter=active');
        });
    });
});
