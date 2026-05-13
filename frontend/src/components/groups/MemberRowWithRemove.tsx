import type {ReactElement} from 'react';
import {Button, DetailRow} from '../UI';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../members/MemberNameWithRegNumber.tsx';
import {UserMinus} from 'lucide-react';
import type {HalResourceLinks} from '../../api';

interface MemberRowWithRemoveProps {
    memberId: string;
    memberLink: HalResourceLinks;
    removeAriaLabel?: string;
    onRemove?: () => void;
}

export const MemberRowWithRemove = ({memberId, memberLink, removeAriaLabel, onRemove}: MemberRowWithRemoveProps): ReactElement => (
    <DetailRow key={memberId} label="">
        <div className="flex items-center justify-between w-full">
            <HalRouteProvider routeLink={memberLink}>
                <MemberNameWithRegNumber/>
            </HalRouteProvider>
            {onRemove && (
                <Button
                    variant="ghost"
                    size="sm"
                    className="text-red-600"
                    aria-label={removeAriaLabel}
                    onClick={onRemove}
                >
                    <UserMinus className="w-4 h-4"/>
                </Button>
            )}
        </div>
    </DetailRow>
);
