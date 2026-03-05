import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Alert} from './Alert';
import {vi} from 'vitest';

describe('Alert Component', () => {
    describe('Rendering', () => {
        it('should render alert with children', () => {
            render(<Alert>Test message</Alert>);
            expect(screen.getByText('Test message')).toBeInTheDocument();
        });

        it('should render alert with info severity by default', () => {
            const {container} = render(<Alert>Test</Alert>);
            const alertElement = container.querySelector('[role="alert"]');
            expect(alertElement).toBeInTheDocument();
        });

        it('should render alert with error severity', () => {
            const {container} = render(<Alert severity="error">Error message</Alert>);
            const alertElement = container.querySelector('[role="alert"]');
            expect(alertElement).toHaveClass('bg-alert-error');
        });

        it('should render alert with warning severity', () => {
            const {container} = render(<Alert severity="warning">Warning message</Alert>);
            const alertElement = container.querySelector('[role="alert"]');
            expect(alertElement).toHaveClass('bg-alert-warning');
        });

        it('should render alert with success severity', () => {
            const {container} = render(<Alert severity="success">Success message</Alert>);
            const alertElement = container.querySelector('[role="alert"]');
            expect(alertElement).toHaveClass('bg-alert-success');
        });

        it('should render with custom className', () => {
            const {container} = render(
                <Alert className="custom-class">Test</Alert>,
            );
            const alertElement = container.querySelector('[role="alert"]');
            expect(alertElement).toHaveClass('custom-class');
        });
    });

    describe('Close Button', () => {
        it('should not render close button when onClose is not provided', () => {
            render(<Alert>Test</Alert>);
            const closeButton = screen.queryByLabelText('Close alert');
            expect(closeButton).not.toBeInTheDocument();
        });

        it('should render close button when onClose is provided', () => {
            render(<Alert onClose={() => {
            }}>Test</Alert>);
            const closeButton = screen.getByLabelText('Close alert');
            expect(closeButton).toBeInTheDocument();
        });

        it('should call onClose when close button is clicked', async () => {
            const user = userEvent.setup();
            const mockOnClose = vi.fn();
            render(<Alert onClose={mockOnClose}>Test</Alert>);
            const closeButton = screen.getByLabelText('Close alert');
            await user.click(closeButton);
            expect(mockOnClose).toHaveBeenCalledTimes(1);
        });
    });

    describe('Content Rendering', () => {
        it('should render complex child content', () => {
            render(
                <Alert>
                    <div>
                        <p>Line 1</p>
                        <p>Line 2</p>
                    </div>
                </Alert>,
            );
            expect(screen.getByText('Line 1')).toBeInTheDocument();
            expect(screen.getByText('Line 2')).toBeInTheDocument();
        });

        it('should render alert with HTML content', () => {
            render(
                <Alert>
                    <strong>Important:</strong> This is important
                </Alert>,
            );
            const strongElement = screen.getByText('Important:');
            expect(strongElement.tagName).toBe('STRONG');
        });
    });

    describe('Accessibility', () => {
        it('should have alert role for semantics', () => {
            const {container} = render(<Alert>Test</Alert>);
            expect(container.querySelector('[role="alert"]')).toBeInTheDocument();
        });

        it('should have accessible close button label', () => {
            render(<Alert onClose={() => {
            }}>Test</Alert>);
            expect(screen.getByLabelText('Close alert')).toBeInTheDocument();
        });
    });
});
