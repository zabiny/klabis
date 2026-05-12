import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
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

    describe('other fields', () => {
        it('delegates non-event-type, non-categories fields to klabisFieldsFactory', () => {
            const result = eventFormFieldsFactory('text', createConf('name'));
            render(<div>{result}</div>);
            expect(screen.getByTestId('hal-input-name')).toBeInTheDocument();
        });
    });
});
