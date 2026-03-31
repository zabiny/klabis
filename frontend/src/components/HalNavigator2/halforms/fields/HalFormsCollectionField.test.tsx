import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {HalFormsCollectionField} from './HalFormsCollectionField.tsx';
import type {HalFormsInputProps} from '../types.ts';
import type {HalFormsProperty} from '../../../../api';

const createProp = (overrides: Partial<HalFormsProperty> = {}): HalFormsProperty => ({
    name: 'tags',
    prompt: 'Tagy',
    type: 'text',
    multiple: true,
    required: false,
    readOnly: false,
    ...overrides,
});

const createProps = (
    prop: HalFormsProperty,
    fieldFactory = vi.fn().mockReturnValue(null)
): HalFormsInputProps => ({
    prop,
    errorText: undefined,
    subElementProps: vi.fn(),
    fieldFactory,
});

const renderWithFormik = (
    props: HalFormsInputProps,
    initialValue: unknown[] = []
) => {
    return render(
        <Formik
            initialValues={{[props.prop.name]: initialValue}}
            onSubmit={vi.fn()}
        >
            <Form>
                <HalFormsCollectionField {...props} />
            </Form>
        </Formik>
    );
};

describe('HalFormsCollectionField', () => {
    describe('empty state', () => {
        it('shows only Add button when collection is empty', () => {
            const props = createProps(createProp());
            renderWithFormik(props, []);

            expect(screen.getByRole('button', {name: /přidat/i})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /odebrat/i})).not.toBeInTheDocument();
        });

        it('shows field label as section header', () => {
            const props = createProps(createProp({prompt: 'Tagy'}));
            renderWithFormik(props, []);

            expect(screen.getByText('Tagy')).toBeInTheDocument();
        });
    });

    describe('adding items', () => {
        it('renders a field instance after clicking Add', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp(), mockFactory);
            renderWithFormik(props, []);

            await userEvent.click(screen.getByRole('button', {name: /přidat/i}));

            expect(screen.getByTestId('inner-field')).toBeInTheDocument();
        });

        it('renders Remove button for each item', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp(), mockFactory);
            renderWithFormik(props, []);

            await userEvent.click(screen.getByRole('button', {name: /přidat/i}));
            await userEvent.click(screen.getByRole('button', {name: /přidat/i}));

            expect(screen.getAllByRole('button', {name: /odebrat/i})).toHaveLength(2);
        });

        it('initializes new item as empty string for primitive types', async () => {
            let capturedProps: HalFormsInputProps | undefined;
            const mockFactory = vi.fn().mockImplementation((_, conf: HalFormsInputProps) => {
                capturedProps = conf;
                return <input data-testid="inner-field" />;
            });
            const props = createProps(createProp({type: 'text'}), mockFactory);
            renderWithFormik(props, []);

            await userEvent.click(screen.getByRole('button', {name: /přidat/i}));

            expect(capturedProps).toBeDefined();
            expect(capturedProps!.prop.name).toBe('tags.0');
        });
    });

    describe('removing items', () => {
        it('removes item when Remove button is clicked', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp(), mockFactory);
            renderWithFormik(props, ['a', 'b']);

            const removeButtons = screen.getAllByRole('button', {name: /odebrat/i});
            expect(removeButtons).toHaveLength(2);

            await userEvent.click(removeButtons[0]);

            expect(screen.getAllByRole('button', {name: /odebrat/i})).toHaveLength(1);
        });
    });

    describe('composite type items', () => {
        it('wraps each composite item in a bordered container', async () => {
            const mockFactory = vi.fn().mockReturnValue(
                <div data-testid="composite-fields">fields</div>
            );
            const props = createProps(createProp({type: 'GuardianDTO'}), mockFactory);
            renderWithFormik(props, [{}]);

            const items = screen.getAllByTestId('collection-item');
            expect(items[0]).toHaveClass('border');
        });

        it('does not add border container for primitive types', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp({type: 'text'}), mockFactory);
            renderWithFormik(props, ['value']);

            const items = screen.getAllByTestId('collection-item');
            expect(items[0]).not.toHaveClass('border');
        });
    });

    describe('min/max constraints', () => {
        it('disables Add button when max items reached', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp({max: 2}), mockFactory);
            renderWithFormik(props, ['a', 'b']);

            expect(screen.getByRole('button', {name: /přidat/i})).toBeDisabled();
        });

        it('enables Add button when below max count', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp({max: 3}), mockFactory);
            renderWithFormik(props, ['a']);

            expect(screen.getByRole('button', {name: /přidat/i})).not.toBeDisabled();
        });

        it('disables Remove button when at min count', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp({min: 1}), mockFactory);
            renderWithFormik(props, ['a']);

            expect(screen.getByRole('button', {name: /odebrat/i})).toBeDisabled();
        });

        it('enables Remove button when above min count', async () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp({min: 1}), mockFactory);
            renderWithFormik(props, ['a', 'b']);

            const removeButtons = screen.getAllByRole('button', {name: /odebrat/i});
            removeButtons.forEach(btn => expect(btn).not.toBeDisabled());
        });
    });

    describe('field name indexing', () => {
        it('passes indexed field name to inner factory for each item', async () => {
            const capturedNames: string[] = [];
            const mockFactory = vi.fn().mockImplementation((_, conf: HalFormsInputProps) => {
                capturedNames.push(conf.prop.name);
                return <input />;
            });
            const props = createProps(createProp({name: 'guardians'}), mockFactory);
            renderWithFormik(props, ['a', 'b']);

            expect(capturedNames).toContain('guardians.0');
            expect(capturedNames).toContain('guardians.1');
        });
    });

    describe('subElementProps for composite items', () => {
        it('resolves sub-field names relative to the indexed item, not the parent', () => {
            const capturedSubProps: HalFormsInputProps[] = [];
            const mockFactory = vi.fn().mockImplementation((_, conf: HalFormsInputProps) => {
                const subProps = conf.subElementProps('firstName');
                capturedSubProps.push(subProps);
                return <input />;
            });
            const props = createProps(createProp({name: 'guardians', type: 'GuardianDTO'}), mockFactory);
            renderWithFormik(props, [{}, {}]);

            expect(capturedSubProps[0].prop.name).toBe('guardians.0.firstName');
            expect(capturedSubProps[1].prop.name).toBe('guardians.1.firstName');
        });
    });

    describe('read-only', () => {
        it('hides Add and Remove buttons when field is readOnly', () => {
            const mockFactory = vi.fn().mockReturnValue(<input data-testid="inner-field" />);
            const props = createProps(createProp({readOnly: true}), mockFactory);
            renderWithFormik(props, ['a', 'b']);

            expect(screen.queryByRole('button', {name: /přidat/i})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /odebrat/i})).not.toBeInTheDocument();
        });
    });
});
