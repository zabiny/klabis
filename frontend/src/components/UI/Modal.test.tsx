import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Modal} from './Modal';
import {vi} from 'vitest';

describe('Modal Component', () => {
    describe('Visibility', () => {
        it('should not render when isOpen is false', () => {
            const {container} = render(
                <Modal isOpen={false} onClose={() => {
                }}>
                    Modal content
                </Modal>,
            );
            expect(container.firstChild).toBeNull();
        });

        it('should render when isOpen is true', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Modal content
                </Modal>,
            );
            expect(screen.getByText('Modal content')).toBeInTheDocument();
        });

        it('should render dialog role when open', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const dialog = container.querySelector('[role="dialog"]');
            expect(dialog).toBeInTheDocument();
        });
    });

    describe('Title', () => {
        it('should render title when provided', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }} title="Test Title">
                    Content
                </Modal>,
            );
            expect(screen.getByText('Test Title')).toBeInTheDocument();
        });

        it('should not render title when not provided', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const titleElement = screen.queryByRole('heading');
            expect(titleElement).not.toBeInTheDocument();
        });

        it('should have modal-title id on title element', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }} title="Test">
                    Content
                </Modal>,
            );
            const title = container.querySelector('#modal-title');
            expect(title).toBeInTheDocument();
            expect(title?.textContent).toBe('Test');
        });
    });

    describe('Close Button', () => {
        it('should render close button by default', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const closeButton = screen.getByLabelText('Close modal');
            expect(closeButton).toBeInTheDocument();
        });

        it('should not render close button when closeButton is false', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }} closeButton={false}>
                    Content
                </Modal>,
            );
            const closeButton = screen.queryByLabelText('Close modal');
            expect(closeButton).not.toBeInTheDocument();
        });

        it('should call onClose when close button is clicked', async () => {
            const user = userEvent.setup();
            const mockOnClose = vi.fn();
            render(
                <Modal isOpen={true} onClose={mockOnClose}>
                    Content
                </Modal>,
            );
            const closeButton = screen.getByLabelText('Close modal');
            await user.click(closeButton);
            expect(mockOnClose).toHaveBeenCalledTimes(1);
        });
    });

    describe('Backdrop', () => {
        it('should render backdrop when open', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const backdrop = container.querySelector('[aria-hidden="true"]');
            expect(backdrop).toBeInTheDocument();
            expect(backdrop).toHaveClass('bg-black', 'bg-opacity-60');
        });

        it('should call onClose when backdrop is clicked', async () => {
            const user = userEvent.setup();
            const mockOnClose = vi.fn();
            const {container} = render(
                <Modal isOpen={true} onClose={mockOnClose}>
                    Content
                </Modal>,
            );
            const backdrop = container.querySelector('[aria-hidden="true"]') as HTMLElement;
            await user.click(backdrop);
            expect(mockOnClose).toHaveBeenCalledTimes(1);
        });
    });

    describe('Size Variants', () => {
        it('should apply sm size class', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }} size="sm">
                    Content
                </Modal>,
            );
            const modal = container.querySelector('.max-w-sm');
            expect(modal).toBeInTheDocument();
        });

        it('should apply md size class by default', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const modal = container.querySelector('.max-w-md');
            expect(modal).toBeInTheDocument();
        });

        it('should apply lg size class', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }} size="lg">
                    Content
                </Modal>,
            );
            const modal = container.querySelector('.max-w-lg');
            expect(modal).toBeInTheDocument();
        });

        it('should apply xl size class', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }} size="xl">
                    Content
                </Modal>,
            );
            const modal = container.querySelector('.max-w-xl');
            expect(modal).toBeInTheDocument();
        });
    });

    describe('Footer', () => {
        it('should not render footer when not provided', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const footerBorders = container.querySelectorAll('.border-t');
            expect(footerBorders.length).toBe(0); // No footer border when footer not provided
        });

        it('should render footer when provided', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }} footer={<button>OK</button>}>
                    Content
                </Modal>,
            );
            const footerButton = screen.getByText('OK');
            expect(footerButton).toBeInTheDocument();
        });

        it('should render footer content correctly', () => {
            render(
                <Modal
                    isOpen={true}
                    onClose={() => {
                    }}
                    footer={
                        <div>
                            <button>Cancel</button>
                            <button>Confirm</button>
                        </div>
                    }
                >
                    Content
                </Modal>,
            );
            expect(screen.getByText('Cancel')).toBeInTheDocument();
            expect(screen.getByText('Confirm')).toBeInTheDocument();
        });
    });

    describe('Content', () => {
        it('should render children content', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    <p>Test content</p>
                </Modal>,
            );
            expect(screen.getByText('Test content')).toBeInTheDocument();
        });

        it('should render complex children', () => {
            render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    <div>
                        <h3>Form Title</h3>
                        <input placeholder="Name"/>
                        <button>Submit</button>
                    </div>
                </Modal>,
            );
            expect(screen.getByPlaceholderText('Name')).toBeInTheDocument();
            expect(screen.getByText('Submit')).toBeInTheDocument();
        });
    });

    describe('Accessibility', () => {
        it('should have dialog role and aria-modal', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const dialog = container.querySelector('[role="dialog"]');
            expect(dialog).toHaveAttribute('aria-modal', 'true');
        });

        it('should have aria-labelledby when title is provided', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }} title="Modal Title">
                    Content
                </Modal>,
            );
            const dialog = container.querySelector('[role="dialog"]');
            expect(dialog).toHaveAttribute('aria-labelledby', 'modal-title');
        });

        it('should not have aria-labelledby when title is not provided', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }}>
                    Content
                </Modal>,
            );
            const dialog = container.querySelector('[role="dialog"]');
            expect(dialog).not.toHaveAttribute('aria-labelledby');
        });
    });

    describe('Custom Styling', () => {
        it('should apply custom className', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {
                }} className="custom-modal">
                    Content
                </Modal>,
            );
            const modal = container.querySelector('.custom-modal');
            expect(modal).toBeInTheDocument();
        });
    });

    describe('Extended Size Variants', () => {
        it('should apply max-w-2xl when size is 2xl', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} size="2xl">
                    Content
                </Modal>
            )
            expect(container.querySelector('.max-w-2xl')).toBeInTheDocument()
        })

        it('should apply max-w-4xl when size is 4xl', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} size="4xl">
                    Content
                </Modal>
            )
            expect(container.querySelector('.max-w-4xl')).toBeInTheDocument()
        })
    });

    describe('CloseOnBackdropClick Prop', () => {
        it('should not call onClose when backdrop is clicked and closeOnBackdropClick is false', async () => {
            const user = userEvent.setup()
            const mockOnClose = vi.fn()
            const {container} = render(
                <Modal isOpen={true} onClose={mockOnClose} closeOnBackdropClick={false}>
                    Content
                </Modal>
            )
            const backdrop = container.querySelector('[aria-hidden="true"]') as HTMLElement
            await user.click(backdrop)
            expect(mockOnClose).not.toHaveBeenCalled()
        })

        it('should not call onClose when content is clicked (propagation stopped)', async () => {
            const user = userEvent.setup()
            const mockOnClose = vi.fn()
            render(
                <Modal isOpen={true} onClose={mockOnClose}>
                    <div data-testid="inner-content">Content</div>
                </Modal>
            )
            await user.click(screen.getByTestId('inner-content'))
            expect(mockOnClose).not.toHaveBeenCalled()
        })
    });

    describe('Header Icon Prop', () => {
        it('should render icon before the title', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} title="Nadpis" headerIcon={<svg data-testid="header-icon"/>}>
                    Content
                </Modal>,
            );
            const icon = screen.getByTestId('header-icon');
            const title = container.querySelector('#modal-title');
            expect(icon).toBeInTheDocument();
            expect(icon.compareDocumentPosition(title!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        });

        it('should tint the header with primary-subtle when headerIcon is provided', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} title="Nadpis" headerIcon={<svg/>}>
                    Content
                </Modal>,
            );
            expect(container.querySelector('[data-testid="modal-header"]')).toHaveClass('bg-primary-subtle');
        });

        it('should keep base header background without headerIcon', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} title="Nadpis">
                    Content
                </Modal>,
            );
            expect(container.querySelector('[data-testid="modal-header"]')).toHaveClass('bg-surface-base');
            expect(container.querySelector('[data-testid="modal-header"]')).not.toHaveClass('bg-primary-subtle');
        });

        it('should render compact close button with header icon', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} title="Nadpis" headerIcon={<svg/>}>
                    Content
                </Modal>,
            );
            expect(container.querySelector('[data-testid="modal-close-button"]')).toHaveClass('h-8', 'w-8');
        });
    });

    describe('Context Strip Prop', () => {
        it('should render context strip above the content inside scroll area', () => {
            render(
                <Modal isOpen={true} onClose={() => {}} context={<div data-testid="context-content">Event name</div>}>
                    Form content
                </Modal>,
            );
            const strip = screen.getByTestId('modal-context');
            expect(strip).toBeInTheDocument();
            expect(strip).toHaveTextContent('Event name');
            const scrollArea = strip.parentElement!;
            expect(scrollArea).toHaveClass('overflow-y-auto');
            expect(scrollArea).toContainElement(screen.getByText('Form content'));
        });

        it('should style context strip with subtle background and bottom border', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} context={<div>ctx</div>}>
                    Content
                </Modal>,
            );
            expect(container.querySelector('[data-testid="modal-context"]')).toHaveClass('bg-bg-subtle', 'border-b', 'border-border', 'px-6', 'py-4');
        });

        it('should not render context strip when not provided', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}}>
                    Content
                </Modal>,
            );
            expect(container.querySelector('[data-testid="modal-context"]')).not.toBeInTheDocument();
        });
    });

    describe('Footer Note Prop', () => {
        it('should render footer note in the left slot', () => {
            render(
                <Modal isOpen={true} onClose={() => {}} footerNote={<span>Poznámka</span>}>
                    Content
                </Modal>,
            );
            expect(screen.getByTestId('modal-footer-note')).toHaveTextContent('Poznámka');
        });

        it('should render footer when only footerNote is provided', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}} footerNote={<span>Poznámka</span>}>
                    Content
                </Modal>,
            );
            expect(container.querySelector('.border-t')).toBeInTheDocument();
        });

        it('should not render footer area when neither footer nor footerNote provided', () => {
            const {container} = render(
                <Modal isOpen={true} onClose={() => {}}>
                    Content
                </Modal>,
            );
            expect(container.querySelector('.border-t')).not.toBeInTheDocument();
        });
    });

});
