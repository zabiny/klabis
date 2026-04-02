import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type {HalFormsProperty, HalFormsTemplate} from '../../../api';
import {HalFormsForm} from './HalFormsForm.tsx';
import {vi} from 'vitest';
import type {HalFormFieldFactory, HalFormsInputProps} from './types.ts';

const createProperty = (overrides: Partial<HalFormsProperty> = {}): HalFormsProperty => ({
    name: 'testField',
    prompt: 'Test Label',
    type: 'text',
    required: false,
    readOnly: false,
    value: undefined,
    ...overrides,
});

const createTemplate = (properties: HalFormsProperty[]): HalFormsTemplate => ({
    method: 'PUT',
    properties,
});

const renderForm = (
    template: HalFormsTemplate,
    data: Record<string, unknown> = {},
    fieldsFactory?: HalFormFieldFactory
) => {
    return render(
        <HalFormsForm
            data={data}
            template={template}
            onSubmit={vi.fn()}
            fieldsFactory={fieldsFactory}
        />
    );
};

describe('HalFormsForm readOnly field rendering', () => {
    it('renders readOnly property as plain text, not as an input', () => {
        const prop = createProperty({readOnly: true, prompt: 'Registration Number'});
        const template = createTemplate([prop]);

        renderForm(template, {testField: '12345'});

        expect(screen.getByText('12345')).toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('uses property prompt as label for readOnly field', () => {
        const prop = createProperty({readOnly: true, prompt: 'My Label'});
        const template = createTemplate([prop]);

        renderForm(template, {testField: 'some value'});

        expect(screen.getByText('My Label')).toBeInTheDocument();
        expect(screen.getByText('some value')).toBeInTheDocument();
    });

    it('renders non-readOnly property through fieldFactory as normal input', () => {
        const prop = createProperty({readOnly: false, prompt: 'Editable Field'});
        const template = createTemplate([prop]);

        renderForm(template, {testField: 'edit me'});

        expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('renders em dash for readOnly property with null value', () => {
        const prop = createProperty({readOnly: true, prompt: 'Empty Field'});
        const template = createTemplate([prop]);

        renderForm(template, {testField: null});

        expect(screen.getByText('\u2014')).toBeInTheDocument();
    });

    it('renders em dash for readOnly property with undefined value', () => {
        const prop = createProperty({readOnly: true, prompt: 'Missing Field'});
        const template = createTemplate([prop]);

        renderForm(template, {});

        expect(screen.getByText('\u2014')).toBeInTheDocument();
    });

    it('does not intercept composite (unknown) type fields even if readOnly', () => {
        const prop = createProperty({readOnly: true, type: 'compositeAddress', prompt: 'Address'});
        const template = createTemplate([prop]);

        const mockFactory: HalFormFieldFactory = vi.fn((_type: string, conf: HalFormsInputProps) => {
            return <div data-testid="custom-composite">{conf.prop.prompt}</div>;
        });

        renderForm(template, {testField: {street: 'Main St'}}, mockFactory);

        expect(mockFactory).toHaveBeenCalled();
    });
});

describe('createValidationSchema — required field validation', () => {
    it('blocks submit when required text field is empty', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'firstName', type: 'text', required: true, prompt: 'Jméno'});
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('blocks submit when required email field is empty', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'email', type: 'email', required: true, prompt: 'Email'});
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('allows submit when required text field is filled', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'firstName', type: 'text', required: true, prompt: 'Jméno'});
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{firstName: 'Jan'}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).toHaveBeenCalled();
    });

    it('does not require optional text field', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'nickname', type: 'text', required: false});
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).toHaveBeenCalled();
    });
});

describe('HalFormsForm — multi: true (backend shorthand for multiple)', () => {
    it('initializes field with multi:true as empty array when no data provided', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'memberIds', type: 'UUID', multi: true} as any);
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({memberIds: []})
        );
    });

    it('initializes field with multi:true as existing array when data provided', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'memberIds', type: 'UUID', multi: true} as any);
        const template = createTemplate([prop]);
        const existingIds = ['id-1', 'id-2'];

        render(<HalFormsForm data={{memberIds: existingIds}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({memberIds: existingIds})
        );
    });
});

describe('HalFormsForm onSubmit value sanitization', () => {
    it('submits null instead of empty string for unfilled text field', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'bankAccountNumber', type: 'text'});
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({bankAccountNumber: null})
        );
    });

    it('submits null instead of empty string for object-type field', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'guardian', type: 'GuardianDTO'});
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({guardian: null})
        );
    });

    it('preserves non-empty string values on submit', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);
        const prop = createProperty({name: 'firstName', type: 'text'});
        const template = createTemplate([prop]);

        render(<HalFormsForm data={{firstName: 'Jan'}} template={template} onSubmit={onSubmit} />);

        await userEvent.click(screen.getByRole('button', {name: /odeslat/i}));

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({firstName: 'Jan'})
        );
    });
});
