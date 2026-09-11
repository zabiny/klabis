import {type ReactElement, useEffect, useMemo, useState} from 'react';
import {Calendar, Check, Hourglass, MapPin, Pencil, UserPlus} from 'lucide-react';
import {labels} from '../../localization';
import {Alert, Modal, Skeleton} from '../UI';
import {type RegistrationDialogData, useRegistrationDialogData} from '../../hooks/useRegistrationDialogData';
import {isFormValidationError, toFormValidationError} from '../../api/hateoas';
import type {Link} from '../../api/types';
import {formatDate, getRelevantDeadlineIndex, getTodayIso} from '../../utils/dateUtils';
import {normalizeKlabisApiPath} from '../../utils/halFormsUtils';
import {HalFormDisplay} from '../HalNavigator2/HalFormDisplay';
import type {FormRenderHelpers, RenderFormCallback} from '../HalNavigator2/halforms';
import {createRegistrationFieldsFactory} from './registrationFieldsFactory.tsx';

export interface EventRegistrationDialogProps {
    registration: Link | null;
    onClose: () => void;
    onRegistered?: () => void;
}

export const EventRegistrationDialog = ({
    registration,
    onClose,
    onRegistered,
}: EventRegistrationDialogProps): ReactElement | null => {
    const {mode, template, memberName, prefillData, eventContext, isLoading, error} = useRegistrationDialogData(registration);

    const [serverFieldErrors, setServerFieldErrors] = useState<Record<string, string>>({});
    useEffect(() => {
        setServerFieldErrors({});
    }, [registration?.href]);

    const transportProp = template?.properties.find(prop => prop.name === 'wantsSharedTransport');
    const accommodationProp = template?.properties.find(prop => prop.name === 'wantsSharedAccommodation');
    const categoryProp = template?.properties.find(prop => prop.name === 'categoryId');
    const showTransportCheckbox = transportProp !== undefined && eventContext?.sharedTransportEnabled === true;
    const showAccommodationCheckbox = accommodationProp !== undefined && eventContext?.sharedAccommodationEnabled === true;

    const fieldsFactory = useMemo(
        () => createRegistrationFieldsFactory({mode: mode ?? 'new', serverFieldErrors}),
        [mode, serverFieldErrors],
    );

    if (!registration) {
        return null;
    }

    const normalizedTemplate = template
        ? {...template, target: normalizeKlabisApiPath(template.target ?? '')}
        : undefined;

    const missingAffordance = !isLoading && !error && mode === undefined;
    const baseTitle = mode === 'new'
        ? labels.dialogTitles.registerForEvent
        : labels.dialogTitles.editRegistration;
    const dialogTitle = memberName ? `${baseTitle} - ${memberName}` : baseTitle;
    const confirmLabel = mode === 'new'
        ? labels.events.registrationModal.confirmNew
        : labels.events.registrationModal.confirmEdit;

    const handleClose = () => {
        setServerFieldErrors({});
        onClose();
    };

    const resolvedResource: Record<string, unknown> = (() => {
        if (!prefillData) return {};
        const base = prefillData as unknown as Record<string, unknown>;
        const categoryId = (prefillData as {category?: {id?: string}}).category?.id;
        return categoryId !== undefined ? {...base, categoryId} : {...base};
    })();

    const stripHiddenSharedServices = (payload: Record<string, unknown>): Record<string, unknown> => {
        const next = {...payload};
        if (!showTransportCheckbox) delete next.wantsSharedTransport;
        if (!showAccommodationCheckbox) delete next.wantsSharedAccommodation;
        return next;
    };

    const handleSubmitError = (submitError: unknown): boolean => {
        const validationError = toFormValidationError(submitError);
        if (isFormValidationError(validationError)) {
            setServerFieldErrors(validationError.validationErrors);
        }
        return false;
    };

    const renderForm: RenderFormCallback = (helpers: FormRenderHelpers) => (
        <div
            data-testid="event-registration-form"
            className="flex flex-col gap-4"
        >
            {helpers.renderField('siCardNumber')}
            {categoryProp && helpers.renderField('categoryId')}
            {(showTransportCheckbox || showAccommodationCheckbox) && (
                <div className="flex flex-col gap-3">
                    {showTransportCheckbox && helpers.renderField('wantsSharedTransport')}
                    {showAccommodationCheckbox && helpers.renderField('wantsSharedAccommodation')}
                </div>
            )}
            <div className="mt-2 flex items-center justify-end gap-3 border-t border-border pt-4">
                {helpers.renderField('cancel')}
                {helpers.renderField('submit')}
            </div>
        </div>
    );

    return (
        <Modal
            isOpen={true}
            onClose={handleClose}
            closeOnBackdropClick={true}
            size="lg"
            title={dialogTitle}
            headerIcon={mode === 'new'
                ? <UserPlus className="h-5 w-5 text-primary"/>
                : <Pencil className="h-5 w-5 text-primary"/>}
            context={!isLoading && eventContext
                ? <RegistrationContext event={eventContext}/>
                : undefined}
        >
            {isLoading ? (
                <DialogSkeleton/>
            ) : error ? (
                <Alert severity="error">{labels.events.registrationModal.prefillLoadError}</Alert>
            ) : missingAffordance || !normalizedTemplate ? (
                <Alert severity="error">{labels.events.registrationModal.noRegistrationAffordance}</Alert>
            ) : (
                <HalFormDisplay
                    template={normalizedTemplate}
                    templateName={mode === 'new' ? 'registerForEvent' : 'editRegistration'}
                    resourceData={resolvedResource}
                    pathname={normalizedTemplate.target}
                    onClose={handleClose}
                    onSubmitSuccess={() => onRegistered?.()}
                    onSubmitError={handleSubmitError}
                    postprocessPayload={stripHiddenSharedServices}
                    navigateOnSuccess={false}
                    successMessage={normalizedTemplate.title
                        ? labels.events.registrationModal.savedWithTemplate(normalizedTemplate.title)
                        : labels.ui.savedSuccessfully}
                    submitButtonLabel={confirmLabel}
                    submitIcon={<Check className="h-4 w-4"/>}
                    fieldsFactory={fieldsFactory}
                    customLayout={renderForm}
                />
            )}
        </Modal>
    );
};

