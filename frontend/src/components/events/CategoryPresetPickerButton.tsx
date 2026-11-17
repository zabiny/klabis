import {type ReactElement, useState} from 'react';
import {useFormikContext} from 'formik';
import {useHalCollection} from '../../hooks/useHalCollection.ts';
import {useHalRoute} from '../../contexts/halRouteContext.ts';
import {toHref} from '../../api/hateoas.ts';
import {Button, Modal} from '../UI';
import {BookTemplate} from 'lucide-react';
import {labels} from '../../localization';

interface CategoryPreset {
    name?: string;
    categories?: string[];
}

export const CategoryPresetPickerButton = (): ReactElement | null => {
    const {setFieldValue} = useFormikContext<Record<string, unknown>>();
    const [isOpen, setIsOpen] = useState(false);

    const {resourceData} = useHalRoute();
    const presetsLink = resourceData?._links?.categoryPresets;

    const {items: presets} = useHalCollection<CategoryPreset>(presetsLink ? toHref(presetsLink) : undefined, {
        staleTime: 5 * 60 * 1000,
    });

    if (presets.length === 0) {
        return null;
    }

    const handleSelect = (preset: CategoryPreset) => {
        const categories = (preset.categories ?? []).map(name => ({name}));
        setFieldValue('categories', categories);
        setIsOpen(false);
    };

    return (
        <>
            <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => setIsOpen(true)}
                startIcon={<BookTemplate className="w-4 h-4"/>}
            >
                {labels.buttons.selectFromTemplates}
            </Button>

            <Modal
                isOpen={isOpen}
                onClose={() => setIsOpen(false)}
                title={labels.dialogTitles.selectCategoryPreset}
                size="sm"
            >
                <ul className="flex flex-col gap-1 py-2">
                    {presets.map((preset, index) => (
                        <li key={index}>
                            <button
                                type="button"
                                onClick={() => handleSelect(preset)}
                                className="w-full text-left px-3 py-2 rounded hover:bg-surface-hover text-text-primary"
                            >
                                {preset.name}
                            </button>
                        </li>
                    ))}
                </ul>
            </Modal>
        </>
    );
};
