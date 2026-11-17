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

vi.mock('../../contexts/halRouteContext', () => ({
    useHalRoute: vi.fn(),
}));

import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {useHalRoute} from '../../contexts/halRouteContext';

const mockUseHalRoute = vi.mocked(useHalRoute);
const mockUseAuthorizedQuery = vi.mocked(useAuthorizedQuery);

const mockQueryData = (envelope: unknown) => {
    mockUseAuthorizedQuery.mockReturnValue({
        data: envelope,
        isLoading: false,
        error: null,
    } as unknown as ReturnType<typeof useAuthorizedQuery>);
};

const PRESETS = [
    {name: 'Šablona A', categories: ['H21', 'D21']},
    {name: 'Šablona B', categories: ['H35', 'D35', 'H45']},
];

interface FormValues {
    categories: {name: string}[];
}

const CaptureValues = ({onValues}: {onValues: (values: FormValues) => void}) => {
    const {values} = useFormikContext<FormValues>();
    onValues(values);
    return null;
};

const renderButton = (onValues?: (values: FormValues) => void) => {
    return render(
        <Formik initialValues={{categories: [] as {name: string}[]}} onSubmit={vi.fn()}>
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
        mockUseHalRoute.mockReturnValue({
            resourceData: {_links: {categoryPresets: {href: '/api/category-presets'}}},
        } as unknown as ReturnType<typeof useHalRoute>);
    });

    describe('categoryPresets link', () => {
        it('does not request presets and shows no button when page resource lacks categoryPresets link', () => {
            mockUseHalRoute.mockReturnValue({
                resourceData: {_links: {self: {href: '/api/events'}}},
            } as unknown as ReturnType<typeof useHalRoute>);
            mockQueryData(undefined);

            renderButton();

            expect(mockUseAuthorizedQuery).toHaveBeenCalledWith(undefined, expect.anything());
            expect(screen.queryByRole('button', {name: /vybrat ze šablon/i})).not.toBeInTheDocument();
        });

        it('loads presets from the href of the categoryPresets link', () => {
            mockUseHalRoute.mockReturnValue({
                resourceData: {_links: {categoryPresets: {href: '/api/custom-presets'}}},
            } as unknown as ReturnType<typeof useHalRoute>);
            mockQueryData({_embedded: {categoryPresetDtoList: PRESETS}});

            renderButton();

            expect(mockUseAuthorizedQuery).toHaveBeenCalledWith('/api/custom-presets', expect.anything());
            expect(screen.getByRole('button', {name: /vybrat ze šablon/i})).toBeInTheDocument();
        });
    });

    describe('button visibility', () => {
        it('shows "Vybrat ze šablon" button when presets exist', () => {
            mockQueryData({_embedded: {categoryPresetDtoList: PRESETS}});

            renderButton();

            expect(screen.getByRole('button', {name: /vybrat ze šablon/i})).toBeInTheDocument();
        });

        it('does not show button when no presets exist (empty list)', () => {
            mockQueryData({_embedded: {categoryPresetDtoList: []}});

            renderButton();

            expect(screen.queryByRole('button', {name: /vybrat ze šablon/i})).not.toBeInTheDocument();
        });

        it('does not show button when data is undefined', () => {
            mockQueryData(undefined);

            renderButton();

            expect(screen.queryByRole('button', {name: /vybrat ze šablon/i})).not.toBeInTheDocument();
        });

        it('does not show button when _embedded is absent', () => {
            mockQueryData({});

            renderButton();

            expect(screen.queryByRole('button', {name: /vybrat ze šablon/i})).not.toBeInTheDocument();
        });
    });

    describe('preset picker dialog', () => {
        it('opens dialog showing preset names when button is clicked', async () => {
            const user = userEvent.setup();
            mockQueryData({_embedded: {categoryPresetDtoList: PRESETS}});

            renderButton();

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));

            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText('Šablona A')).toBeInTheDocument();
            expect(screen.getByText('Šablona B')).toBeInTheDocument();
        });

        it('closes dialog after preset is selected', async () => {
            const user = userEvent.setup();
            mockQueryData({_embedded: {categoryPresetDtoList: PRESETS}});

            renderButton();

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));
            expect(screen.getByRole('dialog')).toBeInTheDocument();

            await user.click(screen.getByText('Šablona A'));

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('populates categories field with selected preset categories', async () => {
            const user = userEvent.setup();
            mockQueryData({_embedded: {categoryPresetDtoList: PRESETS}});

            let capturedValues: FormValues = {categories: []};
            renderButton((values) => {
                capturedValues = values;
            });

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));
            await user.click(screen.getByText('Šablona A'));

            expect(capturedValues.categories).toEqual([{name: 'H21'}, {name: 'D21'}]);
        });

        it('populates categories without id when preset is selected (new categories)', async () => {
            const user = userEvent.setup();
            mockQueryData({_embedded: {categoryPresetDtoList: PRESETS}});

            let capturedValues: FormValues = {categories: []};
            renderButton((values) => {
                capturedValues = values;
            });

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));
            await user.click(screen.getByText('Šablona A'));

            capturedValues.categories.forEach(category => {
                expect(category).not.toHaveProperty('id');
            });
        });

        it('populates categories from second preset when second is selected', async () => {
            const user = userEvent.setup();
            mockQueryData({_embedded: {categoryPresetDtoList: PRESETS}});

            let capturedValues: FormValues = {categories: []};
            renderButton((values) => {
                capturedValues = values;
            });

            await user.click(screen.getByRole('button', {name: /vybrat ze šablon/i}));
            await user.click(screen.getByText('Šablona B'));

            expect(capturedValues.categories).toEqual([{name: 'H35'}, {name: 'D35'}, {name: 'H45'}]);
        });
    });
});
