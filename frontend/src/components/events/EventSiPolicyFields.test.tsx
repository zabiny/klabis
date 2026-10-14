import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Formik} from 'formik';
import {EventSiPolicyFields} from './EventSiPolicyFields';

const renderInput = (name: string) => <input data-testid={`input-${name}`}/>;

const renderFields = (siUsage: string | undefined, hasField: (name: string) => boolean = () => true) =>
    render(
        <Formik initialValues={{siUsage}} onSubmit={() => undefined}>
            <dl>
                <EventSiPolicyFields renderInput={renderInput} hasField={hasField}/>
            </dl>
        </Formik>
    );

describe('EventSiPolicyFields', () => {
    it('shows SI usage and rental tick box when usage is not chosen yet', () => {
        renderFields(undefined);

        expect(screen.getByText('Použití SI čipu')).toBeInTheDocument();
        expect(screen.getByTestId('input-siUsage')).toBeInTheDocument();
        expect(screen.getByText('Půjčení čipu povoleno')).toBeInTheDocument();
        expect(screen.getByTestId('input-siRentalAllowed')).toBeInTheDocument();
    });

    it.each(['OPTIONAL', 'REQUIRED'])('shows the rental tick box when usage is %s', (usage) => {
        renderFields(usage);

        expect(screen.getByTestId('input-siRentalAllowed')).toBeInTheDocument();
    });

    it('hides the rental tick box when SI is not used', () => {
        renderFields('NOT_USED');

        expect(screen.getByTestId('input-siUsage')).toBeInTheDocument();
        expect(screen.queryByTestId('input-siRentalAllowed')).not.toBeInTheDocument();
    });

    it('renders only fields present in the template', () => {
        renderFields('REQUIRED', (name) => name === 'siUsage');

        expect(screen.getByTestId('input-siUsage')).toBeInTheDocument();
        expect(screen.queryByTestId('input-siRentalAllowed')).not.toBeInTheDocument();
    });
});
