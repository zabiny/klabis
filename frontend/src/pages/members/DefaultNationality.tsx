import {useEffect} from 'react';
import {useFormikContext} from 'formik';

const DEFAULT_NATIONALITY = 'CZ';

export const DefaultNationality = (): null => {
    const {values, setFieldValue} = useFormikContext<Record<string, unknown>>();
    const isEmpty = !values.nationality;

    useEffect(() => {
        if (isEmpty) {
            void setFieldValue('nationality', DEFAULT_NATIONALITY);
        }
    }, [isEmpty, setFieldValue]);

    return null;
};