function DialogSkeleton() {
    return (
        <div data-testid="event-registration-dialog-skeleton" className="flex flex-col gap-4">
            <Skeleton height="1.25rem" width="60%"/>
            <Skeleton height="1rem" width="45%"/>
            <Skeleton height="2.75rem"/>
            <Skeleton height="2.75rem"/>
        </div>
    );
}

function RegistrationContext({
    event,
}: {
    event: RegistrationDialogData['eventContext'];
}) {
    const relevantDeadline = event?.deadlines && event.deadlines.length > 0
        ? event.deadlines[getRelevantDeadlineIndex(event.deadlines, getTodayIso())]
        : undefined;

    return (
        <div className="flex flex-col gap-2.5">
            {event?.name && (
                <p className="text-base font-bold text-text-primary">{event.name}</p>
            )}
            {(event?.eventDate || event?.location) && (
                <div className="flex flex-wrap items-center gap-[18px]">
                    {event?.eventDate && (
                        <span className="flex items-center gap-1.5">
                            <Calendar className="h-3.5 w-3.5 text-primary"/>
                            <span className="text-[13px] text-text-secondary">{formatDate(event.eventDate)}</span>
                        </span>
                    )}
                    {event?.location && (
                        <span className="flex items-center gap-1.5">
                            <MapPin className="h-3.5 w-3.5 text-primary"/>
                            <span className="text-[13px] text-text-secondary">{event.location}</span>
                        </span>
                    )}
                </div>
            )}
            {relevantDeadline && (
                <span
                    data-testid="registration-deadline-chip"
                    className="inline-flex items-center gap-2 self-start rounded-md border border-orange-200 bg-orange-50 px-3 py-1.5"
                >
                    <Hourglass className="h-3.5 w-3.5 text-amber-600"/>
                    <span className="text-xs font-semibold text-amber-800">
                        {labels.events.registrationModal.deadlinePrefix(formatDate(relevantDeadline))}
                    </span>
                </span>
            )}
        </div>
    );
}
