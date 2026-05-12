import {type ReactElement} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import type {HalResponse} from '../../api';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay';
import type {RenderFormCallback} from '../../components/HalNavigator2/halforms';
import {Alert, Button, DetailRow, Spinner} from '../../components/UI';
import {Section} from '../members/MemberSection';
import {eventFormFieldsFactory} from '../../components/events/eventFormFieldsFactory';
import {labels} from '../../localization';

const BASIC_FIELDS = ['name', 'eventDate', 'location', 'organizer', 'websiteUrl'];
const COORDINATION_FIELDS = ['eventCoordinatorId', 'eventTypeId'];
const DEADLINE_FIELDS = ['deadlines'];
const CATEGORY_FIELDS = ['categories'];

export const EventCreatePage = (): ReactElement => {
    const navigate = useNavigate();
    const {data: collectionData, isLoading, error} = useAuthorizedQuery<HalResponse>('/api/events');

    if (isLoading) {
        return (
            <div className="flex items-center gap-2">
                <Spinner/>
                <span>{labels.ui.loading}</span>
            </div>
        );
    }

    if (error) {
        return <Alert severity="error">{(error as Error).message}</Alert>;
    }

    const template = collectionData?._templates?.createEvent ?? null;

    if (!template) {
        return (
            <div className="flex flex-col gap-4">
                <Link to="/events" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
                <Alert severity="error">Vytvoření akce není k dispozici.</Alert>
            </div>
        );
    }

    const fieldNameSet = new Set(template.properties.map(p => p.name));

    const hasField = (fieldName: string) => fieldNameSet.has(fieldName);
    const hasFields = (fieldNames: string[]) => fieldNames.some(f => fieldNameSet.has(f));

    const renderForm: RenderFormCallback = ({renderInput, renderField}) => (
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

    return (
        <HalFormDisplay
            template={template}
            templateName="createEvent"
            resourceData={{}}
            pathname="/events"
            onClose={() => navigate('/events')}
            successMessage="Akce byla úspěšně vytvořena"
            fieldsFactory={eventFormFieldsFactory}
            customLayout={renderForm}
        />
    );
};
