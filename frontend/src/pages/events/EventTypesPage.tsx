import type {ReactElement} from "react";
import type {HalFormsTemplate} from "../../api";
import type {components} from "../../api/klabisApi";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {useTemplateDialog} from "../../components/HalNavigator2/useTemplateDialog.tsx";
import {labels} from "../../localization";
import {Button} from "../../components/UI";
import {Pencil, Trash2} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";

type EventTypeListItem = components['schemas']['EntityModelEventTypeDto'] & {
    _templates?: Record<string, HalFormsTemplate>;
};

export const EventTypesPage = (): ReactElement => {
    const actionDialog = useTemplateDialog(undefined, {
        size: 'md',
        title: (template, name) => template.title ?? labels.templates[name as keyof typeof labels.templates],
    });

    const openActionModal = (item: EventTypeListItem, templateName: string) => {
        actionDialog.open(item, templateName);
    };

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

    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const eventType = item as unknown as EventTypeListItem;
        const hasEditTemplate = !!eventType._templates?.updateEventType;
        const hasDeleteTemplate = !!eventType._templates?.deleteEventType;

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {hasEditTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.buttons.edit}
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(eventType, 'updateEventType');
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
                {hasDeleteTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.deleteEventType}
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(eventType, 'deleteEventType');
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
                    collectionName={"eventTypeDtoList"}
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
