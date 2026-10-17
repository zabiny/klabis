import {getEnumLabel} from '../../localization';

export const enumLabelOrEmpty = (enumType: string, value: unknown): string | null =>
    typeof value === 'string' ? getEnumLabel(enumType, value) : null;
