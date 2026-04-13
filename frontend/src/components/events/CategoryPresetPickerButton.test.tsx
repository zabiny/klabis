import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Form, Formik, useFormikContext} from 'formik';
import {vi} from 'vitest';
import {CategoryPresetPickerButton} from './CategoryPresetPickerButton';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

vi.mock('../UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, onClose, title}: {isOpen: boolean; children: React.ReactNode; onClose: () => void; title: string}) =>
        isOpen ? (
            <div role="dialog" aria-label={title}>
                <h4>{title}</h4>
                {children}
                <button onClick={onClose}>Zavřít</button>
            </div>
        ) : null,
}));

import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const PRESETS = [
    {name: 'Šablona A', categories: ['H21', 'D21']},
    {name: 'Šablona B', categories: ['H35', 'D35', 'H45']},
];

interface FormValues {
    categories: string[];
}

const CaptureValues = ({onValues}: {onValues: (values: FormValues) => void}) => {
    const {values} = useFormikContext<FormValues>();
    onValues(values);
    return null;
};

const renderButton = (onValues?: (values: FormValues) => void) => {
    return render(
        <Formik initialValues={{categories: [] as string[]}} onSubmit={vi.fn()}>
            <Form>
                <CategoryPresetPickerButton/>
                {onValues && <CaptureValues onValues={onValues}/>}
            </Form>
        </Formik>,
    );
};

describe('CategoryPresetPickerButton', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('button visibility', () => {
        it('shows "Vybrat ze šablon" button when presets exist', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {_embedded: {categoryPresetDtoList: PRESETS}},
                isLoading: false,
                error: null,
            } as any);

            renderButton();

            expect(screen.getByRole('button', {name: /vybrat ze šablon/i})).toBeInTheDocument();
        });

        it('does not show button when no presets exist (empty list)', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {_embedded: {categoryPresetDtoList: []}},
                isLoading: false,
                error: null,
            } as any);

            renderButton();

            expect(screen.queryByRole('button', {name: /vybrat ze šablon/i})).not.toBeInTheDocument();
        });

        it('does not show button when data is undefined', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
                error: null,
            } as any);

            renderButton();

            expect(screen.queryByRole('button', {name: /vybrat ze šablon/i})).not.toBeInTheDocument();
        });

        it('does not show button when _embedded is absent', () => {
            mockUseAuthorizedQuery.mockReturnValue({
                data: {},
                isLoading: false,
                error: null,
            } as any);

            renderButton();

            expect(screen.queryByRole('button', {name: /vybrat ze šablon/i})).not.toBeInTheDocument();
        });
    });

    describe('preset picker dialog', () => {
        it('opens dialog showing preset names when button is clicked', async () => {
            const user = userEvent.setup();
            mockUseAuthorizedQuery.mockReturnValue({
                data: {_embedded: {categoryPresetDtoList: PRESETS}},
                isLoading: false,
                error: null,
            } as any);

            renderButton();

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText('Šablona A')).toBeInTheDocument();
            expect(screen.getByText('Šablona B')).toBeInTheDocument();
        });

        it('closes dialog after preset is selected', async () => {
            const user = userEvent.setup();
            mockUseAuthorizedQuery.mockReturnValue({
                data: {_embedded: {categoryPresetDtoList: PRESETS}},
                isLoading: false,
                error: null,
            } as any);

            renderButton();

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));
            expect(screen.getByRole('dialog')).toBeInTheDocument();

            await user.click(screen.getByText('Šablona A'));

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('populates categories field with selected preset categories', async () => {
            const user = userEvent.setup();
            mockUseAuthorizedQuery.mockReturnValue({
                data: {_embedded: {categoryPresetDtoList: PRESETS}},
                isLoading: false,
                error: null,
            } as any);

            let capturedValues: FormValues = {categories: []};
            renderButton((values) => {
                capturedValues = values;
            });

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));
            await user.click(screen.getByText('Šablona A'));

            expect(capturedValues.categories).toEqual(['H21', 'D21']);
        });

        it('populates categories from second preset when second is selected', async () => {
            const user = userEvent.setup();
            mockUseAuthorizedQuery.mockReturnValue({
                data: {_embedded: {categoryPresetDtoList: PRESETS}},
                isLoading: false,
                error: null,
            } as any);

            let capturedValues: FormValues = {categories: []};
            renderButton((values) => {
                capturedValues = values;
            });

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));
            await user.click(screen.getByText('Šablona B'));

            expect(capturedValues.categories).toEqual(['H35', 'D35', 'H45']);
        });
    });
});
