import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Form, Formik, useFormikContext} from 'formik';
import {vi} from 'vitest';
import {eventFormFieldsFactory} from './eventFormFieldsFactory.tsx';
import type {HalFormsInputProps} from '../HalNavigator2/halforms/types.ts';

vi.mock('./EventTypeSelectField.tsx', () => ({
    EventTypeSelectField: () => <div data-testid="event-type-select-field"/>,
}));

vi.mock('./CategoryPresetPickerButton.tsx', () => ({
    CategoryPresetPickerButton: () => <div data-testid="category-preset-picker"/>,
}));

vi.mock('../HalNavigator2/halforms/fields', async () => {
    const actual = await vi.importActual('../HalNavigator2/halforms/fields');
    return {
        ...(actual as object),
        HalFormsInput: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-input-${prop.name}`}>{prop.prompt}</div>
        ),
        HalFormsSelect: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-select-${prop.name}`}>{prop.prompt}</div>
        ),
        HalFormsMemberId: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-member-id-${prop.name}`}>{prop.prompt}</div>
        ),
        HalFormsCheckboxGroup: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-checkbox-${prop.name}`}>{prop.prompt}</div>
        ),
    };
});

function createConf(name: string, type = 'text', overrides: Partial<HalFormsInputProps> = {}): HalFormsInputProps {
    return {
        prop: {name, type, prompt: name},
        errorText: undefined,
        subElementProps: vi.fn() as unknown as HalFormsInputProps['subElementProps'],
        ...overrides,
    };
}

