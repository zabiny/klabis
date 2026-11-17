import {getLinkLabel, halResource} from './halResource';

describe('halResource', () => {
    describe('presence', () => {
        it('answers undefined for a missing resource', () => {
            expect(halResource(undefined).template('updateEvent')).toBeUndefined();
            expect(halResource(undefined).link('self')).toBeUndefined();
            expect(halResource(null).template('updateEvent')).toBeUndefined();
            expect(halResource(null).link('self')).toBeUndefined();
        });

        it('answers undefined for any name when the resource carries neither _templates nor _links', () => {
            expect(halResource({}).template('updateEvent')).toBeUndefined();
            expect(halResource({}).link('self')).toBeUndefined();
            expect(halResource({_templates: {}, _links: {}}).template('updateEvent')).toBeUndefined();
            expect(halResource({_templates: {}, _links: {}}).link('self')).toBeUndefined();
        });

        it('answers only for the entries the resource actually carries', () => {
            const resource = {_templates: {updateEvent: {properties: []}}};

            expect(halResource(resource).template('updateEvent')).toBeDefined();
            expect(halResource(resource).template('createEvent')).toBeUndefined();
            expect(halResource(resource).link('self')).toBeUndefined();
        });
    });

    describe('template href', () => {
        it('uses the template target when the server sends one', () => {
            const action = halResource({
                _links: {self: {href: '/api/things/1'}},
                _templates: {reverse: {target: '/api/things/1/reverse', properties: []}},
            }).template('reverse');

            expect(action?.href).toBe('/api/things/1/reverse');
        });

        it('falls back to the resource self link when the template has no target', () => {
            const action = halResource({
                _links: {self: {href: '/api/things/1'}},
                _templates: {updateEvent: {properties: []}},
            }).template('updateEvent');

            expect(action?.href).toBe('/api/things/1');
        });

        it('has no href without a target and without a self link', () => {
            expect(halResource({_templates: {updateEvent: {properties: []}}}).template('updateEvent')?.href)
                .toBeUndefined();
        });

        it('carries the resolved template itself', () => {
            const template = {properties: []};
            expect(halResource({_templates: {updateEvent: template}}).template('updateEvent')?.template)
                .toBe(template);
        });
    });

    describe('template texts', () => {
        it('takes the label from labels.templates, before the server title', () => {
            const action = halResource({
                _templates: {cancelEvent: {title: 'Zrušit', properties: []}},
            }).template('cancelEvent');

            expect(action?.label).toBe('Zrušit akci');
        });

        it('falls back to the template title when the name has no label', () => {
            expect(halResource({_templates: {unknownTemplate: {title: 'Něco', properties: []}}})
                .template('unknownTemplate')?.label).toBe('Něco');
        });

        it('falls back to the name when there is neither a label nor a title', () => {
            expect(halResource({_templates: {unknownTemplate: {properties: []}}})
                .template('unknownTemplate')?.label).toBe('unknownTemplate');
        });

        it('takes the dialog title from labels.dialogTitles', () => {
            expect(halResource({_templates: {cancelEvent: {properties: []}}})
                .template('cancelEvent')?.dialogTitle).toBe('Zrušení akce');
        });

        it('falls back to the label when the name has no dialog title', () => {
            expect(halResource({_templates: {updateEvent: {properties: []}}})
                .template('updateEvent')?.dialogTitle).toBe('Upravit');
        });

        it('builds the success message from the template title', () => {
            expect(halResource({_templates: {updateEvent: {title: 'Upravit', properties: []}}})
                .template('updateEvent')?.successMessage).toBe('Upravit — úspěšně uloženo');
        });

        it('falls back to the generic success message without a template title', () => {
            expect(halResource({_templates: {updateEvent: {properties: []}}})
                .template('updateEvent')?.successMessage).toBe('Úspěšně uloženo');
        });
    });

    describe('link', () => {
        it('resolves a single link to its href', () => {
            const link = {href: '/api/things/1'};

            expect(halResource({_links: {self: link}}).link('self')).toEqual({
                rel: 'self',
                link,
                href: '/api/things/1',
                label: 'self',
            });
        });

        it('returns the first non-templated link of an array relation', () => {
            const resource = {
                _links: {
                    search: [
                        {href: '/api/things{?name}', templated: true},
                        {href: '/api/things?name=x'},
                    ],
                },
            };

            expect(halResource(resource).link('search')?.href).toBe('/api/things?name=x');
        });

        it('answers undefined when an array relation has no concrete link', () => {
            const resource = {_links: {search: [{href: '/api/things{?name}', templated: true}]}};

            expect(halResource(resource).link('search')).toBeUndefined();
        });

        it('keeps a single templated link, as getResourceLink does', () => {
            const link = {href: '/api/things{?name}', templated: true};

            expect(halResource({_links: {search: link}}).link('search')?.href).toBe('/api/things{?name}');
        });

        it('answers undefined for a link with an empty href', () => {
            expect(halResource({_links: {self: {href: ''}}}).link('self')).toBeUndefined();
        });

        it('takes the label from labels.links', () => {
            expect(halResource({_links: {legalGuardians: {href: '/api/things/1'}}})
                .link('legalGuardians')?.label).toBe('Zákonní zástupci');
        });

        it('falls back to the link title, then to the rel', () => {
            expect(halResource({_links: {someRel: {href: '/api/things/1', title: 'Něco'}}})
                .link('someRel')?.label).toBe('Něco');
            expect(halResource({_links: {someRel: {href: '/api/things/1'}}})
                .link('someRel')?.label).toBe('someRel');
        });
    });
});

describe('getLinkLabel', () => {
    it('reads the link label map', () => {
        expect(getLinkLabel('trainingGroup')).toBe('Tréninková skupina');
    });

    it('answers undefined for an unknown relation', () => {
        expect(getLinkLabel('someRel')).toBeUndefined();
    });
});
