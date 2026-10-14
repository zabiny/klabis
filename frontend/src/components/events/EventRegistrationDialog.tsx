import {type ReactElement, useCallback, useEffect, useMemo, useState} from 'react';
import {type LucideIcon, Calendar, Check, FileText, Hourglass, MapPin, Pencil, UserPlus} from 'lucide-react';
import {getFieldLabel, labels} from '../../localization';
import {renderYesNo} from '../../utils/formatters';
import {Alert, Button, DetailRow, Modal, Skeleton} from '../UI';
import {type RegistrationDialogData, type RegistrationDialogMode, useRegistrationDialogData} from '../../hooks/useRegistrationDialogData';
import {isFormValidationError, toFormValidationError} from '../../api/hateoas';
import type {Link} from '../../api/types';
import {formatDate, formatDateTime, getRelevantDeadlineIndex, getTodayIso} from '../../utils/dateUtils';
import {normalizeKlabisApiPath} from '../../utils/halFormsUtils';
import {HalFormDisplay} from '../HalNavigator2/HalFormDisplay';
import type {FormRenderHelpers, RenderFormCallback} from '../HalNavigator2/halforms';
import * as Yup from 'yup';
import {createRegistrationFieldsFactory, type SiRequirement} from './registrationFieldsFactory.tsx';

export interface EventRegistrationDialogProps {
    registration: Link | null;
    onClose: () => void;
    onRegistered?: () => void;
}

