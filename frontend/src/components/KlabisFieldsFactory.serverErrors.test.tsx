import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import type {HalFormsTemplate} from '../api';
import {ApiError} from '../api/authorizedFetch.ts';
import {HalFormsForm} from './HalNavigator2/halforms/HalFormsForm.tsx';
import {klabisFieldsFactory} from './KlabisFieldsFactory';

vi.mock('../hooks/useEventTypes', () => ({
    useEventTypes: () => ({eventTypes: [], isLoading: false, getById: vi.fn()}),
}));

vi.mock('../hooks/useMembershipFeeTierOptions', () => ({
    useMembershipFeeTierOptions: () => [],
}));

const template: HalFormsTemplate = {
    method: 'POST',
    properties: [
        {name: 'nationality', type: 'Country', prompt: 'Státní příslušnost', required: false, readOnly: false},
        {name: 'address', type: 'AddressRequest', prompt: 'Adresa', required: false, readOnly: false},
    ],
};

const renderWithError = (serverError: ApiError) => {
    const form = (error: ApiError | null) => (
        <QueryClientProvider client={new QueryClient()}>
            <HalFormsForm data={{nationality: 'CZ', address: {street: 'A', city: 'B', postalCode: '1', country: 'CZ'}}}
                          template={template} onSubmit={vi.fn()} fieldsFactory={klabisFieldsFactory}
                          serverError={error}/>
        </QueryClientProvider>
    );
    const view = render(form(null));
    view.rerender(form(serverError));
};

describe('country picker server field errors', () => {
    it('shows an invalid address country error on the address country picker', async () => {
        renderWithError(new ApiError(400, {type: '/errors/invalid-address-country', field: 'address.country'}));

        expect(await screen.findByText('Stát v adrese není platný kód státu.')).toBeInTheDocument();
    });

    it('shows an invalid nationality error on the nationality picker', async () => {
        renderWithError(new ApiError(400, {type: '/errors/invalid-nationality', field: 'nationality'}));

        expect(await screen.findByText('Státní příslušnost není platný kód státu.')).toBeInTheDocument();
    });
});
