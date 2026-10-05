import type {ReactElement} from 'react';
import type {HalFormFieldFactory, HalFormsInputProps} from '../HalNavigator2/halforms';
import {HalFormsCheckboxGroup} from '../HalNavigator2/halforms/fields';
import {isMultipleProperty} from '../HalNavigator2/halforms/utils';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';

/**
 * Delegated permissions are an opt-in choice on a short, always-visible list (today a single
 * item), so they render as checkboxes instead of the generic searchable multi-select.
 */
export const groupFormFieldsFactory: HalFormFieldFactory = (
    fieldType: string,
    conf: HalFormsInputProps
): ReactElement | null => {
    if (fieldType === 'Authority' && isMultipleProperty(conf.prop) && conf.prop.options) {
        if (conf.prop.options.inline?.length === 0) return null;
        return <HalFormsCheckboxGroup {...conf}/>;
    }
    return klabisFieldsFactory(fieldType, conf);
};
