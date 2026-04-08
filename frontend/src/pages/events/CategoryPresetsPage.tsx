import {type ReactElement, useState} from "react";
import type {EntityModel, HalFormsTemplate} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {labels} from "../../localization";
import {Badge, Button, Modal} from "../../components/UI";
import {Pencil, Trash2} from "lucide-react";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";

type CategoryPresetListItem = EntityModel<{
    id: string;
    name: string;
    categories?: string[];
}> & {
    _templates?: Record<string, HalFormsTemplate>;
};

interface ActionModalState {
    item: CategoryPresetListItem;
    templateName: string;
    template: HalFormsTemplate;
}

export const CategoryPresetsPage = (): ReactElement => {
    const {route} = useHalPageData();
    const [actionModal, setActionModal] = useState<ActionModalState | null>(null);

    const openActionModal = (item: CategoryPresetListItem, templateName: string) => {
        const template = item._templates?.[templateName];
        if (!template) return;
        setActionModal({item, templateName, template});
    };

    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const preset = item as unknown as CategoryPresetListItem;
        const hasEditTemplate = !!preset._templates?.updateCategoryPreset;
        const hasDeleteTemplate = !!preset._templates?.deleteCategoryPreset;

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {hasEditTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.buttons.edit}
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(preset, 'updateCategoryPreset');
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
                {hasDeleteTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.deleteCategoryPreset}
                        onClick={(e) => {
                            e.stopPropagation();
                            openActionModal(preset, 'deleteCategoryPreset');
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
            <h1 className="text-3xl font-bold text-text-primary">{labels.nav['category-presets']}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.presetsListHeading}</h2>
                    <HalFormButton name="createCategoryPreset" modal={true} label={labels.templates.createCategoryPreset}/>
                </div>
                <HalEmbeddedTable<CategoryPresetListItem>
                    collectionName={"categoryPresetDtoList"}
                    defaultOrderBy={"name"}
                >
                    <TableCell sortable column={"name"}>{labels.fields.name}</TableCell>
                    <TableCell column={"categories"}
                               dataRender={({value}) => {
                                   const cats = value as string[] | undefined;
                                   if (!cats || cats.length === 0) return null;
                                   return (
                                       <div className="flex flex-wrap gap-1">
                                           {cats.map((cat) => (
                                               <Badge key={cat} variant="info" size="sm">{cat}</Badge>
                                           ))}
                                       </div>
                                   );
                               }}>{labels.fields.categories}</TableCell>
                    <TableCell column={"_actions"} dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
                </HalEmbeddedTable>
            </div>

            {actionModal && (
                <Modal
                    isOpen={true}
                    onClose={() => setActionModal(null)}
                    title={actionModal.template.title ?? labels.templates[actionModal.templateName as keyof typeof labels.templates]}
                    size="md"
                >
                    <HalFormDisplay
                        template={actionModal.template}
                        templateName={actionModal.templateName}
                        resourceData={actionModal.item as unknown as Record<string, unknown>}
                        pathname={route.pathname}
                        onClose={() => setActionModal(null)}
                    />
                </Modal>
            )}
        </div>
    );
};
