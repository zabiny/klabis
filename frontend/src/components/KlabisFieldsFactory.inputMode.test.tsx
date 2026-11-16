import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {klabisFieldsFactory} from './KlabisFieldsFactory';
import type {HalFormsInputProps} from './HalNavigator2/halforms';

vi.mock('../hooks/useEventTypes', () => ({
    useEventTypes: () => ({eventTypes: [], isLoading: false, getById: vi.fn()}),
}));

vi.mock('../hooks/useMembershipFeeTierOptions', () => ({
    useMembershipFeeTierOptions: () => [],
}));

const subElementProps = ((attrName: string, configuration?: {prompt?: string; type?: string}) => ({
    prop: {name: `parent.${attrName}`, prompt: configuration?.prompt, type: configuration?.type ?? 'text'},
    errorText: undefined,
    renderMode: 'input',
    subElementProps: vi.fn(),
})) as unknown as HalFormsInputProps['subElementProps'];

const renderInputMode = (fieldType: string, name: string) => {
    const conf: HalFormsInputProps = {
        prop: {name, type: fieldType},
        errorText: undefined,
        renderMode: 'input',
        subElementProps,
    };
    return render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <Formik initialValues={{}} onSubmit={vi.fn()}>
                <Form>{klabisFieldsFactory(fieldType, conf)}</Form>
            </Formik>
        </QueryClientProvider>
    );
};

describe('KlabisFieldsFactory input render mode naming', () => {
    it('names composite sub-fields by their detail row labels', () => {
        renderInputMode('AddressRequest', 'address');

        for (const label of ['Ulice', 'Město', 'PSČ']) {
            expect(screen.getByLabelText(label).tagName).toBe('INPUT');
            expect(screen.getByText(label).tagName).toBe('DT');
        }
    });

    it('renders the address country as a country picker', () => {
        renderInputMode('AddressRequest', 'address');

        expect(screen.getByRole('combobox', {name: 'Stát'})).toBeInTheDocument();
        expect(screen.getByText('Stát').tagName).toBe('DT');
    });

    it('names license level select and validity field by their detail row labels', () => {
        renderInputMode('TrainerLicenseDto', 'trainerLicense');

        expect(screen.getByRole('combobox', {name: 'Stupeň'})).toBeInTheDocument();
        expect(screen.getByLabelText('Platnost')).toBeInTheDocument();
    });
});

describe('KlabisFieldsFactory Country input type', () => {
    it('renders the country picker for nationality', () => {
        renderInputMode('Country', 'nationality');

        expect(screen.getByRole('combobox', {name: 'Státní příslušnost'})).toBeInTheDocument();
    });
});

describe('KlabisFieldsFactory country code input labelling', () => {
    it('distinguishes the code input of nationality and address country', () => {
        const conf = (name: string): HalFormsInputProps => ({
            prop: {name, type: 'Country'}, errorText: undefined, renderMode: 'input', subElementProps,
        });
        render(
            <QueryClientProvider client={new QueryClient()}>
                <Formik initialValues={{nationality: 'FR', parent: {country: 'IT'}}} onSubmit={vi.fn()}>
                    <Form>
                        {klabisFieldsFactory('Country', conf('nationality'))}
                        {klabisFieldsFactory('AddressRequest', {...conf('address'), prop: {name: 'address', type: 'AddressRequest'}})}
                    </Form>
                </Formik>
            </QueryClientProvider>
        );

        expect(screen.getByRole('textbox', {name: 'Kód státu (Státní příslušnost)'})).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Kód státu (Stát)'})).toBeInTheDocument();
    });
});
