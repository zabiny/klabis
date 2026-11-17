import {useState} from 'react';
import type {ReactElement} from 'react';
import {Button} from '../UI/Button';
import {labels} from '../../localization';
import {halResource} from '../../api/halResource';
import type {HalResponse} from '../../api';

interface ImportOrisEventsButtonProps {
    resource: HalResponse;
}

export function ImportOrisEventsButton({resource}: ImportOrisEventsButtonProps): ReactElement | null {
    const [, setIsOpen] = useState(false);

    const actions = halResource(resource);
    const importAction = actions.template('importEventsBatch') ?? actions.template('importEvent');

    if (!importAction) {
        return null;
    }

    return (
        <Button variant="secondary" onClick={() => setIsOpen(true)}>
            {labels.templates.importEvent}
        </Button>
    );
}
