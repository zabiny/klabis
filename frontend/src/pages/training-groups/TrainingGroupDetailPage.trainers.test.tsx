import '@testing-library/jest-dom';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {TrainingGroupDetailPage} from './TrainingGroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {stubFetch} from '../../__mocks__/halDialogTest';
import {
    confirmTemplate,
    memberOptionsResponse,
    memberPickerTemplate,
    renderDetailPage,
} from '../../__mocks__/halGroupPageTest';

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalRouteProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../contexts/halRouteContext.ts', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Petr', lastName: 'Trenér', registrationNumber: 'ZBM1000', _links: {self: {href: '/api/members/trainer-1'}}},
        navigateToResource: vi.fn(),
        isLoading: false,
        error: null,
    })),
}));

const renderPage = (resourceData: HalResponse) =>
    renderDetailPage(<TrainingGroupDetailPage/>, resourceData, {pathname: '/training-groups/tg-1'});

const buildTrainingGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'tg-1',
    name: 'Tréninková skupina U10',
    ageRange: {minAge: 8, maxAge: 10},
    trainers: [],
    members: [],
    _links: {self: {href: '/api/training-groups/tg-1'}},
    ...overrides,
});

const buildTrainer = (overrides?: Record<string, unknown>) => ({
    memberId: 'member-1',
    _links: {member: {href: '/api/members/member-1'}},
    ...overrides,
});

const buildTrainee = (overrides?: Record<string, unknown>) => ({
    memberId: 'member-2',
    joinedAt: '2025-01-15T00:00:00Z',
    _links: {
        self: {href: '/api/training-groups/tg-1/members/member-2'},
        member: {href: '/api/members/member-2'},
    },
    ...overrides,
});

const trainerWithRemoveTemplate = () => buildTrainer({
    _links: {
        member: {href: '/api/members/member-1'},
        self: {href: '/api/training-groups/tg-1/trainers/member-1'},
    },
    _templates: {removeTrainer: confirmTemplate({title: 'Odebrat trenéra'})},
});

