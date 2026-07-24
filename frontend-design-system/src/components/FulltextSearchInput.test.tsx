import '@testing-library/jest-dom';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { FulltextSearchInput } from './FulltextSearchInput';

const renderInput = ({
    value = '',
    onChange = vi.fn(),
    minChars,
    debounceMs,
}: {
    value?: string;
    onChange?: ReturnType<typeof vi.fn>;
    minChars?: number;
    debounceMs?: number;
} = {}) => {
    const utils = render(
        <FulltextSearchInput
            value={value}
            onChange={onChange}
            placeholder="Hledat..."
            ariaLabel="Fulltext hledání"
            minChars={minChars}
            debounceMs={debounceMs}
        />,
    );
    return { ...utils, onChange };
};

const typeInto = (input: HTMLElement, value: string) => {
    fireEvent.change(input, { target: { value } });
};

describe('FulltextSearchInput', () => {
    describe('initial render', () => {
        it('renders input with placeholder', () => {
            renderInput();
            expect(screen.getByPlaceholderText('Hledat...')).toBeInTheDocument();
        });

        it('renders with correct aria-label', () => {
            renderInput();
            expect(screen.getByRole('textbox', { name: 'Fulltext hledání' })).toBeInTheDocument();
        });

        it('shows empty input when value prop is empty', () => {
            renderInput({ value: '' });
            expect(screen.getByRole('textbox')).toHaveValue('');
        });

        it('shows initial value from value prop', () => {
            renderInput({ value: 'abc' });
            expect(screen.getByRole('textbox')).toHaveValue('abc');
        });
    });

    describe('debounce', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('does not call onChange before 250ms', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, 'abc');

            act(() => {
                vi.advanceTimersByTime(100);
            });

            expect(onChange).not.toHaveBeenCalled();
        });

        it('calls onChange after 250ms debounce with >= 2 chars', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, 'abc');

            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledOnce();
            expect(onChange).toHaveBeenCalledWith('abc');
        });

        it('resets debounce on each change — single onChange call at end', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, 'ab');
            act(() => {
                vi.advanceTimersByTime(100);
            });
            expect(onChange).not.toHaveBeenCalled();

            typeInto(input, 'abc');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledOnce();
            expect(onChange).toHaveBeenCalledWith('abc');
        });
    });

    describe('min-chars gate', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('emits "" when input has 1 char (below min-chars)', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, 'a');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledWith('');
        });

        it('emits "" for whitespace-only input', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, '  ');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledWith('');
        });

        it('emits trimmed value when input has exactly 2 chars', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, 'ab');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledWith('ab');
        });

        it('emits "" when input falls below min chars after a valid emit', () => {
            const onChange = vi.fn();
            renderInput({ value: 'abc', onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, 'a');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledWith('');
        });

        it('emits trimmed value', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, ' ab ');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledWith('ab');
        });

        it('clears the input and emits ""', () => {
            const onChange = vi.fn();
            renderInput({ value: 'abc', onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, '');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(onChange).toHaveBeenCalledWith('');
        });
    });

    describe('no-op guard', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('does not call onChange a second time when effective value is unchanged', () => {
            const onChange = vi.fn();
            renderInput({ onChange });
            const input = screen.getByRole('textbox');

            typeInto(input, 'abc');
            act(() => {
                vi.advanceTimersByTime(300);
            });
            expect(onChange).toHaveBeenCalledOnce();

            typeInto(input, 'abc');
            act(() => {
                vi.advanceTimersByTime(300);
            });
            expect(onChange).toHaveBeenCalledOnce();
        });
    });

    describe('external value prop sync', () => {
        it('updates visible input when value prop changes externally', () => {
            const { rerender } = renderInput({ value: 'abc' });
            expect(screen.getByRole('textbox')).toHaveValue('abc');

            rerender(
                <FulltextSearchInput
                    value="xyz"
                    onChange={vi.fn()}
                    placeholder="Hledat..."
                    ariaLabel="Fulltext hledání"
                />,
            );

            expect(screen.getByRole('textbox')).toHaveValue('xyz');
        });

        it('clears visible input when value prop is reset to empty', () => {
            const { rerender } = renderInput({ value: 'hello' });

            rerender(
                <FulltextSearchInput
                    value=""
                    onChange={vi.fn()}
                    placeholder="Hledat..."
                    ariaLabel="Fulltext hledání"
                />,
            );

            expect(screen.getByRole('textbox')).toHaveValue('');
        });
    });
});
