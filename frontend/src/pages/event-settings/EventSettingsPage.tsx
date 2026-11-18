import type {ReactElement} from 'react';
import {Navigate, NavLink, Outlet, useLocation} from 'react-router-dom';
import {Skeleton, TabButton} from '../../components/UI';
import {EVENT_SETTINGS_TAB_RELS, useEventSettingsTabs} from '../../hooks/useRootNavigation';
import {labels} from '../../localization';
import ForbiddenPage from '../ForbiddenPage';
import {EVENT_SETTINGS_BASE} from './eventSettingsBase';

export const EventSettingsPage = (): ReactElement => {
    const {data: items, isLoading} = useEventSettingsTabs();
    const {pathname} = useLocation();

    if (isLoading || !items) {
        return <Skeleton/>;
    }

    const availableRels = EVENT_SETTINGS_TAB_RELS.filter(rel => items.some(i => i.rel === rel));
    if (availableRels.length === 0) {
        return <ForbiddenPage/>;
    }

    const activeRel = availableRels.find(rel => pathname === `${EVENT_SETTINGS_BASE}/${rel}`);
    if (!activeRel) {
        return <Navigate to={`${EVENT_SETTINGS_BASE}/${availableRels[0]}`} replace/>;
    }

    return (
        <div className="flex flex-col gap-4">
            <h1 className="text-3xl font-bold text-text-primary">{labels.nav['event-settings']}</h1>
            <div role="tablist" className="flex border-b border-border">
                {availableRels.map(rel => (
                    <NavLink key={rel} to={`${EVENT_SETTINGS_BASE}/${rel}`} tabIndex={-1} className="no-underline">
                        <TabButton active={rel === activeRel} onClick={() => undefined} label={labels.nav[rel]}/>
                    </NavLink>
                ))}
            </div>
            <Outlet/>
        </div>
    );
};
