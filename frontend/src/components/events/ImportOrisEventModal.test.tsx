import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {ImportOrisEventModal, type ImportOrisEventModalProps} from './ImportOrisEventModal';

const orisEvents = [
    {id: 101, name: 'Jarní sprint', date: '2025-04-10', location: 'Brno', organizer: 'ZBM'},
    {id: 202, name: 'Letní závod', date: '2025-07-20', location: null, organizer: 'HBT'},
];

const defaultProps: ImportOrisEventModalProps = {
    isOpen: true,
    onClose: vi.fn(),
    events: orisEvents,
    fetchState: 'success',
    selectedRegion: 'JIHOMORAVSKA',
    onRegionChange: vi.fn(),
    isSubmitting: false,
    submitError: null,
    onImport: vi.fn(),
};

const renderModal = (props: Partial<ImportOrisEventModalProps> = {}) =>
    render(
        <MemoryRouter>
            <ImportOrisEventModal {...defaultProps} {...props} />
        </MemoryRouter>,
    );

describe('ImportOrisEventModal', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('visibility', () => {
        it('does not render when isOpen is false', () => {
            const {container} = renderModal({isOpen: false});
            expect(container.firstChild).toBeNull();
        });

        it('renders modal title when open', () => {
            renderModal();
            expect(screen.getByText('Import akce z ORIS')).toBeInTheDocument();
        });
    });

    describe('loading state', () => {
        it('shows loading indicator when fetchState is loading', () => {
            renderModal({fetchState: 'loading', events: []});
            expect(screen.getByText(/načítání/i)).toBeInTheDocument();
        });

        it('submit button is disabled while loading', () => {
            renderModal({fetchState: 'loading', events: []});
            expect(screen.getByRole('button', {name: /importovat/i})).toBeDisabled();
        });
    });

    describe('fetch error state', () => {
        it('shows error message when fetchState is error', () => {
            renderModal({fetchState: 'error', events: []});
            expect(screen.getByText('Nepodařilo se načíst akce z ORIS.')).toBeInTheDocument();
        });

        it('submit button is disabled when fetch error', () => {
            renderModal({fetchState: 'error', events: []});
            expect(screen.getByRole('button', {name: /importovat/i})).toBeDisabled();
        });
    });

    describe('event list', () => {
        it('renders select with ORIS events formatted as "date — name"', () => {
            renderModal();
            expect(screen.getByRole('combobox')).toBeInTheDocument();
            expect(screen.getByText('2025-04-10 ZBM Jarní sprint — Brno')).toBeInTheDocument();
            expect(screen.getByText('2025-07-20 HBT Letní závod')).toBeInTheDocument();
        });

        it('has no option pre-selected (placeholder shown)', () => {
            renderModal();
            const select = screen.getByRole('combobox') as HTMLSelectElement;
            expect(select.value).toBe('');
        });

        it('submit button is disabled when no option selected', () => {
            renderModal();
            expect(screen.getByRole('button', {name: /importovat/i})).toBeDisabled();
        });

        it('submit button becomes enabled after selecting an option', async () => {
            const user = userEvent.setup();
            renderModal();

            await user.selectOptions(screen.getByRole('combobox'), '101');

            expect(screen.getByRole('button', {name: /importovat/i})).toBeEnabled();
        });
    });

    describe('empty list handling', () => {
        it('shows empty message when events list is empty', () => {
            renderModal({events: []});
            expect(screen.getByText('Žádné akce k importu.')).toBeInTheDocument();
        });

        it('submit button is disabled when event list is empty', () => {
            renderModal({events: []});
            expect(screen.getByRole('button', {name: /importovat/i})).toBeDisabled();
        });
    });

    describe('submit', () => {
        it('calls onImport with the selected orisId on submit', async () => {
            const user = userEvent.setup();
            const onImport = vi.fn();
            renderModal({onImport});

            await user.selectOptions(screen.getByRole('combobox'), '101');
            await user.click(screen.getByRole('button', {name: /importovat/i}));

            expect(onImport).toHaveBeenCalledWith(101);
        });

        it('does not call onImport when no event selected', async () => {
            const user = userEvent.setup();
            const onImport = vi.fn();
            renderModal({onImport});

            await user.click(screen.getByRole('button', {name: /importovat/i}));

            expect(onImport).not.toHaveBeenCalled();
        });

        it('shows loading state on submit button when isSubmitting is true', () => {
            renderModal({isSubmitting: true});
            expect(screen.getByRole('button', {name: /importuji/i})).toBeDisabled();
        });
    });

    describe('error display', () => {
        it('shows submitError when provided', () => {
            renderModal({submitError: 'Tato akce již byla importována.'});
            expect(screen.getByText('Tato akce již byla importována.')).toBeInTheDocument();
        });

        it('shows generic submit error when provided', () => {
            renderModal({submitError: 'Import akce se nezdařil. Zkuste to prosím znovu.'});
            expect(screen.getByText('Import akce se nezdařil. Zkuste to prosím znovu.')).toBeInTheDocument();
        });

        it('does not show error paragraph when submitError is null', () => {
            renderModal({submitError: null});
            expect(screen.queryByText('Tato akce již byla importována.')).not.toBeInTheDocument();
        });
    });

    describe('region picker', () => {
        it('renders three radio buttons for JIHOMORAVSKA, MORAVA, CR regions', () => {
            renderModal();
            expect(screen.getByRole('radio', {name: 'Jihomoravská'})).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: 'Žebříček Morava'})).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: 'ČR'})).toBeInTheDocument();
        });

        it('JIHOMORAVSKA radio is selected by default', () => {
            renderModal({selectedRegion: 'JIHOMORAVSKA'});
            expect(screen.getByRole('radio', {name: 'Jihomoravská'})).toBeChecked();
        });

        it('calls onRegionChange when a different region is picked', async () => {
            const user = userEvent.setup();
            const onRegionChange = vi.fn();
            renderModal({onRegionChange});

            await user.click(screen.getByRole('radio', {name: 'ČR'}));

            expect(onRegionChange).toHaveBeenCalledWith('CR');
        });

        it('clears selected event id when region changes', async () => {
            const user = userEvent.setup();
            renderModal();

            await user.selectOptions(screen.getByRole('combobox'), '101');
            expect((screen.getByRole('combobox') as HTMLSelectElement).value).toBe('101');

            await user.click(screen.getByRole('radio', {name: 'ČR'}));

            expect((screen.getByRole('combobox') as HTMLSelectElement).value).toBe('');
        });
    });

    describe('cancel/close handling', () => {
        it('calls onClose when Cancel button is clicked', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();
            renderModal({onClose});

            await user.click(screen.getByRole('button', {name: /zrušit/i}));

            expect(onClose).toHaveBeenCalled();
        });
    });
});
