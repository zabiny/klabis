import type {ReactElement} from "react";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {labels} from "../../localization";
import {Badge} from "../../components/UI";

type CategoryPresetListItem = EntityModel<{
    id: string;
    name: string;
    categories?: string[];
}>;

export const CategoryPresetsPage = (): ReactElement => {
    const {route} = useHalPageData();

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
                    onRowClick={route.navigateToResource}
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
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