describe('eventFormFieldsFactory', () => {
    describe('eventTypeId field (B7.1)', () => {
        it('renders EventTypeSelectField for eventTypeId field', () => {
            const result = eventFormFieldsFactory('text', createConf('eventTypeId'));
            render(<div>{result}</div>);
            expect(screen.getByTestId('event-type-select-field')).toBeInTheDocument();
        });

        it('renders EventTypeSelectField regardless of fieldType when field name is eventTypeId', () => {
            const result = eventFormFieldsFactory('UUID', createConf('eventTypeId'));
            render(<div>{result}</div>);
            expect(screen.getByTestId('event-type-select-field')).toBeInTheDocument();
        });
    });

    describe('categories field', () => {
        it('renders CategoryPresetPickerButton alongside categories field', () => {
            const result = eventFormFieldsFactory('text', createConf('categories'));
            render(<div>{result}</div>);
            expect(screen.getByTestId('category-preset-picker')).toBeInTheDocument();
        });
    });

    describe('categories collection field (full integration, real HalFormsCollectionField)', () => {
        // Regression coverage for a bug where eventFormFieldsFactory intercepted the
        // *array* "categories" property itself (fieldType is the element type
        // "CategoryRequest", not "array") and rendered a single CategoryRowField
        // directly instead of letting HalFormsCollectionField iterate the array.
        // That collapsed the field to one row with no add/remove, corrupted the
        // submitted shape (an object instead of an array), and dropped existing
        // categories on edit. Only testing CategoryRowField in isolation (as the
        // earlier tests in this file do) could not catch this — the bug lived
        // entirely in how the two are wired together.
        //
        // conf here mirrors the real live HAL-FORMS template returned by the backend
        // for the `categories` property: {"multi": true, "name": "categories", "type": "CategoryRequest"}
        // Row recursion goes through halFormsFieldsFactory's 3rd `customFactory` param
        // (see HalFormsFieldFactory.tsx D1-D3): eventFormFieldsFactory is a plain
        // HalFormFieldFactory that composes its row logic via expandHalFormsFieldFactory,
        // so calling it directly at the top level exercises the real production wiring.
        const categoriesConf = (): HalFormsInputProps => ({
            prop: {name: 'categories', type: 'CategoryRequest', multi: true, prompt: 'Kategorie'},
            errorText: undefined,
            subElementProps: vi.fn() as unknown as HalFormsInputProps['subElementProps'],
        });

        const CapturedValues = ({onValues}: {onValues: (values: {categories: unknown[]}) => void}) => {
            const {values} = useFormikContext<{categories: unknown[]}>();
            onValues(values);
            return null;
        };

        const renderCategoriesField = (initialCategories: unknown[], onValues?: (values: {categories: unknown[]}) => void) => {
            return render(
                <Formik initialValues={{categories: initialCategories}} onSubmit={vi.fn()}>
                    <Form>
                        {eventFormFieldsFactory('CategoryRequest', categoriesConf())}
                        {onValues && <CapturedValues onValues={onValues}/>}
                    </Form>
                </Formik>
            );
        };

        it('loads existing categories into separate rows, preserving id', () => {
            const {container} = renderCategoriesField([
                {id: 'cat-1', name: 'H21'},
                {id: 'cat-2', name: 'D21'},
                {id: 'cat-3', name: 'H35'},
            ]);

            expect(container.querySelectorAll('[data-testid="collection-item"]')).toHaveLength(3);
            expect(screen.getByDisplayValue('H21')).toBeInTheDocument();
            expect(screen.getByDisplayValue('D21')).toBeInTheDocument();
            expect(screen.getByDisplayValue('H35')).toBeInTheDocument();
        });

        it('shows an add button and a remove button per row', () => {
            renderCategoriesField([{id: 'cat-1', name: 'H21'}]);

            expect(screen.getByRole('button', {name: /přidat/i})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /odebrat/i})).toBeInTheDocument();
        });

        it('clicking add appends a new row without id, keeping existing rows and their ids intact', async () => {
            const user = userEvent.setup();
            let capturedValues: {categories: unknown[]} = {categories: []};
            const {container} = renderCategoriesField(
                [{id: 'cat-1', name: 'H21'}],
                (values) => { capturedValues = values; }
            );

            await user.click(screen.getByRole('button', {name: /přidat/i}));

            expect(container.querySelectorAll('[data-testid="collection-item"]')).toHaveLength(2);
            expect(capturedValues.categories).toEqual([
                {id: 'cat-1', name: 'H21'},
                {},
            ]);
        });

        it('clicking remove on one row deletes only that row, keeping others intact', async () => {
            const user = userEvent.setup();
            let capturedValues: {categories: unknown[]} = {categories: []};
            renderCategoriesField(
                [{id: 'cat-1', name: 'H21'}, {id: 'cat-2', name: 'D21'}],
                (values) => { capturedValues = values; }
            );

            const removeButtons = screen.getAllByRole('button', {name: /odebrat/i});
            await user.click(removeButtons[0]);

            expect(capturedValues.categories).toEqual([{id: 'cat-2', name: 'D21'}]);
        });

        it('submits categories as an array of {id?, name, fee?} objects, not a single object', async () => {
            let capturedValues: {categories: unknown[]} = {categories: []};
            renderCategoriesField(
                [{id: 'cat-1', name: 'H21'}, {id: 'cat-2', name: 'D21'}],
                (values) => { capturedValues = values; }
            );

            expect(Array.isArray(capturedValues.categories)).toBe(true);
            expect(capturedValues.categories).toHaveLength(2);
        });

        it('renders an empty collection with no rows and only the add button when there are no categories', () => {
            const {container} = renderCategoriesField([]);

            expect(container.querySelectorAll('[data-testid="collection-item"]')).toHaveLength(0);
            expect(screen.getByRole('button', {name: /přidat/i})).toBeInTheDocument();
        });
    });

    describe('category row (CategoryRequest field type)', () => {
        const renderCategoryRow = (initialValue: Record<string, unknown>) => {
            const conf = createConf('categories.0', 'CategoryRequest');
            return render(
                <Formik initialValues={{'categories': [initialValue]}} onSubmit={vi.fn()}>
                    <Form>
                        {eventFormFieldsFactory('CategoryRequest', {...conf, prop: {...conf.prop, name: 'categories.0'}})}
                    </Form>
                </Formik>
            );
        };

        it('renders name and fee inputs for a category row', () => {
            renderCategoryRow({id: 'cat-1', name: 'H21', fee: null});
            expect(screen.getByDisplayValue('H21')).toBeInTheDocument();
        });

        it('does not render a visible input for the category id', () => {
            const {container} = renderCategoryRow({id: 'cat-1', name: 'H21', fee: null});
            // id is not rendered as a visible input — it is carried in Formik state only,
            // and must round-trip unchanged on submit so the backend updates in place
            expect(container.querySelector('input[name="categories.0.id"]')).not.toBeInTheDocument();
        });

        it('renders a category row without id for a newly added category', () => {
            const {container} = renderCategoryRow({name: ''});
            expect(container.querySelector('input[name="categories.0.name"]')).toBeInTheDocument();
            expect(container.querySelector('input[name="categories.0.id"]')).not.toBeInTheDocument();
        });

        it('renders fee amount input when category has a fee', () => {
            renderCategoryRow({id: 'cat-1', name: 'H21', fee: {amount: 200, currency: 'CZK'}});
            expect(screen.getByDisplayValue('200')).toBeInTheDocument();
        });
    });

    describe('other fields', () => {
        it('delegates non-event-type, non-categories fields to klabisFieldsFactory', () => {
            const result = eventFormFieldsFactory('text', createConf('name'));
            render(<div>{result}</div>);
            expect(screen.getByTestId('hal-input-name')).toBeInTheDocument();
        });
    });

    describe('shared-service boolean fields (event-shared-transport-accommodation 6.1)', () => {
        // The backend serialises boolean HAL-FORMS properties with type "Boolean"
        // (Spring HATEOAS has no HtmlInputType for java.lang.Boolean).
        const renderField = (name: string) => render(
            <Formik initialValues={{[name]: false}} onSubmit={vi.fn()}>
                <Form>{eventFormFieldsFactory('Boolean', createConf(name, 'Boolean'))}</Form>
            </Formik>
        );

        it('renders a checkbox for sharedTransportEnabled', () => {
            renderField('sharedTransportEnabled');
            expect(screen.getByRole('checkbox')).toBeInTheDocument();
        });

        it('renders a checkbox for sharedAccommodationEnabled', () => {
            renderField('sharedAccommodationEnabled');
            expect(screen.getByRole('checkbox')).toBeInTheDocument();
        });
    });
});