const MODE_PRESENTATION: Record<RegistrationDialogMode, {title: string; Icon: LucideIcon}> = {
    new: {title: labels.dialogTitles.registerForEvent, Icon: UserPlus},
    view: {title: labels.dialogTitles.registrationDetail, Icon: FileText},
    edit: {title: labels.dialogTitles.editRegistration, Icon: Pencil},
};

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
    const hasInternalNote = template?.properties.some(prop => prop.name === 'internalNote');
    const hasOrisNote = template?.properties.some(prop => prop.name === 'orisNote');
    const categoryProp = template?.properties.find(prop => prop.name === 'categoryId');
    const showTransportCheckbox = transportProp !== undefined && eventContext?.sharedTransportEnabled === true;
    const showAccommodationCheckbox = accommodationProp !== undefined && eventContext?.sharedAccommodationEnabled === true;

    const siUsage = eventContext?.siUsage ?? 'OPTIONAL';
    const showSiField = siUsage !== 'NOT_USED';
    const hasRentProp = template?.properties.some(prop => prop.name === 'wantsToRentSi') ?? false;
    const showRentCheckbox = hasRentProp && showSiField && eventContext?.siRentalAllowed === true;
    const siRequirement: SiRequirement = siUsage !== 'REQUIRED'
        ? 'none'
        : showRentCheckbox ? 'unlessRenting' : 'required';

    const fieldsFactory = useMemo(
        () => createRegistrationFieldsFactory({mode: mode === 'edit' ? 'edit' : 'new', siRequirement, serverFieldErrors}),
        [mode, siRequirement, serverFieldErrors],
    );

    const refineValidationSchema = useCallback(
        (schema: Yup.ObjectSchema<Record<string, unknown>>) => siRequirement !== 'unlessRenting'
            ? schema
            : schema.test('si-required-unless-renting', function (values) {
                const siNumber = typeof values?.siCardNumber === 'string' ? values.siCardNumber.trim() : '';
                if (siNumber || values?.wantsToRentSi) return true;
                return this.createError({
                    path: 'siCardNumber',
                    message: labels.events.registrationModal.siChipRequiredUnlessRenting,
                });
            }),
        [siRequirement],
    );

    if (!registration) {
        return null;
    }

    const normalizedTemplate = template
        ? {
            ...template,
            target: normalizeKlabisApiPath(template.target ?? ''),
            properties: template.properties
                .filter(prop => !(prop.name === 'siCardNumber' && !showSiField)
                    && !(prop.name === 'wantsToRentSi' && !showRentCheckbox))
                .map(prop => prop.name === 'siCardNumber'
                    ? {...prop, required: siRequirement === 'required'}
                    : prop),
        }
        : undefined;

    const missingAffordance = !isLoading && !error && mode === undefined;
    const {title: baseTitle, Icon: HeaderIcon} = MODE_PRESENTATION[mode ?? 'edit'];
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
        const withDefaults = {...base, wantsToRentSi: base.wantsToRentSi === true};
        return categoryId !== undefined ? {...withDefaults, categoryId} : withDefaults;
    })();

    const stripHiddenSharedServices = (payload: Record<string, unknown>): Record<string, unknown> => {
        const next = {...payload};
        if (!showTransportCheckbox) delete next.wantsSharedTransport;
        if (!showAccommodationCheckbox) delete next.wantsSharedAccommodation;
        if (!showSiField) delete next.siCardNumber;
        if (!showRentCheckbox) delete next.wantsToRentSi;
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
            {showSiField && helpers.renderField('siCardNumber')}
            {showRentCheckbox && helpers.renderField('wantsToRentSi')}
            {categoryProp && helpers.renderField('categoryId')}
            {(showTransportCheckbox || showAccommodationCheckbox) && (
                <div className="flex flex-col gap-3">
                    {showTransportCheckbox && helpers.renderField('wantsSharedTransport')}
                    {showAccommodationCheckbox && helpers.renderField('wantsSharedAccommodation')}
                </div>
            )}
            {hasInternalNote && helpers.renderField('internalNote')}
            {hasOrisNote && helpers.renderField('orisNote')}
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
            headerIcon={<HeaderIcon className="h-5 w-5 text-primary"/>}
            context={!isLoading && eventContext
                ? <RegistrationContext event={eventContext}/>
                : undefined}
        >
            {isLoading ? (
                <DialogSkeleton/>
            ) : error ? (
                <Alert severity="error">{labels.events.registrationModal.prefillLoadError}</Alert>
            ) : mode === 'view' && prefillData ? (
                <ReadOnlyRegistration
                    registration={resolvedResource}
                    memberName={memberName}
                    eventContext={eventContext}
                    onClose={handleClose}
                />
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
                    refineValidationSchema={refineValidationSchema}
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

const hasText = (value: unknown): boolean => typeof value === 'string' && value.trim() !== '';

function ReadOnlyRegistration({registration, memberName, eventContext, onClose}: {
    registration: Record<string, unknown>;
    memberName: string;
    eventContext: RegistrationDialogData['eventContext'];
    onClose: () => void;
}) {
    const category = (registration.category as {name?: string} | null | undefined)?.name;
    const registeredAt = registration.registeredAt as string | undefined;

    return (
        <div data-testid="event-registration-detail" className="flex flex-col gap-4">
            <dl className="flex flex-col">
                {memberName && <DetailRow label={labels.fields.memberId}>{memberName}</DetailRow>}
                {category && <DetailRow label={labels.events.registrationModal.category}>{category}</DetailRow>}
                {registration.siCardNumber != null && (
                    <DetailRow label={labels.events.registrationModal.siChip}>{String(registration.siCardNumber)}</DetailRow>
                )}
                {registration.wantsToRentSi === true && (
                    <DetailRow label={labels.events.registrationModal.rentSi}>{renderYesNo(true)}</DetailRow>
                )}
                {registeredAt && (
                    <DetailRow label={labels.tables.registeredAt}>{formatDateTime(registeredAt)}</DetailRow>
                )}
                {eventContext?.sharedTransportEnabled && registration.wantsSharedTransport != null && (
                    <DetailRow label={getFieldLabel('wantsSharedTransport')}>{renderYesNo(registration.wantsSharedTransport)}</DetailRow>
                )}
                {eventContext?.sharedAccommodationEnabled && registration.wantsSharedAccommodation != null && (
                    <DetailRow label={getFieldLabel('wantsSharedAccommodation')}>{renderYesNo(registration.wantsSharedAccommodation)}</DetailRow>
                )}
                {hasText(registration.internalNote) && (
                    <DetailRow label={getFieldLabel('internalNote')}>{String(registration.internalNote)}</DetailRow>
                )}
                {hasText(registration.orisNote) && (
                    <DetailRow label={getFieldLabel('orisNote')}>{String(registration.orisNote)}</DetailRow>
                )}
            </dl>
            <div className="mt-2 flex items-center justify-end border-t border-border pt-4">
                <Button variant="secondary" onClick={onClose}>{labels.buttons.close}</Button>
            </div>
        </div>
    );
}

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
