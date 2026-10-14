import {useId} from 'react'

export const useFieldId = (explicitId?: string): string => {
    const generatedId = useId()
    return explicitId ?? generatedId
}
