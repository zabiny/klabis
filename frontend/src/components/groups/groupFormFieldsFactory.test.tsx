import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import type {HalFormsTemplate} from '../../api';
import {HalFormsForm} from '../HalNavigator2/halforms';
import {groupFormFieldsFactory} from './groupFormFieldsFactory.tsx';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: undefined, error: null, isLoading: false}),
    useAuthorizedMutation: vi.fn(),
}));

const createGroupTemplate = (inline: string[] = ['MEMBERS:EDIT_PROFILE']): HalFormsTemplate => ({
    method: 'POST',
    properties: [
        {name: 'name', prompt: 'Název', type: 'text', required: true},
        {
            name: 'delegatedAuthorities',
            type: 'Authority',
            multi: true,
            required: false,
            options: {inline},
        },
    ],
});

const renderForm = (template: HalFormsTemplate, onSubmit = vi.fn().mockResolvedValue(undefined)) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    render(
        <QueryClientProvider client={queryClient}>
            <HalFormsForm data={{}} template={template} onSubmit={onSubmit} fieldsFactory={groupFormFieldsFactory}/>
        </QueryClientProvider>
    );
    return onSubmit;
};

describe('groupFormFieldsFactory', () => {
    it('offers delegated permissions as an unchecked checkbox labelled "Úprava údajů člena"', () => {
        renderForm(createGroupTemplate());

        const checkbox = screen.getByRole('checkbox', {name: 'Úprava údajů člena'});
        expect(checkbox).not.toBeChecked();
        expect(screen.getByText('Oprávnění vlastníků nad členy')).toBeInTheDocument();
    });

    it('does not render a combobox for the delegated permissions', () => {
        renderForm(createGroupTemplate());

        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    });

    it('submits an empty delegatedAuthorities list when the checkbox is left unchecked', async () => {
        const user = userEvent.setup();
        const onSubmit = renderForm(createGroupTemplate());

        await user.type(screen.getByRole('textbox'), 'Závoďáci');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalled());
        expect(onSubmit.mock.calls[0][0]).toMatchObject({name: 'Závoďáci', delegatedAuthorities: []});
    });

    it('submits MEMBERS:EDIT_PROFILE when the checkbox is checked', async () => {
        const user = userEvent.setup();
        const onSubmit = renderForm(createGroupTemplate());

        await user.type(screen.getByRole('textbox'), 'Závoďáci');
        await user.click(screen.getByRole('checkbox', {name: 'Úprava údajů člena'}));
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalled());
        expect(onSubmit.mock.calls[0][0]).toMatchObject({delegatedAuthorities: ['MEMBERS:EDIT_PROFILE']});
    });

    it('renders no delegation field when the backend offers no options', () => {
        renderForm(createGroupTemplate([]));

        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    });
});
