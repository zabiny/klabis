import {TableCell} from '../KlabisTable';
import {formatDateTimeSeconds} from '../../utils/dateUtils';
import {labels} from '../../localization';
import {enumLabelOrEmpty} from './syncAttemptFormat';

export const enumCell = (enumType: string) =>
    ({value}: {value: unknown}) => enumLabelOrEmpty(enumType, value);

const textCell = ({value}: {value: unknown}) => typeof value === 'string' ? value : '';

const columnLabels = labels.syncOverview.columns;

export const attemptColumns = {
    startedAt: (
        <TableCell key="startedAt" column="startedAt"
                   dataRender={({value}) => typeof value === 'string' ? formatDateTimeSeconds(value) : ''}>
            {columnLabels.startedAt}
        </TableCell>
    ),
    trigger: (
        <TableCell key="trigger" column="trigger" dataRender={enumCell('syncTrigger')}>
            {columnLabels.trigger}
        </TableCell>
    ),
    direction: (
        <TableCell key="direction" column="direction" dataRender={enumCell('syncDirection')}>
            {columnLabels.direction}
        </TableCell>
    ),
    outcome: (
        <TableCell key="outcome" column="outcome" dataRender={enumCell('syncOutcome')}>
            {columnLabels.outcome}
        </TableCell>
    ),
    failureReason: (
        <TableCell key="failureReason" column="failureReason" dataRender={textCell}>{columnLabels.failureReason}</TableCell>
    ),
    performedBy: (
        <TableCell key="performedBy" column="performedBy" dataRender={textCell}>{columnLabels.performedBy}</TableCell>
    ),
};
