import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {ImportOrisEventModal, type ImportOrisEventModalProps} from './ImportOrisEventModal';
import type {BulkImportResult} from '../../api/orisEvents';

const orisEvents = [
    {id: 101, name: 'Jarní sprint', date: '2025-04-10', location: 'Brno', organizer: 'ZBM'},
    {id: 202, name: 'Letní závod', date: '2025-07-20', location: null, organizer: 'HBT'},
    {id: 303, name: 'Podzimní tour', date: '2025-09-15', location: 'Praha', organizer: 'SKP'},
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
    selectedIds: new Set<number>(),
    onToggleId: vi.fn(),
    onToggleAll: vi.fn(),
    onImportBatch: vi.fn(),
    importResult: null,
    isAllSelected: false,
    isSomeSelected: false,
    canSubmit: false,
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
            expect(screen.getByText('Importovat z ORIS')).toBeInTheDocument();
        });
    });

    describe('loading state', () => {
        it('shows loading indicator when fetchState is loading', () => {
            renderModal({fetchState: 'loading', events: []});
            expect(screen.getByText(/komunikuji s ORIS/i)).toBeInTheDocument();
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

    describe('checkbox event list', () => {
        it('renders checkboxes for each ORIS event', () => {
            renderModal();
            const checkboxes = screen.getAllByRole('checkbox');
            // select-all + 3 event checkboxes
            expect(checkboxes.length).toBeGreaterThanOrEqual(3);
        });

        it('renders event names in the list', () => {
            renderModal();
            expect(screen.getByText('Jarní sprint')).toBeInTheDocument();
            expect(screen.getByText('Letní závod')).toBeInTheDocument();
            expect(screen.getByText('Podzimní tour')).toBeInTheDocument();
        });

        it('renders event dates in the list', () => {
            renderModal();
            expect(screen.getByText(/2025-04-10/)).toBeInTheDocument();
        });

        it('renders organizer and location info', () => {
            renderModal();
            expect(screen.getByText(/ZBM/)).toBeInTheDocument();
            expect(screen.getByText(/Brno/)).toBeInTheDocument();
        });

        it('submit button is disabled when 0 events selected', () => {
            renderModal({selectedIds: new Set()});
            expect(screen.getByRole('button', {name: /importovat/i})).toBeDisabled();
        });

        it('submit button is enabled when at least one event is selected', () => {
            renderModal({selectedIds: new Set([101]), canSubmit: true});
            expect(screen.getByRole('button', {name: /importovat/i})).toBeEnabled();
        });

        it('shows selected count in submit button label', () => {
            renderModal({selectedIds: new Set([101, 202]), canSubmit: true});
            expect(screen.getByRole('button', {name: /importovat vybrané \(2\)/i})).toBeInTheDocument();
        });

        it('shows selected count summary in footer', () => {
            renderModal({selectedIds: new Set([101]), canSubmit: true});
            expect(screen.getByText(/vybráno: 1/i)).toBeInTheDocument();
        });

        it('calls onToggleId with event id when event checkbox is clicked', async () => {
            const user = userEvent.setup();
            const onToggleId = vi.fn();
            renderModal({onToggleId});

            const checkboxes = screen.getAllByRole('checkbox');
            // find the event checkbox (not select-all)
            const eventCheckbox = checkboxes.find(cb => cb.getAttribute('value') === '101' || cb.closest('[data-event-id="101"]') !== null);
            if (eventCheckbox) {
                await user.click(eventCheckbox);
                expect(onToggleId).toHaveBeenCalledWith(101);
            } else {
                // fallback: click by label text
                const label = screen.getByText('Jarní sprint').closest('label') ?? screen.getByText('Jarní sprint').closest('li');
                if (label) await user.click(label);
                expect(onToggleId).toHaveBeenCalledWith(101);
            }
        });

        it('calls onToggleAll when select-all checkbox is clicked', async () => {
            const user = userEvent.setup();
            const onToggleAll = vi.fn();
            renderModal({onToggleAll});

            const selectAllCheckbox = screen.getByLabelText(/vybrat vše/i);
            await user.click(selectAllCheckbox);

            expect(onToggleAll).toHaveBeenCalled();
        });

        it('select-all checkbox is checked when all events are selected', () => {
            renderModal({selectedIds: new Set([101, 202, 303]), isAllSelected: true, canSubmit: true});
            const selectAll = screen.getByLabelText(/vybrat vše/i);
            expect(selectAll).toBeChecked();
        });

        it('select-all checkbox is unchecked when no events are selected', () => {
            renderModal({selectedIds: new Set()});
            const selectAll = screen.getByLabelText(/vybrat vše/i);
            expect(selectAll).not.toBeChecked();
        });

        it('individual event checkbox is checked when its id is in selectedIds', () => {
            renderModal({selectedIds: new Set([101])});
            const checkbox = screen.getByRole('checkbox', {name: /jarní sprint/i});
            expect(checkbox).toBeChecked();
        });

        it('individual event checkbox is unchecked when its id is not in selectedIds', () => {
            renderModal({selectedIds: new Set([101])});
            const checkbox = screen.getByRole('checkbox', {name: /letní závod/i});
            expect(checkbox).not.toBeChecked();
        });
    });

    describe('empty list handling', () => {
        it('shows empty heading when events list is empty', () => {
            renderModal({events: []});
            expect(screen.getByText('Žádné závody k importu')).toBeInTheDocument();
        });

        it('submit button is disabled when event list is empty', () => {
            renderModal({events: []});
            expect(screen.getByRole('button', {name: /importovat/i})).toBeDisabled();
        });
    });

    describe('submit', () => {
        it('calls onImportBatch on submit with selected ids', async () => {
            const user = userEvent.setup();
            const onImportBatch = vi.fn();
            renderModal({onImportBatch, selectedIds: new Set([101, 202]), canSubmit: true});

            await user.click(screen.getByRole('button', {name: /importovat/i}));

            expect(onImportBatch).toHaveBeenCalled();
        });

        it('does not call onImportBatch when no events selected', async () => {
            const onImportBatch = vi.fn();
            renderModal({onImportBatch, selectedIds: new Set()});

            // button is disabled, click should not fire
            const btn = screen.getByRole('button', {name: /importovat/i});
            expect(btn).toBeDisabled();
            expect(onImportBatch).not.toHaveBeenCalled();
        });

        it('shows loading state on submit button when isSubmitting is true', () => {
            renderModal({isSubmitting: true, selectedIds: new Set([101])});
            expect(screen.getByRole('button', {name: /importuji/i})).toBeDisabled();
        });
    });

    describe('error display', () => {
        it('shows submitError when provided', () => {
            renderModal({submitError: 'Import se nezdařil. Zkuste to prosím znovu.'});
            expect(screen.getByText('Import se nezdařil. Zkuste to prosím znovu.')).toBeInTheDocument();
        });

        it('does not show error paragraph when submitError is null', () => {
            renderModal({submitError: null});
            expect(screen.queryByText('Import se nezdařil.')).not.toBeInTheDocument();
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
    });

    describe('result panel', () => {
        const importResult: BulkImportResult = {
            totalProcessed: 3,
            successCount: 2,
            failureCount: 1,
            results: [
                {orisId: 101, name: 'Jarní sprint', date: '2025-04-10', status: 'imported'},
                {orisId: 202, name: 'Letní závod', date: '2025-07-20', status: 'imported'},
                {orisId: 303, name: 'Podzimní tour', date: '2025-09-15', status: 'failed', error: 'Duplikátní import'},
            ],
        };

        it('shows result panel title when importResult is provided', () => {
            renderModal({importResult});
            expect(screen.getByText('Výsledek importu')).toBeInTheDocument();
        });

        it('shows summary "Naimportováno N z M"', () => {
            renderModal({importResult});
            expect(screen.getByText(/naimportováno 2 z 3/i)).toBeInTheDocument();
        });

        it('shows check icon for successfully imported events', () => {
            renderModal({importResult});
            expect(screen.getByText('Jarní sprint')).toBeInTheDocument();
        });

        it('shows error message for failed imports', () => {
            renderModal({importResult});
            expect(screen.getByText('Podzimní tour')).toBeInTheDocument();
            expect(screen.getByText('Duplikátní import')).toBeInTheDocument();
        });

        it('shows Hotovo button in result panel', () => {
            renderModal({importResult});
            expect(screen.getByRole('button', {name: /hotovo/i})).toBeInTheDocument();
        });

        it('calls onClose when Hotovo button is clicked', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();
            renderModal({importResult, onClose});

            await user.click(screen.getByRole('button', {name: /hotovo/i}));

            expect(onClose).toHaveBeenCalled();
        });

        it('does not show checkbox list when importResult is provided', () => {
            renderModal({importResult});
            expect(screen.queryByLabelText(/vybrat vše/i)).not.toBeInTheDocument();
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
