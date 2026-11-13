import type {HalFormsProperty} from '../../../../api';
import {getFieldLabel} from '../../../../localization';

export const fieldLabel = (prop: HalFormsProperty): string => prop.prompt || getFieldLabel(prop.name);
