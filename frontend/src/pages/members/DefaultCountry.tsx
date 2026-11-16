import {useEffect} from 'react';
import {getIn, useFormikContext} from 'formik';

const DEFAULT_COUNTRY = 'CZ';

export const DefaultCountry = ({field}: {field: string}): null => {
    const {values, setFieldValue} = useFormikContext<Record<string, unknown>>();
    const isEmpty = !getIn(values, field);

    useEffect(() => {
        if (isEmpty) {
            void setFieldValue(field, DEFAULT_COUNTRY);
        }
    }, [isEmpty, field, setFieldValue]);

    return null;
};
