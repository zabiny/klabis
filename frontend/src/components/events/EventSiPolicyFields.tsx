import type {ReactElement, ReactNode} from 'react';
import {useFormikContext} from 'formik';
import {DetailRow} from '../UI';
import {labels} from '../../localization';

interface EventSiPolicyFieldsProps {
    renderInput: (name: string) => ReactNode;
    hasField: (name: string) => boolean;
}

// Rental makes no sense when the event does not use SI cards at all.
export const EventSiPolicyFields = ({renderInput, hasField}: EventSiPolicyFieldsProps): ReactElement => {
    const {values} = useFormikContext<{ siUsage?: string }>();
    const showRental = hasField('siRentalAllowed') && values.siUsage !== 'NOT_USED';

    return (
        <>
            {hasField('siUsage') && (
                <DetailRow label={labels.fields.siUsage}>{renderInput('siUsage')}</DetailRow>
            )}
            {showRental && (
                <DetailRow label={labels.fields.siRentalAllowed}>{renderInput('siRentalAllowed')}</DetailRow>
            )}
        </>
    );
};
