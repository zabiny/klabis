import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {ImportOrisEventsButton} from './ImportOrisEventsButton';
import {mockHalFormsTemplate, mockHalResponse} from '../../__mocks__/halData.ts';
import type {HalFormsTemplate, HalResponse} from '../../api';

const importTemplate = (): HalFormsTemplate => mockHalFormsTemplate({
    method: 'POST',
    target: '/api/events/importFromOris',
    properties: [{name: 'orisIds', type: 'array', max: 50}],
});

const eventsResource = (templates: Record<string, HalFormsTemplate> = {}): HalResponse => ({
    ...mockHalResponse(),
    _templates: templates,
});

const importButton = () => screen.queryByRole('button', {name: /importovat z oris/i});

describe('ImportOrisEventsButton', () => {
    describe('presence rule', () => {
        it('renders nothing when the resource carries no import template', () => {
            render(<ImportOrisEventsButton resource={eventsResource()}/>);

            expect(importButton()).toBeNull();
        });

        it('renders the button when the resource carries only the single-event import template', () => {
            render(<ImportOrisEventsButton resource={eventsResource({importEvent: importTemplate()})}/>);

            expect(importButton()).toBeInTheDocument();
        });

        it('renders the button when the resource carries the batch import template', () => {
            render(<ImportOrisEventsButton resource={eventsResource({importEventsBatch: importTemplate()})}/>);

            expect(importButton()).toBeInTheDocument();
        });
    });
});
