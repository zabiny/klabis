import '@testing-library/jest-dom';
import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useNavigate, useSearchParams } from 'react-router-dom';
import { FulltextSearchInput } from './FulltextSearchInput';

// Reads the URL param value and exposes it in a testid span — used to verify URL writes
const ParamReader = ({ paramName = 'q' }: { paramName?: string }) => {
    const [params] = useSearchParams();
    const value = params.get(paramName) ?? '(none)';
    return <span data-testid="param-value">{value}</span>;
};

// Renders a button that navigates to a new URL — simulates external URL changes (back/forward)
const NavButton = ({ to, label }: { to: string; label: string }) => {
    const navigate = useNavigate();
    return <button onClick={() => navigate(to)}>{label}</button>;
};

const renderInput = ({
    initialUrl = '/',
    paramName,
}: { initialUrl?: string; paramName?: string } = {}) => {
    const utils = render(
        <MemoryRouter initialEntries={[initialUrl]}>
            <Routes>
                <Route
                    path="*"
                    element={
                        <>
                            <FulltextSearchInput
                                paramName={paramName}
                                placeholder="Hledat..."
                                ariaLabel="Fulltext hledání"
                            />
                            <ParamReader paramName={paramName ?? 'q'} />
                        </>
                    }
                />
            </Routes>
        </MemoryRouter>,
    );
    return utils;
};

// Type into input using fireEvent (synchronous, compatible with fake timers)
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

        it('shows empty input when URL has no q param', () => {
            renderInput();
            expect(screen.getByRole('textbox')).toHaveValue('');
        });

        it('shows initial value from URL q param', () => {
            renderInput({ initialUrl: '/?q=abc' });
            expect(screen.getByRole('textbox')).toHaveValue('abc');
        });

        it('shows initial value from custom paramName', () => {
            render(
                <MemoryRouter initialEntries={['/?search=hello']}>
                    <Routes>
                        <Route
                            path="*"
                            element={
                                <FulltextSearchInput
                                    paramName="search"
                                    placeholder="Hledat..."
                                    ariaLabel="Fulltext hledání"
                                />
                            }
                        />
                    </Routes>
                </MemoryRouter>,
            );
            expect(screen.getByRole('textbox')).toHaveValue('hello');
        });
    });

    describe('debounce', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('does not write to URL before 250ms', () => {
            renderInput();
            const input = screen.getByRole('textbox');

            typeInto(input, 'abc');

            // Advance only 100ms — debounce not yet fired
            act(() => {
                vi.advanceTimersByTime(100);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('(none)');
        });

        it('writes to URL after 250ms debounce with >= 2 chars', () => {
            renderInput();
            const input = screen.getByRole('textbox');

            typeInto(input, 'abc');

            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('abc');
        });

        it('resets debounce on each change — single URL update at end', () => {
            renderInput();
            const input = screen.getByRole('textbox');

            typeInto(input, 'ab');
            act(() => {
                vi.advanceTimersByTime(100);
            });
            // Still inside debounce window — URL not yet updated
            expect(screen.getByTestId('param-value')).toHaveTextContent('(none)');

            typeInto(input, 'abc');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            // Single final URL update with full value
            expect(screen.getByTestId('param-value')).toHaveTextContent('abc');
        });
    });

    describe('min-chars gate', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('does not set URL param when input has 1 char', () => {
            renderInput();
            const input = screen.getByRole('textbox');

            typeInto(input, 'a');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('(none)');
        });

        it('does not set URL param for whitespace-only input', () => {
            renderInput();
            const input = screen.getByRole('textbox');

            typeInto(input, '  ');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('(none)');
        });

        it('sets URL param when input has exactly 2 chars', () => {
            renderInput();
            const input = screen.getByRole('textbox');

            typeInto(input, 'ab');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('ab');
        });

        it('removes URL param when input falls below min chars after having been set', () => {
            renderInput({ initialUrl: '/?q=abc' });
            const input = screen.getByRole('textbox');
            expect(input).toHaveValue('abc');
            expect(screen.getByTestId('param-value')).toHaveTextContent('abc');

            typeInto(input, 'a');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('(none)');
        });

        it('writes trimmed value to URL param', () => {
            renderInput();
            const input = screen.getByRole('textbox');

            typeInto(input, ' ab ');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('ab');
        });
    });

    describe('external URL sync', () => {
        it('updates input when URL param changes externally (back/forward)', async () => {
            const user = userEvent.setup();
            render(
                <MemoryRouter initialEntries={['/?q=abc']}>
                    <Routes>
                        <Route
                            path="*"
                            element={
                                <>
                                    <FulltextSearchInput
                                        placeholder="Hledat..."
                                        ariaLabel="Fulltext hledání"
                                    />
                                    <NavButton to="/?q=xyz" label="go-xyz" />
                                </>
                            }
                        />
                    </Routes>
                </MemoryRouter>,
            );

            expect(screen.getByRole('textbox')).toHaveValue('abc');

            await user.click(screen.getByRole('button', { name: 'go-xyz' }));

            expect(screen.getByRole('textbox')).toHaveValue('xyz');
        });

        it('clears input when URL param is removed externally', async () => {
            const user = userEvent.setup();
            render(
                <MemoryRouter initialEntries={['/?q=hello']}>
                    <Routes>
                        <Route
                            path="*"
                            element={
                                <>
                                    <FulltextSearchInput
                                        placeholder="Hledat..."
                                        ariaLabel="Fulltext hledání"
                                    />
                                    <NavButton to="/" label="go-home" />
                                </>
                            }
                        />
                    </Routes>
                </MemoryRouter>,
            );

            expect(screen.getByRole('textbox')).toHaveValue('hello');

            await user.click(screen.getByRole('button', { name: 'go-home' }));

            expect(screen.getByRole('textbox')).toHaveValue('');
        });
    });

    describe('custom paramName', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('writes to the custom param name in URL', () => {
            renderInput({ paramName: 'search' });
            const input = screen.getByRole('textbox');

            typeInto(input, 'test');
            act(() => {
                vi.advanceTimersByTime(300);
            });

            expect(screen.getByTestId('param-value')).toHaveTextContent('test');
        });
    });
});
