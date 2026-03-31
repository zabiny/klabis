import type {ReactElement} from 'react'
import {useField} from 'formik'
import {useRef} from 'react'
import {Button} from '../../../UI'
import {type HalFormsInputProps, SIMPLE_FIELD_TYPES, type SubElementConfiguration} from '../types.ts'
import {labels} from '../../../../localization/labels.ts'

function emptyValueForType(type: string): unknown {
    return SIMPLE_FIELD_TYPES.has(type) ? '' : {}
}

function isCompositeType(type: string): boolean {
    return !SIMPLE_FIELD_TYPES.has(type)
}

let nextId = 0
function newKey(): number {
    return nextId++
}

export const HalFormsCollectionField = ({
    prop,
    renderMode = 'field',
    fieldFactory,
}: HalFormsInputProps): ReactElement => {
    const [field, , helpers] = useField<unknown[]>(prop.name)
    const items: unknown[] = Array.isArray(field.value) ? field.value : []

    // Stable keys that survive mid-list removal — avoids index-as-key reconciliation bugs
    const keysRef = useRef<number[]>([])
    while (keysRef.current.length < items.length) {
        keysRef.current.push(newKey())
    }

    const atMax = prop.max !== undefined && items.length >= prop.max
    const atMin = prop.min !== undefined && items.length <= prop.min
    const composite = isCompositeType(prop.type)

    const handleAdd = () => {
        keysRef.current.push(newKey())
        helpers.setValue([...items, emptyValueForType(prop.type)])
    }

    const handleRemove = (index: number) => {
        keysRef.current.splice(index, 1)
        helpers.setValue(items.filter((_, i) => i !== index))
    }

    const renderItem = (index: number): ReactElement | null => {
        if (!fieldFactory) return null

        const indexedProp = {
            ...prop,
            name: `${prop.name}.${index}`,
            prompt: undefined,
            multiple: false,
        }

        // Rebuild subElementProps so composite sub-fields resolve relative to the indexed item
        const indexedSubElementProps = (attrName: string, conf?: SubElementConfiguration): HalFormsInputProps => {
            const subProp = {
                ...indexedProp,
                name: `${indexedProp.name}.${attrName}`,
                prompt: conf?.prompt || attrName,
                type: conf?.type || 'text',
                options: undefined,
                multiple: false,
                value: undefined,
            }
            return {
                prop: subProp,
                renderMode,
                subElementProps: indexedSubElementProps,
                fieldFactory,
            }
        }

        const indexedInputProps: HalFormsInputProps = {
            prop: indexedProp,
            renderMode,
            subElementProps: indexedSubElementProps,
            fieldFactory,
        }

        return fieldFactory(prop.type, indexedInputProps)
    }

    return (
        <div className="space-y-2">
            {renderMode === 'field' && prop.prompt && (
                <span className="block text-sm font-medium text-text-primary">{prop.prompt}</span>
            )}

            {items.map((_, index) => (
                <div
                    key={keysRef.current[index]}
                    data-testid="collection-item"
                    className={composite ? 'border border-border rounded-md p-3 space-y-2' : 'flex items-start gap-2'}
                >
                    <div className="flex-1">
                        {renderItem(index)}
                    </div>
                    {!prop.readOnly && (
                        <div className={composite ? 'flex justify-end mt-1' : ''}>
                            <Button
                                type="button"
                                variant="danger-ghost"
                                size="sm"
                                disabled={atMin}
                                onClick={() => handleRemove(index)}
                            >
                                {labels.buttons.removeItem}
                            </Button>
                        </div>
                    )}
                </div>
            ))}

            {!prop.readOnly && (
                <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    disabled={atMax}
                    onClick={handleAdd}
                >
                    {labels.buttons.addItem}
                </Button>
            )}
        </div>
    )
}

HalFormsCollectionField.displayName = 'HalFormsCollectionField'
