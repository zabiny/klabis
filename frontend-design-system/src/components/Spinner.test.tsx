import {render, screen} from '@testing-library/react';
import {Spinner} from './Spinner';

describe('Spinner Component', () => {
    describe('Rendering', () => {
        it('should render spinner element', () => {
            const {container} = render(<Spinner/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toBeInTheDocument();
        });

        it('should have animate-spin class', () => {
            const {container} = render(<Spinner/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveClass('animate-spin');
        });

        it('should have correct border classes', () => {
            const {container} = render(<Spinner/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveClass('rounded-full', 'border-current', 'border-t-transparent');
        });
    });

    describe('Size Variants', () => {
        it('should render with md size by default', () => {
            const {container} = render(<Spinner/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveClass('w-6', 'h-6', 'border-2');
        });

        it('should render with sm size', () => {
            const {container} = render(<Spinner size="sm"/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveClass('w-4', 'h-4', 'border-2');
        });

        it('should render with lg size', () => {
            const {container} = render(<Spinner size="lg"/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveClass('w-8', 'h-8', 'border-3');
        });
    });

    describe('Accessibility', () => {
        it('should have status role for accessibility', () => {
            const {container} = render(<Spinner/>);
            expect(container.querySelector('[role="status"]')).toBeInTheDocument();
        });

        it('should have aria-label', () => {
            const {container} = render(<Spinner/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveAttribute('aria-label', 'Loading');
        });

        it('should have sr-only loading text', () => {
            render(<Spinner/>);
            const srText = screen.getByText('Loading...');
            expect(srText).toHaveClass('sr-only');
        });
    });

    describe('Custom Styling', () => {
        it('should apply custom className', () => {
            const {container} = render(<Spinner className="custom-spinner"/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveClass('custom-spinner');
        });

        it('should combine custom and default classes', () => {
            const {container} = render(<Spinner className="text-red-500"/>);
            const spinner = container.querySelector('[role="status"]');
            expect(spinner).toHaveClass('animate-spin', 'text-red-500');
        });
    });

    describe('Display Name', () => {
        it('should have correct displayName', () => {
            expect(Spinner.displayName).toBe('Spinner');
        });
    });
});
