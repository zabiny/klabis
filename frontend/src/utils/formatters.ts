import {labels} from '../localization';

export const renderYesNo = (value: unknown): string => (value ? labels.ui.yes : labels.ui.no);
