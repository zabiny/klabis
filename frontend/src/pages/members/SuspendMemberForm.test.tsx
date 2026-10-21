import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import type {HalFormsTemplate} from '../../api';
import {HalFormsForm} from '../../components/HalNavigator2/halforms/HalFormsForm.tsx';
import {klabisFieldsFactory} from '../../components/KlabisFieldsFactory';
import {getFieldLabel} from '../../localization';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(() => ({data: undefined, isLoading: false, error: null})),
    useAuthorizedMutation: vi.fn(),
}));

const prop = (p: Record<string, unknown>) => p as unknown as HalFormsTemplate['properties'][number];

const suspendTemplate: HalFormsTemplate = {
    method: 'POST',
    target: '/api/members/1/suspend',
    title: 'Ukončit členství',
    properties: [
        prop({name: 'reason', type: 'DeactivationReason', required: true, options: {inline: ['ODHLASKA', 'PRESTUP', 'OTHER']}}),
        prop({name: 'note', type: 'text'}),
    ],
};

const renderForm = () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<HalFormsForm data={{}} template={suspendTemplate} onSubmit={onSubmit} fieldsFactory={klabisFieldsFactory}/>);
    return onSubmit;
};

describe('suspendMember form', () => {
    it('does not submit without a reason', async () => {
        const onSubmit = renderForm();

        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('submits once a reason is chosen, note stays optional', async () => {
        const onSubmit = renderForm();

        await userEvent.selectOptions(screen.getByRole('combobox'), 'PRESTUP');
        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({reason: 'PRESTUP'})));
    });

    it('marks the reason field as required', () => {
        renderForm();

        expect(screen.getByText('*')).toBeInTheDocument();
    });

    describe('field labels used when the server sends no prompt', () => {
        it.fails('does not label the mandatory reason as optional', () => {
            expect(getFieldLabel('reason')).not.toMatch(/volitelné/i);
        });

        it.fails('has a localized label for note instead of the raw property name', () => {
            expect(getFieldLabel('note')).not.toBe('note');
        });
    });
});
