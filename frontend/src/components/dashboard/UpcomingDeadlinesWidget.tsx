import {type ReactElement, useState} from 'react';
import {Link as RouterLink} from 'react-router-dom';
import {Calendar, UserPlus} from 'lucide-react';
import {Button, Card, Modal} from '../UI';
import {HalFormDisplay} from '../HalNavigator2/HalFormDisplay';
import {useUpcomingDeadlines} from '../../hooks/useUpcomingDeadlines';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {formatDate} from '../../utils/dateUtils';
import {normalizeKlabisApiPath} from '../../utils/halFormsUtils';
import {extractNavigationPath} from '../../utils/navigationPath';
import {labels} from '../../localization/labels';
import type {HalFormsTemplate} from '../../api';

const SHOW_ALL_DEADLINES_PATH = '/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me';

interface RegistrationModalState {
    newRegistrationHref: string;
    eventName: string;
    registerForEventTemplate: HalFormsTemplate;
}

interface NewRegistrationPrefillData {
    _templates?: Record<string, HalFormsTemplate>;
    [key: string]: unknown;
}

const RegistrationModal = ({
    state,
    onClose,
}: {
    state: RegistrationModalState;
    onClose: () => void;
}): ReactElement => {
    const {data: prefillData} = useAuthorizedQuery<NewRegistrationPrefillData>(state.newRegistrationHref, {
        staleTime: 0,
        gcTime: 0,
        retry: false,
    });

    const registerTemplate = prefillData?._templates?.registerForEvent ?? state.registerForEventTemplate;
    const registerTemplatePath = registerTemplate.target
        ? normalizeKlabisApiPath(registerTemplate.target)
        : state.newRegistrationHref;

    return (
        <Modal isOpen={true} onClose={onClose} title={labels.templates.registerForEvent} size="2xl">
            {prefillData && (
                <HalFormDisplay
                    template={registerTemplate}
                    templateName="registerForEvent"
                    resourceData={prefillData as unknown as Record<string, unknown>}
                    pathname={registerTemplatePath}
                    onClose={onClose}
                    navigateOnSuccess={false}
                />
            )}
        </Modal>
    );
};

export interface UpcomingDeadlinesWidgetProps {
    upcomingDeadlinesHref: string | undefined;
}

export const UpcomingDeadlinesWidget = ({upcomingDeadlinesHref}: UpcomingDeadlinesWidgetProps): ReactElement | null => {
    const [registrationModal, setRegistrationModal] = useState<RegistrationModalState | null>(null);
    const {data} = useUpcomingDeadlines(upcomingDeadlinesHref);

    const items = data?.items ?? [];
    const totalElements = data?.totalElements ?? 0;
    const hasMore = totalElements > items.length;

    if (!upcomingDeadlinesHref || items.length === 0) {
        return null;
    }

    return (
        <>
            <div>
                <h2 className="text-2xl font-display font-bold text-text-primary mb-4">
                    {labels.dashboard.upcomingDeadlinesTitle}
                </h2>
                <Card className="overflow-hidden">
                    <div className="divide-y divide-border">
                        {items.map((item) => (
                            <RouterLink
                                key={item.selfHref}
                                to={extractNavigationPath(item.selfHref)}
                                className="flex items-center justify-between p-4 hover:bg-surface-hover transition-colors"
                            >
                                <div className="flex items-center gap-4 flex-1 min-w-0">
                                    <div className="p-2 bg-amber-100 dark:bg-amber-900/30 rounded-lg shrink-0">
                                        <Calendar className="w-4 h-4 text-amber-600 dark:text-amber-400"/>
                                    </div>
                                    <div className="min-w-0">
                                        <p className="font-medium text-text-primary truncate">{item.name}</p>
                                        <p className="text-sm text-text-secondary">
                                            {formatDate(item.eventDate)}
                                            {item.deadline && (
                                                <span className="ml-2">
                                                    · {labels.dashboard.deadlinePrefix} {formatDate(item.deadline)}
                                                </span>
                                            )}
                                        </p>
                                    </div>
                                </div>
                                {item.newRegistrationHref && item.registerForEventTemplate && (
                                    <Button
                                        variant="primary"
                                        size="sm"
                                        className="ml-3 shrink-0"
                                        onClick={(e) => {
                                            e.preventDefault();
                                            setRegistrationModal({
                                                newRegistrationHref: item.newRegistrationHref!,
                                                eventName: item.name,
                                                registerForEventTemplate: item.registerForEventTemplate!,
                                            });
                                        }}
                                    >
                                        <UserPlus className="w-4 h-4 mr-1"/>
                                        {labels.dashboard.registerForEvent}
                                    </Button>
                                )}
                            </RouterLink>
                        ))}
                    </div>
                    {hasMore && (
                        <div className="p-4 border-t border-border">
                            <RouterLink
                                to={SHOW_ALL_DEADLINES_PATH}
                                className="text-sm font-medium text-primary hover:underline"
                            >
                                {labels.dashboard.showAllDeadlines}
                            </RouterLink>
                        </div>
                    )}
                </Card>
            </div>

            {registrationModal && (
                <RegistrationModal
                    state={registrationModal}
                    onClose={() => setRegistrationModal(null)}
                />
            )}
        </>
    );
};
