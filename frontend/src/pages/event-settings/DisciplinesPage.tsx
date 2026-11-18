import type {ReactElement} from 'react';
import {Archive, ArchiveRestore, Pencil} from 'lucide-react';
import type {components} from '../../api/klabisApi';
import {halResource} from '../../api/halResource';
import {TableCell} from '../../components/KlabisTable';
import type {TableCellRenderProps} from '../../components/KlabisTable/types.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {useTemplateDialog} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {Button} from '../../components/UI';
import {labels} from '../../localization';

type DisciplineListItem = components['schemas']['EntityModelDisciplineDto'];

export const DisciplinesPage = (): ReactElement => {
    const actionDialog = useTemplateDialog({
        size: 'md',
        title: (template, name) => template.title ?? labels.templates[name as keyof typeof labels.templates],
    });

    const renderStatusCell = ({item: discipline}: TableCellRenderProps<DisciplineListItem>) => (
        <span className={discipline.archived ? 'text-text-secondary' : undefined}>
            {discipline.archived ? labels.ui.disciplineArchived : labels.ui.disciplineActive}
        </span>
    );

    const renderActionsCell = ({item: discipline}: TableCellRenderProps<DisciplineListItem>) => {
        const actions = halResource(discipline);
        const editAction = actions.template('updateDiscipline');
        const archiveAction = actions.template('archiveDiscipline');
        const restoreAction = actions.template('restoreDiscipline');

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {editAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.updateDiscipline}
                        onClick={(e) => {
                            e.stopPropagation();
                            actionDialog.open(editAction);
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
                {archiveAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.archiveDiscipline}
                        onClick={(e) => {
                            e.stopPropagation();
                            actionDialog.open(archiveAction);
                        }}
                    >
                        <Archive className="w-4 h-4"/>
                    </Button>
                )}
                {restoreAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.restoreDiscipline}
                        onClick={(e) => {
                            e.stopPropagation();
                            actionDialog.open(restoreAction);
                        }}
                    >
                        <ArchiveRestore className="w-4 h-4"/>
                    </Button>
                )}
            </div>
        );
    };

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.nav.disciplines}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.disciplinesListHeading}</h2>
                    <HalFormButton name="createDiscipline" modal={true} label={labels.templates.createDiscipline}
                                   navigateOnSuccess={false}/>
                </div>
                <HalEmbeddedTable<DisciplineListItem> tableId="disciplines" defaultOrderBy="name">
                    <TableCell sortable column="code">{labels.fields.code}</TableCell>
                    <TableCell sortable column="name">{labels.fields.name}</TableCell>
                    <TableCell column="archived" dataRender={renderStatusCell}>{labels.tables.status}</TableCell>
                    <TableCell column="_actions" dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
                </HalEmbeddedTable>
            </div>

            {actionDialog.dialog}
        </div>
    );
};
