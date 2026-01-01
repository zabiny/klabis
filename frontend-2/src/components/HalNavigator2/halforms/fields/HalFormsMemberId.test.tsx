import {fireEvent, render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import {Form, Formik} from 'formik';
import * as Yup from 'yup';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormsMemberId} from './HalFormsMemberId.tsx';
import type {HalFormsInputProps} from '../types.ts';

// Mock useHalFormOptions at module level to provide test member data
vi.mock('../../../hooks/useHalFormOptions', () => ({
    useHalFormOptions: () => ({
        options: [
            {value: '1', label: 'John Smith (ZBM9000)'},
            {value: '2', label: 'Jane Doe (ZBM9001)'},
        ],
        isLoading: false,
        error: null,
    }),
}));

/**
 * Tests for HalFormsMemberId component
 *
 * Verifies that member selection dropdown displays with a clear button (X icon)
 * that appears only when a value is selected.
 */
describe('HalFormsMemberId', () => {

    const renderWithFormik = (initialValues: any, onSubmit: any) => {
        const validationSchema = Yup.object().shape({
            coordinator: Yup.string(),
        });

        const queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });

        return render(
            <QueryClientProvider client={queryClient}>
                <Formik
                    initialValues={initialValues}
                    validationSchema={validationSchema}
                    onSubmit={onSubmit}
                >
                    {() => (
                        <Form>
                            <HalFormsMemberId
                                prop={{
                                    name: 'coordinator',
                                    prompt: 'Select Coordinator',
                                    type: 'MemberId',
                                }}
                                errorText={undefined}
                                subElementProps={vi.fn()}
                            />
                        </Form>
                    )}
                </Formik>
            </QueryClientProvider>
        );
    };

    it('should not show clear button when field is empty', () => {
        // Act
        renderWithFormik({coordinator: ''}, vi.fn());

        // Assert
        const clearButton = screen.queryByTestId('clear-member-button');
        expect(clearButton).not.toBeInTheDocument();
    });

    it('should show clear button when field has a value', () => {
        // Act
        renderWithFormik({coordinator: '1'}, vi.fn());

        // Assert
        const clearButton = screen.getByTestId('clear-member-button');
        expect(clearButton).toBeInTheDocument();
    });

    it('should clear field value when clear button is clicked', () => {
        // Arrange
        const handleSubmit = vi.fn();
        renderWithFormik({coordinator: '1'}, handleSubmit);

        // Act
        const clearButton = screen.getByTestId('clear-member-button');
        fireEvent.click(clearButton);

        // Assert - clear button should disappear (value is now empty)
        expect(screen.queryByTestId('clear-member-button')).not.toBeInTheDocument();
    });

    it('should hide clear button when field is read-only', () => {
        // Arrange
        const mockProp: HalFormsInputProps = {
            prop: {
                name: 'coordinator',
                prompt: 'Select Coordinator',
                type: 'MemberId',
                readOnly: true,
            },
            errorText: undefined,
            subElementProps: vi.fn(),
        };

        const queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });

        // Act
        render(
            <QueryClientProvider client={queryClient}>
                <Formik initialValues={{coordinator: '1'}} onSubmit={vi.fn()}>
                    {() => (
                        <Form>
                            <HalFormsMemberId {...mockProp} />
                        </Form>
                    )}
                </Formik>
            </QueryClientProvider>
        );

        // Assert - clear button should not show for read-only fields
        const clearButton = screen.queryByTestId('clear-member-button');
        expect(clearButton).not.toBeInTheDocument();
    });

    it('should use provided label and member options', () => {
        const queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });

        // Act
        render(
            <QueryClientProvider client={queryClient}>
                <Formik initialValues={{coordinator: ''}} onSubmit={vi.fn()}>
                    {() => (
                        <Form>
                            <HalFormsMemberId
                                prop={{
                                    name: 'coordinator',
                                    prompt: 'Vedoucí',
                                    type: 'MemberId',
                                }}
                                errorText={undefined}
                                subElementProps={vi.fn()}
                            />
                        </Form>
                    )}
                </Formik>
            </QueryClientProvider>
        );

        // Assert - field should be present with correct label
        const field = screen.getByLabelText('Vedoucí');
        expect(field).toBeInTheDocument();
    });
});
