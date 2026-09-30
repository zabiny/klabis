import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {HalFormsTemplate} from '../../api';
import {HalFormsForm} from '../../components/HalNavigator2/halforms/HalFormsForm.tsx';
import {klabisFieldsFactory} from '../../components/KlabisFieldsFactory';
import {RegistrationContactSection, RegistrationGuardianSections} from './RegistrationGuardianSections.tsx';

const guardianProfiles = vi.hoisted(() => ({current: undefined as Record<string, unknown> | undefined}));
const requestedUrls = vi.hoisted(() => [] as string[]);
const candidates = vi.hoisted(() => [
    {value: 'user-1', prompt: 'Jana Nováková'},
    {value: 'user-2', prompt: 'Petr Svoboda'},
]);
vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn((url: string, options?: { enabled?: boolean; select?: (data: unknown) => unknown }) => {
        if (options?.enabled === false) return {data: undefined, isLoading: false, error: null};
        if (url.startsWith('/legal-guardians/')) {
            requestedUrls.push(url);
            return {data: guardianProfiles.current, isLoading: false, error: null};
        }
        return {data: options?.select ? options.select(candidates) : candidates, isLoading: false, error: null};
    }),
    useAuthorizedMutation: vi.fn(),
}));
vi.mock('../../hooks/useEventTypes', () => ({useEventTypes: () => ({eventTypes: [], isLoading: false})}));
vi.mock('../../hooks/useMembershipFeeTierOptions', () => ({useMembershipFeeTierOptions: () => []}));

const prop = (p: Record<string, unknown>) => p as unknown as HalFormsTemplate['properties'][number];

const template: HalFormsTemplate = {
    method: 'POST',
    target: '/api/members',
    title: 'Registrovat člena',
    properties: [
        prop({name: 'firstName', prompt: 'Jméno', type: 'text'}),
        prop({name: 'lastName', prompt: 'Příjmení', type: 'text'}),
        prop({name: 'dateOfBirth', prompt: 'Datum narození', type: 'date'}),
        prop({name: 'email', prompt: 'E-mail', type: 'email'}),
        prop({name: 'phone', prompt: 'Telefon', type: 'tel'}),
        prop({
            name: 'legalGuardians', prompt: 'Zástupci', type: 'LegalGuardianInputRequest', multi: true,
            options: {link: {href: '/api/legal-guardian-options'}},
        }),
        prop({
            name: 'legalGuardianUserId', prompt: 'Zástupce', type: 'UserId',
            options: {link: {href: '/api/legal-guardian-options?kind=LEGAL_GUARDIAN'}},
        }),
    ],
};

const input = (name: string) => document.querySelector(`input[name="${name}"]`) as HTMLInputElement;

const renderForm = (onSubmit = vi.fn().mockResolvedValue(undefined)) => {
    render(
        <HalFormsForm
            data={{}}
            template={template}
            onSubmit={onSubmit}
            fieldsFactory={klabisFieldsFactory}
            renderForm={({renderInput, renderField}) => {
                const hasField = (n: string) => template.properties.some(p => p.name === n);
                return (
                    <div>
                        {renderInput('firstName')}
                        {renderInput('lastName')}
                        {renderInput('dateOfBirth')}
                        <RegistrationContactSection renderInput={renderInput} hasField={hasField}/>
                        <RegistrationGuardianSections renderInput={renderInput} hasField={hasField}/>
                        {renderField('submit')}
                    </div>
                );
            }}
        />,
    );
    return onSubmit;
};

const setBirthDate = async (value: string) => {
    await userEvent.type(input('dateOfBirth'), value);
};

describe('member registration form sections', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        guardianProfiles.current = undefined;
        requestedUrls.length = 0;
    });

    it('shows no guardian sections until date of birth is filled', () => {
        renderForm();
        expect(screen.queryByText('ZÁKONNÍ ZÁSTUPCI')).not.toBeInTheDocument();
        expect(screen.queryByText('PŘEVZETÍ ZÁKONNÉHO ZÁSTUPCE')).not.toBeInTheDocument();
    });

    it('registers a minor with a new guardian and optional own contacts', async () => {
        const onSubmit = renderForm();
        await userEvent.type(input('firstName'), 'Karel');
        await userEvent.type(input('lastName'), 'Malý');
        await setBirthDate('2015-05-05');

        expect(screen.getByText('ZÁKONNÍ ZÁSTUPCI')).toBeInTheDocument();
        expect(screen.queryByText('PŘEVZETÍ ZÁKONNÉHO ZÁSTUPCE')).not.toBeInTheDocument();
        expect(screen.queryByText('E-mail *')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Přidat'}));
        await userEvent.click(screen.getByRole('button', {name: 'Založit nového zástupce'}));
        await userEvent.type(input('legalGuardians.0.firstName'), 'Eva');
        await userEvent.type(input('legalGuardians.0.lastName'), 'Malá');
        await userEvent.type(input('legalGuardians.0.email'), 'eva@example.com');
        await userEvent.type(input('legalGuardians.0.phone'), '+420777111222');
        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
            firstName: 'Karel',
            dateOfBirth: '2015-05-05',
            legalGuardians: [{firstName: 'Eva', lastName: 'Malá', email: 'eva@example.com', phone: '+420777111222'}],
        })));
    });

    it('registers an adult taking over a guardian, prefilling data from the guardian profile', async () => {
        guardianProfiles.current = {
            userId: 'user-1', firstName: 'Jana', lastName: 'Nováková',
            email: 'jana@example.com', phone: '+420601000000', loginName: 'EXT0001',
        };
        const onSubmit = renderForm();
        await setBirthDate('1985-01-01');

        expect(screen.getByText('PŘEVZETÍ ZÁKONNÉHO ZÁSTUPCE')).toBeInTheDocument();
        expect(screen.queryByText('ZÁKONNÍ ZÁSTUPCI')).not.toBeInTheDocument();
        expect(screen.getByText('E-mail *')).toBeInTheDocument();

        await userEvent.selectOptions(screen.getByRole('combobox'), 'user-1');

        await waitFor(() => expect(input('firstName')).toHaveValue('Jana'));
        expect(requestedUrls).toContain('/legal-guardians/user-1');
        expect(input('lastName')).toHaveValue('Nováková');
        expect(input('email')).toHaveValue('jana@example.com');
        expect(input('phone')).toHaveValue('+420601000000');

        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));
        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
            legalGuardianUserId: 'user-1',
            email: 'jana@example.com',
        })));
    });

    it('drops the takeover selection when the birth date turns the member into a minor', async () => {
        guardianProfiles.current = {firstName: 'Jana', lastName: 'N', email: 'j@e.cz', phone: '+420601000000'};
        const onSubmit = renderForm();
        await setBirthDate('1985-01-01');
        await userEvent.selectOptions(screen.getByRole('combobox'), 'user-1');
        await waitFor(() => expect(input('firstName')).toHaveValue('Jana'));

        await userEvent.clear(input('dateOfBirth'));
        await setBirthDate('2015-05-05');
        await screen.findByText('ZÁKONNÍ ZÁSTUPCI');
        await userEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => expect(onSubmit).toHaveBeenCalled());
        expect(onSubmit.mock.calls[0][0].legalGuardianUserId).toBeFalsy();
    });
});
