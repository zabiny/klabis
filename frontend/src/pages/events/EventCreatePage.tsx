import {type ReactElement} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {Button, DetailRow} from '../../components/UI';
import {Section} from '../members/MemberSection';
import {eventFormFieldsFactory} from '../../components/events/eventFormFieldsFactory';
import {labels} from '../../localization';
import {HalFormPanel} from '../../components/HalNavigator2/HalFormPanel';
import type {HalFormPanelRenderHelpers} from '../../components/HalNavigator2/HalFormPanel';

const BASIC_FIELDS = ['name', 'eventDate', 'location', 'organizer', 'websiteUrl'];
const COORDINATION_FIELDS = ['eventCoordinatorId', 'eventTypeId'];
const DEADLINE_FIELDS = ['deadlines'];
const CATEGORY_FIELDS = ['categories'];

export const EventCreatePage = (): ReactElement => {
    const navigate = useNavigate();

    return (
        <HalFormPanel
            collectionUrl="/api/events"
            templateName="createEvent"
            fieldsFactory={eventFormFieldsFactory}
            successMessage="Akce byla úspěšně vytvořena"
            onSuccess={() => navigate('/events')}
            templateMissingMessage="Vytvoření akce není k dispozici."
        >
            {({renderInput, renderField, hasField}: HalFormPanelRenderHelpers) => {
                const hasFields = (fieldNames: string[]) => fieldNames.some(f => hasField(f));
                return (
                    <div className="flex flex-col gap-8">
                        <div>
                            <Link to="/events" className="text-sm text-primary hover:text-primary-light">
                                {labels.ui.backToList}
                            </Link>
                        </div>

                        <div>
                            <h1 className="text-3xl font-bold text-text-primary">
                                {labels.sections.newEvent}
                            </h1>
                            <hr className="border-border mt-4"/>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                            <div className="flex flex-col gap-6">
                                {hasFields(BASIC_FIELDS) && (
                                    <Section title={labels.sections.eventBasicInfo}>
                                        {hasField('name') && <DetailRow label={labels.fields.name}>{renderInput('name')}</DetailRow>}
                                        {hasField('eventDate') && <DetailRow label={labels.fields.eventDate}>{renderInput('eventDate')}</DetailRow>}
                                        {hasField('location') && <DetailRow label={labels.fields.location}>{renderInput('location')}</DetailRow>}
                                        {hasField('organizer') && <DetailRow label={labels.fields.organizer}>{renderInput('organizer')}</DetailRow>}
                                        {hasField('websiteUrl') && <DetailRow label={labels.fields.websiteUrl}>{renderInput('websiteUrl')}</DetailRow>}
                                    </Section>
                                )}
                            </div>

                            <div className="flex flex-col gap-6">
                                {hasFields(COORDINATION_FIELDS) && (
                                    <Section title={labels.sections.eventCoordination}>
                                        {hasField('eventCoordinatorId') && (
                                            <DetailRow label={labels.fields.eventCoordinatorId}>
                                                {renderInput('eventCoordinatorId')}
                                            </DetailRow>
                                        )}
                                        {hasField('eventTypeId') && (
                                            <DetailRow label={labels.fields.eventTypeId}>
                                                {renderInput('eventTypeId')}
                                            </DetailRow>
                                        )}
                                    </Section>
                                )}
                            </div>
                        </div>

                        {hasFields(DEADLINE_FIELDS) && (
                            <Section title={labels.sections.eventDeadlines}>
                                {renderInput('deadlines')}
                            </Section>
                        )}

                        {hasFields(CATEGORY_FIELDS) && (
                            <Section title={labels.sections.eventCategories}>
                                {renderInput('categories')}
                            </Section>
                        )}

                        <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-border">
                            <Link to="/events">
                                <Button variant="secondary">
                                    {labels.buttons.cancel}
                                </Button>
                            </Link>
                            {renderField('submit')}
                        </div>
                    </div>
                );
            }}
        </HalFormPanel>
    );
};
