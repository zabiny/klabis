import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {halFormsFieldsFactory} from './HalFormsFieldFactory.tsx';
import type {HalFormsProperty} from '../../../api';
import type {HalFormsInputProps} from './types.ts';
import {vi} from 'vitest';

vi.mock('../../../hooks/useHalFormOptions', () => ({
    useHalFormOptions: vi.fn(() => ({
        options: [
            {value: 'A', label: 'A'},
            {value: 'B', label: 'B'},
        ],
        isLoading: false,
        error: null,
    })),
}));

const makeProps = (prop: HalFormsProperty): HalFormsInputProps => ({
    prop,
    errorText: undefined,
    subElementProps: vi.fn(),
});

const renderInFormik = (element: React.ReactElement | null, initialValue: string = '') => {
    if (!element) throw new Error('Factory returned null');
    const name = 'testField';
    return render(
        <Formik initialValues={{[name]: initialValue}} onSubmit={vi.fn()}>
            <Form>{element}</Form>
        </Formik>
    );
};

describe('halFormsFieldsFactory', () => {
    describe('select field dispatch', () => {
        it('renders select when type is "select"', () => {
            const prop: HalFormsProperty = {
                name: 'testField',
                type: 'select',
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('select', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });

        it('renders select when type is "text" but options are present', () => {
            const prop: HalFormsProperty = {
                name: 'testField',
                type: 'text',
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });

        it('renders text input when type is "text" and no options present', () => {
            const prop: HalFormsProperty = {
                name: 'testField',
                type: 'text',
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('textbox')).toBeInTheDocument();
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });

        it('returns null when options.inline is an empty array', () => {
            const prop: HalFormsProperty = {
                name: 'category',
                type: 'text',
                options: {inline: []},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            expect(element).toBeNull();
        });

        it('renders select when options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'category',
                type: 'text',
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });
    });

    describe('boolean / checkbox dispatch', () => {
        // The backend emits boolean HAL-FORMS properties with type "Boolean"
        // (Spring HATEOAS has no HtmlInputType for java.lang.Boolean, so Klabis'
        // KlabisHalFormsPropertyMetadataWrapper falls back to the class simple name).
        it.each(['Boolean', 'boolean', 'checkbox'])('renders a checkbox when type is "%s"', (type) => {
            const prop: HalFormsProperty = {name: 'testField', type};
            const element = halFormsFieldsFactory(type, makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('checkbox')).toBeInTheDocument();
        });

        it('renders the checkbox unchecked when the form value is falsy', () => {
            const prop: HalFormsProperty = {name: 'testField', type: 'Boolean'};
            const element = halFormsFieldsFactory('Boolean', makeProps(prop));
            renderInFormik(element, '');
            expect(screen.getByRole('checkbox')).not.toBeChecked();
        });
    });

    describe('multi-select with inline options (no custom widget)', () => {
        // D7: a multi property with options and no custom widget for its field type renders
        // the generic searchable multi-select (chips), not a checkbox group — the checkbox
        // group remains available for callers that render it directly (e.g.
        // MembershipFeeTierMultiSelect), but the base factory no longer dispatches to it.
        it('renders the multi-select combobox when multi=true and options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'disciplineIds',
                type: 'number',
                multi: true,
                options: {inline: [{value: '1', prompt: 'Orientační běh'}, {value: '3', prompt: 'Lyžařský OB'}]},
            };
            const element = halFormsFieldsFactory('number', makeProps(prop));
            renderInFormik(element, '');
            expect(screen.getByRole('combobox')).toBeInTheDocument();
            expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        });

        it('renders the multi-select combobox when multiple=true and options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'disciplineIds',
                type: 'text',
                multiple: true,
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element, '');
            expect(screen.getByRole('combobox')).toBeInTheDocument();
            expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        });

        it('renders select (not multi-select) when multi=false and options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'disciplineId',
                type: 'text',
                multi: false,
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element, '');
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });
    });

    describe('multi-select with remote (link) options', () => {
        // Regression for the coordinators/disciplineIds bug: a multi property whose options
        // come from a link (not inline) must still render as an array-producing widget. D7
        // splits this in two: no custom widget for the field type -> the generic multi-select;
        // a custom widget exists (e.g. "MemberId") -> per-row via HalFormsCollectionField.
        it('renders the generic multi-select (not the custom widget) when multi=true, options.link is set, and the field type has no custom widget', () => {
            const prop: HalFormsProperty = {
                name: 'disciplineIds',
                type: 'UUID',
                multi: true,
                options: {link: {href: 'http://localhost:8443/api/members/options'}},
            };
            const customFactory = vi.fn(() => null);
            const element = halFormsFieldsFactory('UUID', makeProps(prop), customFactory);
            renderInFormik(element, '');

            expect(customFactory).toHaveBeenCalled();
            expect(screen.queryByTestId('custom-member-picker')).not.toBeInTheDocument();
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });

        it('renders the custom widget per row via HalFormsCollectionField when multi=true, options.link is set, and the field type has a custom widget', () => {
            const prop: HalFormsProperty = {
                name: 'coordinators',
                type: 'MemberId',
                multi: true,
                options: {link: {href: 'http://localhost:8443/api/members/options'}},
            };
            const customFactory = vi.fn(() => <div data-testid="custom-member-picker" />);
            const element = halFormsFieldsFactory('MemberId', makeProps(prop), customFactory);
            render(
                <Formik initialValues={{coordinators: ['m1']}} onSubmit={vi.fn()}>
                    <Form>{element}</Form>
                </Formik>
            );

            expect(customFactory).toHaveBeenCalledWith('MemberId', expect.objectContaining({
                prop: expect.objectContaining({multi: false, multiple: false}),
            }));
            expect(screen.getAllByTestId('custom-member-picker')).toHaveLength(1);
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });

        it('submits an array value when an option is picked for a multi + link-options field with no custom widget', async () => {
            const prop: HalFormsProperty = {
                name: 'disciplineIds',
                type: 'UUID',
                multi: true,
                options: {link: {href: 'http://localhost:8443/api/members/options'}},
            };
            const element = halFormsFieldsFactory('UUID', makeProps(prop), () => null);
            if (!element) throw new Error('Factory returned null');

            const {default: userEvent} = await import('@testing-library/user-event');
            const user = userEvent.setup();
            let submitted: unknown;
            render(
                <Formik
                    initialValues={{disciplineIds: []}}
                    onSubmit={(values) => {
                        submitted = values.disciplineIds;
                    }}
                >
                    {({submitForm}) => (
                        <Form>
                            {element}
                            <button type="button" onClick={submitForm}>Submit</button>
                        </Form>
                    )}
                </Formik>
            );

            await user.click(screen.getByRole('combobox'));
            await user.click(screen.getByRole('option', {name: 'A'}));
            await user.click(screen.getByText('Submit'));

            expect(submitted).toEqual(['A']);
        });
    });

    describe('customFactory parameter', () => {
        it('routes multi field to collection, rendering the custom widget per row (not for the raw multi value)', () => {
            const prop: HalFormsProperty = {
                name: 'categories',
                type: 'CategoryRequest',
                multi: true,
            };
            const customFactory = vi.fn(() => <div data-testid="custom-row" />);
            const element = halFormsFieldsFactory('CategoryRequest', makeProps(prop), customFactory);
            render(
                <Formik initialValues={{categories: ['x']}} onSubmit={vi.fn()}>
                    <Form>{element}</Form>
                </Formik>
            );
            // multi routes to HalFormsCollectionField; the custom factory is probed with a
            // single-value conf (multi/multiple forced false) to detect a per-row widget, then
            // called again by the collection for each row — never with the raw multi conf.
            expect(customFactory).toHaveBeenCalledWith('CategoryRequest', expect.objectContaining({
                prop: expect.objectContaining({multi: false, multiple: false}),
            }));
            expect(screen.getByText(/přidat/i)).toBeInTheDocument();
        });

        it('lets a custom single field win over the built-in default', () => {
            const prop: HalFormsProperty = {
                name: 'category',
                type: 'CategoryRequest',
            };
            const customFactory = vi.fn(() => <div data-testid="custom-field" />);
            const element = halFormsFieldsFactory('CategoryRequest', makeProps(prop), customFactory);
            render(<>{element}</>);
            expect(customFactory).toHaveBeenCalledWith('CategoryRequest', expect.objectContaining({prop}));
            expect(screen.getByTestId('custom-field')).toBeInTheDocument();
        });

        it('falls through to the built-in switch when customFactory returns null', () => {
            const prop: HalFormsProperty = {
                name: 'testField',
                type: 'text',
            };
            const customFactory = vi.fn(() => null);
            const element = halFormsFieldsFactory('text', makeProps(prop), customFactory);
            renderInFormik(element);
            expect(customFactory).toHaveBeenCalled();
            expect(screen.getByRole('textbox')).toBeInTheDocument();
        });

        it('still renders a collection of a built-in element type when customFactory is provided', () => {
            const prop: HalFormsProperty = {
                name: 'tags',
                type: 'text',
                multi: true,
            };
            const customFactory = vi.fn(() => null);
            const element = halFormsFieldsFactory('text', makeProps(prop), customFactory);
            render(
                <Formik initialValues={{tags: ['x']}} onSubmit={vi.fn()}>
                    <Form>{element}</Form>
                </Formik>
            );
            expect(screen.getByRole('textbox')).toBeInTheDocument();
        });
    });

    // design.md D7: the three ways a `multi` property can resolve, driven purely by whether a
    // custom widget exists for the field type and whether options are present — never by field
    // name or type string matching.
    describe('base factory: the three multi branches', () => {
        it('branch 1 - custom widget exists for the type -> HalFormsCollectionField renders it per row', () => {
            const prop: HalFormsProperty = {name: 'items', type: 'Custom', multi: true};
            const customFactory = vi.fn(() => <div data-testid="custom-row" />);
            const element = halFormsFieldsFactory('Custom', makeProps(prop), customFactory);
            render(
                <Formik initialValues={{items: ['x']}} onSubmit={vi.fn()}>
                    <Form>{element}</Form>
                </Formik>
            );
            expect(screen.getAllByTestId('custom-row')).toHaveLength(1);
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });

        it('branch 2 - no custom widget, options present -> generic multi-select', () => {
            const prop: HalFormsProperty = {
                name: 'items',
                type: 'text',
                multi: true,
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop), () => null);
            renderInFormik(element, '');
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });

        it('branch 3 - no custom widget, no options -> HalFormsCollectionField of the basic type', () => {
            const prop: HalFormsProperty = {name: 'tags', type: 'text', multi: true};
            const element = halFormsFieldsFactory('text', makeProps(prop), () => null);
            render(
                <Formik initialValues={{tags: ['x']}} onSubmit={vi.fn()}>
                    <Form>{element}</Form>
                </Formik>
            );
            expect(screen.getByRole('textbox')).toBeInTheDocument();
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });
    });

    describe('UUID field type without a MemberId hint (D7)', () => {
        it('does not route to a member-picker-shaped custom widget just because the type string is "UUID"', () => {
            const prop: HalFormsProperty = {name: 'someId', type: 'UUID'};
            // Simulates klabisCustomFieldFactory after D7: only "MemberId" gets a widget.
            const customFactory = vi.fn((fieldType: string) => fieldType === 'MemberId' ? <div data-testid="member-picker" /> : null);
            const element = halFormsFieldsFactory('UUID', makeProps(prop), customFactory);
            expect(element).toBeNull();
            expect(customFactory).toHaveBeenCalledWith('UUID', expect.anything());
        });
    });
});
