import {type ReactElement, type ReactNode, useEffect} from 'react';
import {useFormikContext} from 'formik';
import {DetailRow} from '../../components/UI';

export const isCzNationality = (nationality: unknown): boolean =>
    nationality === 'CZ' || nationality === 'CZE';

interface BirthNumberConditionalFieldProps {
    renderInput: (name: string) => ReactNode;
}

export const BirthNumberConditionalField = ({renderInput}: BirthNumberConditionalFieldProps): ReactElement | null => {
    const {values, setFieldValue} = useFormikContext<Record<string, unknown>>();
    const isCz = isCzNationality(values.nationality);

    useEffect(() => {
        if (!isCz && values.birthNumber) {
            setFieldValue('birthNumber', '');
        }
    }, [isCz, setFieldValue, values.birthNumber]);

    if (!isCz) return null;

    return (
        <DetailRow label="Rodné číslo">
            {renderInput('birthNumber')}
        </DetailRow>
    );
};