describe('TrainingGroupDetailPage — trainer management', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch(memberOptionsResponse));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows trainers section label', () => {
        renderPage(buildTrainingGroupDetail({trainers: [buildTrainer()]}));
        expect(screen.getByText(/trenéři/i)).toBeInTheDocument();
    });

    it('shows "Přidat trenéra" button when addTrainer template exists on resource', () => {
        renderPage(buildTrainingGroupDetail({
            trainers: [buildTrainer()],
            _templates: {addTrainer: memberPickerTemplate({title: 'Přidat trenéra'})},
        }));
        expect(screen.getByRole('button', {name: /přidat trenéra/i})).toBeInTheDocument();
    });

    it('does NOT show "Přidat trenéra" button when addTrainer template is absent', () => {
        renderPage(buildTrainingGroupDetail({trainers: [buildTrainer()]}));
        expect(screen.queryByRole('button', {name: /přidat trenéra/i})).not.toBeInTheDocument();
    });

    it('"Přidat trenéra" dialog hides current trainers and submits to the template target', async () => {
        const user = userEvent.setup();
        renderPage(buildTrainingGroupDetail({
            trainers: [buildTrainer()],
            members: [buildTrainee()],
            _templates: {addTrainer: memberPickerTemplate({title: 'Přidat trenéra', target: '/api/training-groups/tg-1/trainers'})},
        }));
        await user.click(screen.getByRole('button', {name: /přidat trenéra/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Přidat trenéra');
        expect(await screen.findByRole('option', {name: 'Karel Dvořák'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Jana Nováková'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Petr Svoboda'})).not.toBeInTheDocument();

        await user.selectOptions(screen.getByRole('combobox', {name: /člen/i}), 'member-3');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/training-groups/tg-1/trainers');
        await waitFor(() => expect(screen.getByTestId('toast')).toHaveTextContent('Úspěšně uloženo'));
    });

    it('shows "Odebrat trenéra" button per trainer when removeTrainer template and self link exist on trainer', () => {
        renderPage(buildTrainingGroupDetail({trainers: [trainerWithRemoveTemplate()]}));
        expect(screen.getByRole('button', {name: /odebrat trenéra/i})).toBeInTheDocument();
    });

    it('does NOT show "Odebrat trenéra" button when removeTrainer template is absent on trainer', () => {
        renderPage(buildTrainingGroupDetail({trainers: [buildTrainer()]}));
        expect(screen.queryByRole('button', {name: /odebrat trenéra/i})).not.toBeInTheDocument();
    });

    it('does NOT show "Odebrat trenéra" button when the trainer has no self link', () => {
        renderPage(buildTrainingGroupDetail({
            trainers: [buildTrainer({_templates: {removeTrainer: confirmTemplate({title: 'Odebrat trenéra'})}})],
        }));
        expect(screen.queryByRole('button', {name: /odebrat trenéra/i})).not.toBeInTheDocument();
    });

    it('clicking "Odebrat trenéra" opens confirmation dialog and submits to the trainer self link', async () => {
        const user = userEvent.setup();
        renderPage(buildTrainingGroupDetail({trainers: [trainerWithRemoveTemplate()]}));
        await user.click(screen.getByRole('button', {name: /odebrat trenéra/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Odebrat trenéra');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/training-groups/tg-1/trainers/member-1');
        expect((writeCalls()[0][1] as RequestInit).method).toBe('DELETE');
    });

    it('shows single edit button when updateTrainingGroup template exists', () => {
        renderPage(buildTrainingGroupDetail({
            _templates: {updateTrainingGroup: mockHalFormsTemplate({title: 'Upravit skupinu', method: 'PATCH'})},
        }));
        expect(screen.getByRole('button', {name: /upravit skupinu/i})).toBeInTheDocument();
    });

    it('does NOT show separate "Změnit věkové rozmezí" button (merged into single edit)', () => {
        renderPage(buildTrainingGroupDetail({
            _templates: {updateTrainingGroup: mockHalFormsTemplate({title: 'Upravit skupinu', method: 'PATCH'})},
        }));
        expect(screen.queryByRole('button', {name: /věkové rozmezí/i})).not.toBeInTheDocument();
    });
});

describe('TrainingGroupDetailPage — trainees and deletion', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch(memberOptionsResponse));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('"Přidat člena" dialog hides current trainees and trainers', async () => {
        const user = userEvent.setup();
        renderPage(buildTrainingGroupDetail({
            trainers: [buildTrainer()],
            members: [buildTrainee()],
            _templates: {addTrainingGroupMember: memberPickerTemplate({title: 'Přidat člena'})},
        }));
        await user.click(screen.getByRole('button', {name: /přidat člena/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Přidat člena');
        expect(await screen.findByRole('option', {name: 'Karel Dvořák'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Petr Svoboda'})).not.toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Jana Nováková'})).not.toBeInTheDocument();
    });

    it('removing a trainee uses the template title and submits to the member self link', async () => {
        const user = userEvent.setup();
        renderPage(buildTrainingGroupDetail({
            members: [buildTrainee({_templates: {removeTrainingGroupMember: confirmTemplate({title: 'Odebrat člena'})}})],
        }));
        await user.click(screen.getByRole('button', {name: /odebrat/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Odebrat člena');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/training-groups/tg-1/members/member-2');
    });

    it('deleting the group opens the template title dialog and navigates to the list', async () => {
        const user = userEvent.setup();
        renderPage(buildTrainingGroupDetail({
            _templates: {deleteTrainingGroup: confirmTemplate({title: 'Smazat skupinu', target: '/api/training-groups/tg-1'})},
        }));
        await user.click(screen.getByRole('button', {name: /smazat tréninkovou skupinu/i}));

        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Smazat skupinu');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/training-groups/tg-1');
        await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(/^\/training-groups$/));
    });
});
