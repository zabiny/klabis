import type {ReactElement} from 'react';
import {Modal} from '../UI';
import {HalFormDisplay, type HalFormDisplayProps} from './HalFormDisplay.tsx';

export interface HalFormModalProps extends HalFormDisplayProps {
    title: string;
    size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl';
}

export const HalFormModal = ({title, size = 'md', ...formDisplayProps}: HalFormModalProps): ReactElement => (
    <Modal isOpen={true} onClose={formDisplayProps.onClose} title={title} size={size}>
        <HalFormDisplay {...formDisplayProps} />
    </Modal>
);
