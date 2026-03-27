import {render, screen} from '@testing-library/react';
import {Button} from './Button';

describe('Button Component', () => {
    describe('Variants', () => {
        it('should render primary variant by default', () => {
            render(<Button>Click me</Button>);
            expect(screen.getByRole('button', {name: 'Click me'})).toHaveClass('bg-primary');
        });

        it('should render secondary variant', () => {
            render(<Button variant="secondary">Click me</Button>);
            expect(screen.getByRole('button', {name: 'Click me'})).toHaveClass('bg-surface');
        });

        it('should render ghost variant', () => {
            render(<Button variant="ghost">Click me</Button>);
            expect(screen.getByRole('button', {name: 'Click me'})).toHaveClass('bg-transparent');
        });

        it('should render danger variant', () => {
            render(<Button variant="danger">Click me</Button>);
            expect(screen.getByRole('button', {name: 'Click me'})).toHaveClass('bg-error');
        });

        it('should render danger-ghost variant with red text on soft background', () => {
            render(<Button variant="danger-ghost">Odhlásit</Button>);
            const btn = screen.getByRole('button', {name: 'Odhlásit'});
            expect(btn).toHaveClass('text-red-600');
            expect(btn).toHaveClass('bg-red-50');
            expect(btn).toHaveClass('hover:bg-red-100');
        });

        it('should render danger-ghost variant with dark mode classes', () => {
            render(<Button variant="danger-ghost">Odhlásit</Button>);
            const btn = screen.getByRole('button', {name: 'Odhlásit'});
            expect(btn).toHaveClass('dark:text-red-400');
            expect(btn).toHaveClass('dark:bg-red-950/50');
            expect(btn).toHaveClass('dark:hover:bg-red-950');
        });
    });

    describe('Loading state', () => {
        it('should show spinner when loading', () => {
            const {container} = render(<Button loading>Saving</Button>);
            const spinner = container.querySelector('.animate-spin');
            expect(spinner).toBeInTheDocument();
        });

        it('should be disabled when loading', () => {
            render(<Button loading>Saving</Button>);
            expect(screen.getByRole('button', {name: 'Saving'})).toBeDisabled();
        });
    });

    describe('Sizes', () => {
        it('should apply sm size classes', () => {
            render(<Button size="sm">Small</Button>);
            expect(screen.getByRole('button', {name: 'Small'})).toHaveClass('px-3', 'py-1.5', 'text-sm');
        });

        it('should apply lg size classes', () => {
            render(<Button size="lg">Large</Button>);
            expect(screen.getByRole('button', {name: 'Large'})).toHaveClass('px-6', 'py-3', 'text-lg');
        });
    });

    describe('Full width', () => {
        it('should apply w-full when fullWidth is true', () => {
            render(<Button fullWidth>Full Width</Button>);
            expect(screen.getByRole('button', {name: 'Full Width'})).toHaveClass('w-full');
        });
    });

    describe('Children', () => {
        it('should render children text', () => {
            render(<Button>Submit</Button>);
            expect(screen.getByText('Submit')).toBeInTheDocument();
        });
    });
});
