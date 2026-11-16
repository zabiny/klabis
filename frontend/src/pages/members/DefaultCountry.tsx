import {useEffect} from 'react';
import {getIn, useFormikContext} from 'formik';

const DEFAULT_COUNTRY = 'CZ';

// Seeds once on mount: HalFormsForm builds initial values from the template and cannot nest-merge the
// composite address, and a later clear by the user must not be overwritten.
export const DefaultCountry = ({field}: {field: string}): null => {
    const {values, setFieldValue} = useFormikContext<Record<string, unknown>>();

    useEffect(() => {
        if (!getIn(values, field)) {
            void setFieldValue(field, DEFAULT_COUNTRY);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};
