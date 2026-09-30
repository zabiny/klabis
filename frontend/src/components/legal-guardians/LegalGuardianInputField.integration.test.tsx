import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import type {HalFormsTemplate} from '../../api';
import {HalFormsForm} from '../HalNavigator2/halforms/HalFormsForm.tsx';
import {klabisFieldsFactory} from '../KlabisFieldsFactory';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn((_url: string, options?: {select?: (data: unknown) => unknown}) => {
        const data = [
            {value: 'user-1', prompt: 'Jana Nováková'},
            {value: 'user-2', prompt: 'Petr Svoboda'},
        ];
        return {data: options?.select ? options.select(data) : data, isLoading: false, error: null};
    }),
    useAuthorizedMutation: vi.fn(),
}));

vi.mock('../../hooks/useEventTypes', () => ({useEventTypes: () => ({eventTypes: [], isLoading: false})}));
vi.mock('../../hooks/useMembershipFeeTierOptions', () => ({useMembershipFeeTierOptions: () => []}));

// Mirrors what the backend sends for setMemberLegalGuardians: array of objects, options bound to the array property.
const template: HalFormsTemplate = {
    method: 'PUT',
    target: '/api/members/m-1/legal-guardians',
    title: 'Upravit zástupce',
    properties: [{
        name: 'legalGuardians',
        prompt: 'Zákonní zástupci',
        type: 'LegalGuardianInputRequest',
        required: true,
        multi: true,
        options: {link: {href: '/api/legal-guardian-options'}},
    } as HalFormsTemplate['properties'][number]],
};

const input = (name: string) => document.querySelector(`input[name="${name}"]`) as HTMLInputElement;

const renderForm = (data: Record<string, unknown>, onSubmit = vi.fn().mockResolvedValue(undefined)) => {
    render(<HalFormsForm data={data} template={template} onSubmit={onSubmit} fieldsFactory={klabisFieldsFactory}/>);
    return onSubmit;
};

describe('legalGuardians collection in HAL-FORMS (integration)', () => {
    it('prefills existing guardians as pickers and submits them as {userId}', async () => {
        const onSubmit = renderForm({legalGuardians: [{userId: 'user-1'}]});

        expect(screen.getAllByTestId('collection-item')).toHaveLength(1);
        expect(screen.getByRole('combobox')).toHaveValue('user-1');

        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({legalGuardians: [{userId: 'user-1'}]}));
    });

    it('adds an existing candidate by picking from options', async () => {
        const onSubmit = renderForm({legalGuardians: []});

        await userEvent.click(screen.getByRole('button', {name: 'Přidat'}));
        await userEvent.selectOptions(screen.getByRole('combobox'), 'user-2');
        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({legalGuardians: [{userId: 'user-2'}]}));
    });

    it('creates a new guardian from name, e-mail and phone', async () => {
        const onSubmit = renderForm({legalGuardians: []});

        await userEvent.click(screen.getByRole('button', {name: 'Přidat'}));
        await userEvent.click(screen.getByRole('button', {name: 'Založit nového zástupce'}));
        await userEvent.type(input('legalGuardians.0.firstName'), 'Eva');
        await userEvent.type(input('legalGuardians.0.lastName'), 'Nová');
        await userEvent.type(input('legalGuardians.0.email'), 'eva@example.com');
        await userEvent.type(input('legalGuardians.0.phone'), '+420777111222');
        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
            legalGuardians: [{firstName: 'Eva', lastName: 'Nová', email: 'eva@example.com', phone: '+420777111222'}],
        }));
    });

    it('mixes existing and new guardians and removes a row', async () => {
        const onSubmit = renderForm({legalGuardians: [{userId: 'user-1'}, {userId: 'user-2'}]});

        expect(screen.getAllByTestId('collection-item')).toHaveLength(2);
        await userEvent.click(screen.getAllByRole('button', {name: 'Odebrat'})[1]);
        await userEvent.click(screen.getByRole('button', {name: 'Přidat'}));
        await userEvent.click(screen.getAllByRole('button', {name: 'Založit nového zástupce'})[1]);
        await userEvent.type(input('legalGuardians.1.firstName'), 'Eva');
        await userEvent.type(input('legalGuardians.1.lastName'), 'Nová');
        await userEvent.type(input('legalGuardians.1.email'), 'eva@example.com');
        await userEvent.type(input('legalGuardians.1.phone'), '+420777111222');
        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
            legalGuardians: [
                {userId: 'user-1'},
                {firstName: 'Eva', lastName: 'Nová', email: 'eva@example.com', phone: '+420777111222'},
            ],
        }));
    });

    it('drops a stale userId when the selection is cleared', async () => {
        const onSubmit = renderForm({legalGuardians: [{userId: 'user-1'}]});

        await userEvent.click(screen.getByTestId('clear-member-button'));
        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({legalGuardians: [{}]}));
    });
});
