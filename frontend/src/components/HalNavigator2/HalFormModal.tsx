import type {ReactElement, ReactNode} from 'react';
import {Modal} from '../UI';
import {HalFormDisplay, type HalFormDisplayProps} from './HalFormDisplay.tsx';

export interface HalFormModalProps extends HalFormDisplayProps {
    title: string;
    size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl';
    /** Explanatory / warning text rendered in the band under the title. */
    note?: ReactNode;
}

export const HalFormModal = ({title, size = 'md', note, ...formDisplayProps}: HalFormModalProps): ReactElement => (
    <Modal isOpen={true} onClose={formDisplayProps.onClose} title={title} size={size} context={note}>
        <HalFormDisplay {...formDisplayProps} />
    </Modal>
);