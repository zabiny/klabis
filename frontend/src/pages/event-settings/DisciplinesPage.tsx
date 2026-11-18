import type {ReactElement} from 'react';
import {Spinner} from '../../components/UI';
import {GenericHalPageContent} from '../GenericHalPage.tsx';
import {HalPageGate} from '../HalPageGate.tsx';

export const DisciplinesPage = (): ReactElement => (
    <HalPageGate
        loading={
            <div className="flex items-center justify-center py-12">
                <Spinner/>
            </div>
        }
    >
        {(resourceData) => <GenericHalPageContent resourceData={resourceData}/>}
    </HalPageGate>
);
