import type {ReactElement} from "react";
import type {ListEventTypesItemResource} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {useTemplateDialog} from "../../components/HalNavigator2/useTemplateDialog.tsx";
import {halResource} from "../../api/halResource";
import {labels} from "../../localization";
import {Button} from "../../components/UI";
import {Pencil, Trash2} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";

type EventTypeListItem = ListEventTypesItemResource;

export const EventTypesPage = (): ReactElement => {
    const actionDialog = useTemplateDialog({
        size: 'md',
        title: (template, name) => template.title ?? labels.templates[name as keyof typeof labels.templates],
    });

    const renderColorSwatch = ({value}: TableCellRenderProps) => {
        const color = value as string | undefined;
        if (!color) return null;
        return (
            <span
                className="inline-block w-5 h-5 rounded border border-zinc-300"
                style={{backgroundColor: color}}
                title={color}
            />
        );
    };

    const renderActionsCell = ({item: eventType}: TableCellRenderProps<EventTypeListItem>) => {
        const actions = halResource(eventType);
        const editAction = actions.template('updateEventType');
        const deleteAction = actions.template('deleteEventType');

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {editAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.buttons.edit}
                        onClick={(e) => {
                            e.stopPropagation();
                            actionDialog.open(editAction);
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
                {deleteAction && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.deleteEventType}
                        onClick={(e) => {
                            e.stopPropagation();
                            actionDialog.open(deleteAction);
                        }}
                    >
                        <Trash2 className="w-4 h-4"/>
                    </Button>
                )}
            </div>
        );
    };

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.nav['event-types']}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.eventTypesListHeading}</h2>
                    <HalFormButton name="createEventType" modal={true} label={labels.templates.createEventType} navigateOnSuccess={false}/>
                </div>
                <HalEmbeddedTable<EventTypeListItem>
                    tableId="event-types"
                    defaultOrderBy={"sortOrder"}
                >
                    <TableCell column={"color"} dataRender={renderColorSwatch}>{labels.fields.color}</TableCell>
                    <TableCell sortable column={"name"}>{labels.fields.name}</TableCell>
                    <TableCell sortable column={"sortOrder"}>{labels.fields.sortOrder}</TableCell>
                    <TableCell column={"_actions"} dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
                </HalEmbeddedTable>
            </div>

            {actionDialog.dialog}
        </div>
    );
};
