import React from 'react';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {createMemberFilteredFactory, klabisFieldsFactory} from './KlabisFieldsFactory';
import type {HalFormsInputProps} from './HalNavigator2/halforms';

vi.mock('../hooks/useEventTypes', () => ({
    useEventTypes: () => ({
        eventTypes: [
            {id: 'uuid-race', name: 'Závod', color: 'red', sortOrder: 1},
            {id: 'uuid-training', name: 'Trénink', color: 'blue', sortOrder: 2},
        ],
        isLoading: false,
        getById: vi.fn(),
    }),
}));

vi.mock('../hooks/useMembershipFeeTierOptions', () => ({
    useMembershipFeeTierOptions: () => [
        {value: 'uuid-tier-a', prompt: 'Základní'},
        {value: 'uuid-tier-b', prompt: 'Premium'},
    ],
}));

vi.mock('./HalNavigator2/halforms/fields', async () => {
    const actual = await vi.importActual('./HalNavigator2/halforms/fields');
    return {
        ...(actual as object),
        HalFormsInput: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-input-${prop.name}`}>{prop.prompt}</div>
        ),
        HalFormsMemberId: ({prop, errorText, excludeIds, includeIds}: HalFormsInputProps & {excludeIds?: string[]; includeIds?: string[]}) => (
            <div data-testid="hal-forms-memberid-mock">
                <span data-testid="select-name">{prop.name}</span>
                <span data-testid="select-prompt">{prop.prompt}</span>
                {prop.options?.link?.href && (
                    <span data-testid="select-href">{prop.options.link.href}</span>
                )}
                {excludeIds && <span data-testid="select-excluded">{excludeIds.join(',')}</span>}
                {includeIds && <span data-testid="select-included">{includeIds.join(',')}</span>}
                {errorText && <span data-testid="select-error">{errorText}</span>}
            </div>
        ),
        HalFormsCheckboxGroup: ({prop}: HalFormsInputProps) => (
            <div data-testid="hal-forms-checkboxgroup-mock">
                <span data-testid="checkboxgroup-name">{prop.name}</span>
                <span data-testid="checkboxgroup-prompt">{prop.prompt}</span>
                {prop.options?.link?.href && (
                    <span data-testid="checkboxgroup-href">{prop.options.link.href}</span>
                )}
            </div>
        ),
        HalFormsSelect: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-select-${prop.name}`}>
                {prop.prompt}
                {prop.options?.link?.href && (
                    <span data-testid="select-href">{prop.options.link.href}</span>
                )}
            </div>
        ),
        HalFormsMultiSelect: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-multiselect-${prop.name}`}>
                <span data-testid="multiselect-prompt">{prop.prompt}</span>
                {prop.options?.link?.href && (
                    <span data-testid="multiselect-href">{prop.options.link.href}</span>
                )}
            </div>
        ),
    };
});

function createMockSubElementProps(): HalFormsInputProps['subElementProps'] {
    return vi.fn((attrName: string, configuration?: { prompt?: string; type?: string }) => ({
        prop: {
            name: `parent.${attrName}`,
            prompt: configuration?.prompt,
            type: configuration?.type || 'text',
        },
        errorText: undefined,
        subElementProps: vi.fn(),
    })) as unknown as HalFormsInputProps['subElementProps'];
}

function createMockConf(overrides: Partial<HalFormsInputProps> & { prop: HalFormsInputProps['prop'] }): HalFormsInputProps {
    return {
        errorText: undefined,
        subElementProps: createMockSubElementProps(),
        ...overrides,
    };
}

describe('KlabisFieldsFactory', () => {

    describe('MemberId field type', () => {

        it('should render HalFormsMemberId for MemberId type', () => {
            const mockConf = createMockConf({
                prop: {name: 'assignedMemberId', prompt: 'Select Member', type: 'MemberId'},
            });

            const result = klabisFieldsFactory('MemberId', mockConf);

            expect(result).not.toBeNull();
            const componentType = result?.type;
            if (typeof componentType === 'object' && componentType !== null && ('displayName' in componentType || 'name' in componentType)) {
                const ct = componentType as {displayName?: string; name?: string};
                expect(ct.displayName || ct.name).toMatch(/HalFormsMemberId/i);
            }
        });

        it('should pass through the backend-provided options.link untouched', () => {
            const mockConf = createMockConf({
                prop: {
                    name: 'memberId', prompt: 'Choose Member', type: 'MemberId',
                    options: {link: {href: 'http://localhost:8443/api/members/options'}},
                },
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('hal-forms-memberid-mock')).toBeInTheDocument();
            expect(screen.getByTestId('select-href')).toHaveTextContent('http://localhost:8443/api/members/options');
        });

        it('should preserve the original prompt from prop', () => {
            const mockConf = createMockConf({
                prop: {name: 'coordinatorId', prompt: 'Assign Coordinator', type: 'MemberId'},
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('select-prompt')).toHaveTextContent('Assign Coordinator');
        });

        it('should handle error text when provided', () => {
            const mockConf = createMockConf({
                prop: {name: 'memberId', prompt: 'Member', type: 'MemberId', required: true},
                errorText: 'Member selection is required',
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('select-error')).toHaveTextContent('Member selection is required');
        });

        it('should pass through other HalFormsInputProps to HalFormsMemberId', () => {
            const mockConf = createMockConf({
                prop: {name: 'testMemberId', prompt: 'Test Prompt', type: 'MemberId', required: true, readOnly: false},
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('select-name')).toHaveTextContent('testMemberId');
        });

        // D7: the base factory detects that a custom widget exists for "MemberId" (by probing
        // with a single-value-shaped conf) and, for a multi property, renders one row per item
        // through that same widget via HalFormsCollectionField — the member picker's options
        // link is shared across all rows.
        it('should render one HalFormsMemberId row per item for a multi MemberId field, via HalFormsCollectionField', () => {
            const mockConf = createMockConf({
                prop: {name: 'memberIds', prompt: 'Vyberte členy', type: 'MemberId', multiple: true},
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(
                <Formik initialValues={{memberIds: ['m1', 'm2']}} onSubmit={vi.fn()}>
                    <Form>{fieldElement}</Form>
                </Formik>
            );

            expect(screen.getAllByTestId('hal-forms-memberid-mock')).toHaveLength(2);
        });

        it('should pass through the backend-provided options.link to each row of a multi MemberId field', () => {
            const mockConf = createMockConf({
                prop: {
                    name: 'memberIds', prompt: 'Vyberte členy', type: 'MemberId', multiple: true,
                    options: {link: {href: 'http://localhost:8443/api/members/options'}},
                },
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(
                <Formik initialValues={{memberIds: ['m1', 'm2']}} onSubmit={vi.fn()}>
                    <Form>{fieldElement}</Form>
                </Formik>
            );

            const rows = screen.getAllByTestId('hal-forms-memberid-mock');
            expect(rows).toHaveLength(2);
            expect(screen.getAllByTestId('select-href')[0]).toHaveTextContent('http://localhost:8443/api/members/options');
        });

        it('should preserve the original prompt as the collection header for a multi MemberId field', () => {
            const mockConf = createMockConf({
                prop: {name: 'memberIds', prompt: 'Členové rodiny', type: 'MemberId', multiple: true},
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(
                <Formik initialValues={{memberIds: ['m1']}} onSubmit={vi.fn()}>
                    <Form>{fieldElement}</Form>
                </Formik>
            );

            expect(screen.getByText('Členové rodiny')).toBeInTheDocument();
        });
    });

    // A family group parent is a user of the system and need not be a club member, so the API
    // types the add-parent / create-group field as "UserId" instead of "MemberId". The options
    // still come from listMemberOptions (whose UUIDs are identical), so the member picker — not a
    // plain select — is what the field needs.
    describe('UserId field type', () => {

        it('should render the member picker for a UserId field', () => {
            const mockConf = createMockConf({
                prop: {
                    name: 'userId', prompt: 'Rodič', type: 'UserId',
                    options: {link: {href: 'http://localhost:8443/api/members/options'}},
                },
            });

            const fieldElement = klabisFieldsFactory('UserId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('hal-forms-memberid-mock')).toBeInTheDocument();
            expect(screen.getByTestId('select-href')).toHaveTextContent('http://localhost:8443/api/members/options');
        });

        it('should preserve the original field name and prompt', () => {
            const mockConf = createMockConf({
                prop: {name: 'userId', prompt: 'Rodič', type: 'UserId'},
            });

            const fieldElement = klabisFieldsFactory('UserId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('select-name')).toHaveTextContent('userId');
            expect(screen.getByTestId('select-prompt')).toHaveTextContent('Rodič');
        });

        it('should respect inline options instead of the member picker', () => {
            const mockConf = createMockConf({
                prop: {name: 'userId', prompt: 'Rodič', type: 'UserId', options: {inline: [{value: 'u1', prompt: 'Jana'}]}},
            });

            const fieldElement = klabisFieldsFactory('UserId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('hal-select-userId')).toBeInTheDocument();
            expect(screen.queryByTestId('hal-forms-memberid-mock')).not.toBeInTheDocument();
        });
    });

    describe('AddressRequest field type', () => {
        it('should call subElementProps with "street" (not "streetAndNumber")', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'address', prompt: 'Adresa', type: 'AddressRequest'},
                subElementProps: mockSubElementProps,
            });

            const fieldElement = klabisFieldsFactory('AddressRequest', mockConf);
            render(fieldElement!);

            expect(mockSubElementProps).toHaveBeenCalledWith('street', {prompt: 'Ulice'});
            expect(mockSubElementProps).toHaveBeenCalledWith('city', {prompt: 'Město'});
            expect(mockSubElementProps).toHaveBeenCalledWith('postalCode', {prompt: 'PSČ'});
            expect(mockSubElementProps).toHaveBeenCalledWith('country', {prompt: 'Stát'});
            expect(mockSubElementProps).not.toHaveBeenCalledWith('streetAndNumber', expect.anything());
        });
    });

    describe('AgeRangeRequest field type', () => {

        it('should call subElementProps with "minAge" and "maxAge" as number fields', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'ageRange', prompt: 'Věkové rozmezí', type: 'AgeRangeRequest'},
                subElementProps: mockSubElementProps,
            });

            const fieldElement = klabisFieldsFactory('AgeRangeRequest', mockConf);
            render(fieldElement!);

            expect(mockSubElementProps).toHaveBeenCalledWith('minAge', {prompt: 'Min. věk', type: 'number'});
            expect(mockSubElementProps).toHaveBeenCalledWith('maxAge', {prompt: 'Max. věk', type: 'number'});
        });
    });

    describe('IdentityCardDto field type', () => {

        it('should be registered under "IdentityCardDto" type name', () => {
            const mockConf = createMockConf({
                prop: {name: 'identityCard', prompt: 'Občanský průkaz', type: 'IdentityCardDto'},
            });

            const result = klabisFieldsFactory('IdentityCardDto', mockConf);
            expect(result).not.toBeNull();
        });

        it('should not be registered under old "IdentityCardApiDto" type name', () => {
            const mockConf = createMockConf({
                prop: {name: 'identityCard', prompt: 'Občanský průkaz', type: 'IdentityCardApiDto'},
            });

            const result = klabisFieldsFactory('IdentityCardApiDto', mockConf);
            expect(result).toBeNull();
        });

        it('should call subElementProps with "cardNumber" and "validityDate"', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'identityCard', prompt: 'Občanský průkaz', type: 'IdentityCardDto'},
                subElementProps: mockSubElementProps,
            });

            const fieldElement = klabisFieldsFactory('IdentityCardDto', mockConf);
            render(fieldElement!);

            expect(mockSubElementProps).toHaveBeenCalledWith('cardNumber', {prompt: 'Číslo OP'});
            expect(mockSubElementProps).toHaveBeenCalledWith('validityDate', {prompt: 'Platnost OP', type: 'date'});
            expect(mockSubElementProps).not.toHaveBeenCalledWith('number', expect.anything());
            expect(mockSubElementProps).not.toHaveBeenCalledWith('expiryDate', expect.anything());
        });
    });

    describe('GuardianDTO field type', () => {

        it('should render as single object with 5 sub-fields', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'guardian', prompt: 'Zákonný zástupce', type: 'GuardianDTO'},
                subElementProps: mockSubElementProps,
            });

            const fieldElement = klabisFieldsFactory('GuardianDTO', mockConf);
            render(fieldElement!);

            expect(mockSubElementProps).toHaveBeenCalledWith('firstName', {prompt: 'Jméno'});
            expect(mockSubElementProps).toHaveBeenCalledWith('lastName', {prompt: 'Příjmení'});
            expect(mockSubElementProps).toHaveBeenCalledWith('relationship', {prompt: 'Vztah'});
            expect(mockSubElementProps).toHaveBeenCalledWith('email', {prompt: 'E-mail', type: 'email'});
            expect(mockSubElementProps).toHaveBeenCalledWith('phone', {prompt: 'Telefon', type: 'tel'});
        });

        it('should not render FieldArray or add/remove buttons', () => {
            const mockConf = createMockConf({
                prop: {name: 'guardian', prompt: 'Zákonný zástupce', type: 'GuardianDTO'},
            });

            const fieldElement = klabisFieldsFactory('GuardianDTO', mockConf);
            render(fieldElement!);

            expect(screen.queryByText('Pridej')).not.toBeInTheDocument();
            expect(screen.queryByText('Odeber')).not.toBeInTheDocument();
        });
    });

    describe('MedicalCourseDto field type', () => {

        it('should be registered and render sub-fields', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'medicalCourse', prompt: 'Zdravotní kurz', type: 'MedicalCourseDto'},
                subElementProps: mockSubElementProps,
            });

            const result = klabisFieldsFactory('MedicalCourseDto', mockConf);
            expect(result).not.toBeNull();

            render(result!);

            expect(mockSubElementProps).toHaveBeenCalledWith('completionDate', {prompt: 'Datum absolvování kurzu', type: 'date'});
            expect(mockSubElementProps).toHaveBeenCalledWith('validityDate', {prompt: 'Platnost', type: 'date'});
        });
    });

    describe('TrainerLicenseDto field type', () => {

        it('should be registered and render sub-fields', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'trainerLicense', prompt: 'Trenérská licence', type: 'TrainerLicenseDto'},
                subElementProps: mockSubElementProps,
            });

            const result = klabisFieldsFactory('TrainerLicenseDto', mockConf);
            expect(result).not.toBeNull();

            render(result!);

            expect(mockSubElementProps).toHaveBeenCalledWith('level', {prompt: 'Stupeň'});
            expect(mockSubElementProps).toHaveBeenCalledWith('validityDate', {prompt: 'Platnost', type: 'date'});
        });
    });

    // D7: "UUID" alone no longer implies a member picker (that regressed multi UUID fields with
    // link options — coordinators, disciplineIds — into a single select). The member picker is
    // used only for the explicit "MemberId" field type hint (see 'MemberId field type' above);
    // a bare "UUID" field renders by its options/basic type instead.
    describe('UUID field type (no member picker without the MemberId hint)', () => {

        it('should render nothing for a single UUID field with no options (no widget registered for bare UUID)', () => {
            const mockConf = createMockConf({
                prop: {name: 'someId', prompt: 'Some Id', type: 'UUID'},
            });

            const fieldElement = klabisFieldsFactory('UUID', mockConf);

            expect(fieldElement).toBeNull();
        });

        it('should render the plain select (not the member picker) for a single UUID field with options.link', () => {
            const mockConf = createMockConf({
                prop: {
                    name: 'memberId', prompt: 'Vyberte člena', type: 'UUID',
                    options: {link: {href: 'http://localhost:8443/api/members/options'}},
                },
            });

            const fieldElement = klabisFieldsFactory('UUID', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('hal-select-memberId')).toBeInTheDocument();
            expect(screen.getByTestId('select-href')).toHaveTextContent('http://localhost:8443/api/members/options');
            expect(screen.queryByTestId('hal-forms-memberid-mock')).not.toBeInTheDocument();
        });

        it('should render the plain select (not the member picker) when the backend sends inline options for a UUID field', () => {
            const mockConf = createMockConf({
                prop: {
                    name: 'level', prompt: 'Úroveň', type: 'UUID',
                    options: {inline: [{value: 'a', prompt: 'A'}]},
                },
            });

            const fieldElement = klabisFieldsFactory('UUID', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('hal-select-level')).toBeInTheDocument();
            expect(screen.queryByTestId('hal-forms-memberid-mock')).not.toBeInTheDocument();
        });

        // Regression test: before the fix, a multi UUID field with backend-provided
        // options.link fell through to the single-value member picker (HalFormsMemberId),
        // submitting a scalar instead of an array — this is exactly the coordinators/
        // disciplineIds bug. Now, since "UUID" has no registered custom widget, it falls to
        // the generic multi-select — per row member pickers are reserved for the "MemberId"
        // field type hint (see 'MemberId field type' describe block above).
        it('should render the generic multi-select (not per-row member pickers) for a multi UUID field with options.link', () => {
            const mockConf = createMockConf({
                prop: {
                    name: 'memberIds', prompt: 'Vyberte členy', type: 'UUID', multiple: true,
                    options: {link: {href: 'http://localhost:8443/api/members/options'}},
                },
            });

            const fieldElement = klabisFieldsFactory('UUID', mockConf);
            render(
                <Formik initialValues={{memberIds: ['m1']}} onSubmit={vi.fn()}>
                    <Form>{fieldElement}</Form>
                </Formik>
            );

            expect(screen.getByTestId('multiselect-href')).toHaveTextContent('http://localhost:8443/api/members/options');
            expect(screen.queryByTestId('hal-forms-memberid-mock')).not.toBeInTheDocument();
            expect(screen.queryByTestId('collection-item')).not.toBeInTheDocument();
        });

        it('should render nothing per row for a multi UUID field without options (no widget registered for bare UUID)', () => {
            const mockConf = createMockConf({
                prop: {name: 'memberIds', prompt: 'Vyberte členy', type: 'UUID', multi: true},
            });

            const fieldElement = klabisFieldsFactory('UUID', mockConf);
            render(
                <Formik initialValues={{memberIds: ['m1']}} onSubmit={vi.fn()}>
                    <Form>{fieldElement}</Form>
                </Formik>
            );

            // Falls back to HalFormsCollectionField (no custom widget, no options), but each
            // row itself has nothing to render for bare "UUID" — no member-picker mock appears.
            expect(screen.getAllByTestId('collection-item')).toHaveLength(1);
            expect(screen.queryByTestId('hal-forms-memberid-mock')).not.toBeInTheDocument();
        });
    });

    describe('LegalGuardianInputRequest field type', () => {

        const optionsLink = {link: {href: 'http://localhost:8443/api/legal-guardian-options'}};

        it('should delegate the multi property to HalFormsCollectionField', () => {
            const mockConf = createMockConf({
                prop: {name: 'legalGuardians', prompt: 'Zástupci', type: 'LegalGuardianInputRequest', multiple: true, options: optionsLink},
            });

            const result = klabisFieldsFactory('LegalGuardianInputRequest', mockConf);

            const element = result as React.ReactElement;
            const componentName = (element.type as {displayName?: string; name?: string}).displayName
                ?? (element.type as {displayName?: string; name?: string}).name;
            expect(componentName).toMatch(/CollectionField/i);
        });

        it('should render userId of each item as a picker using the collection options link', () => {
            const mockConf = createMockConf({
                prop: {name: 'legalGuardians.0', prompt: 'Zástupci', type: 'LegalGuardianInputRequest', options: optionsLink},
            });

            render(klabisFieldsFactory('LegalGuardianInputRequest', mockConf)!);

            expect(screen.getByTestId('select-name')).toHaveTextContent('parent.userId');
            expect(screen.getByTestId('select-href'))
                .toHaveTextContent('http://localhost:8443/api/legal-guardian-options');
        });
    });

    describe('PaymentRuleRequest field type', () => {

        it('should delegate to HalFormsCollectionField for multiple/collection PaymentRuleRequest', () => {
            const mockConf = createMockConf({
                prop: {name: 'rules', prompt: 'Pravidla', type: 'PaymentRuleRequest', multiple: true},
            });

            const result = klabisFieldsFactory('PaymentRuleRequest', mockConf);
            // multiple=true falls through to halFormsFieldsFactory which returns HalFormsCollectionField
            expect(result).not.toBeNull();
            const element = result as React.ReactElement;
            const componentName = (element.type as {displayName?: string; name?: string}).displayName
                ?? (element.type as {displayName?: string; name?: string}).name;
            expect(componentName).toMatch(/CollectionField/i);
        });

        it('should render eventTypeId as a select with event type options', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'rules.0', prompt: 'Pravidlo', type: 'PaymentRuleRequest'},
                subElementProps: mockSubElementProps,
            });

            const result = klabisFieldsFactory('PaymentRuleRequest', mockConf);
            expect(result).not.toBeNull();

            render(result!);

            // subElementProps mock returns "parent.<attr>" as the field name
            expect(screen.getByTestId('hal-select-parent.eventTypeId')).toBeInTheDocument();
        });

        it('should render ruleType as a select with PERCENTAGE and FIXED_AMOUNT options', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'rules.0', prompt: 'Pravidlo', type: 'PaymentRuleRequest'},
                subElementProps: mockSubElementProps,
            });

            const result = klabisFieldsFactory('PaymentRuleRequest', mockConf);
            render(result!);

            expect(screen.getByTestId('hal-select-parent.ruleType')).toBeInTheDocument();
        });

        it('should render rankingShortName, percent, fixedAmount, fixedCurrency as inputs', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'rules.0', prompt: 'Pravidlo', type: 'PaymentRuleRequest'},
                subElementProps: mockSubElementProps,
            });

            const result = klabisFieldsFactory('PaymentRuleRequest', mockConf);
            render(result!);

            expect(screen.getByTestId('hal-input-parent.rankingShortName')).toBeInTheDocument();
            expect(screen.getByTestId('hal-input-parent.percent')).toBeInTheDocument();
            expect(screen.getByTestId('hal-input-parent.fixedAmount')).toBeInTheDocument();
            expect(screen.getByTestId('hal-input-parent.fixedCurrency')).toBeInTheDocument();
        });

        it('should populate eventTypeId select with options from event types API', () => {
            const mockSubElementProps = createMockSubElementProps();
            const mockConf = createMockConf({
                prop: {name: 'rules.0', prompt: 'Pravidlo', type: 'PaymentRuleRequest'},
                subElementProps: mockSubElementProps,
            });

            const result = klabisFieldsFactory('PaymentRuleRequest', mockConf);
            render(result!);

            const eventTypeSelect = screen.getByTestId('hal-select-parent.eventTypeId');
            expect(eventTypeSelect).toBeInTheDocument();
        });
    });

    describe('MembershipFeeTierMultiSelect field type', () => {

        it('should render HalFormsCheckboxGroup for MembershipFeeTierMultiSelect type', () => {
            const mockConf = createMockConf({
                prop: {name: 'tierIds', prompt: 'Výběr tarifů', type: 'MembershipFeeTierMultiSelect'},
            });

            const result = klabisFieldsFactory('MembershipFeeTierMultiSelect', mockConf);
            expect(result).not.toBeNull();

            render(result!);
            expect(screen.getByTestId('hal-forms-checkboxgroup-mock')).toBeInTheDocument();
        });

        it('should populate options from useMembershipFeeTierOptions', () => {
            const mockConf = createMockConf({
                prop: {name: 'tierIds', prompt: 'Výběr tarifů', type: 'MembershipFeeTierMultiSelect'},
            });

            const result = klabisFieldsFactory('MembershipFeeTierMultiSelect', mockConf);
            expect(result).not.toBeNull();

            render(result!);

            const checkboxGroup = screen.getByTestId('hal-forms-checkboxgroup-mock');
            expect(checkboxGroup).toBeInTheDocument();
        });

        it('should preserve original prop name and prompt', () => {
            const mockConf = createMockConf({
                prop: {name: 'tierIds', prompt: 'Výběr tarifů', type: 'MembershipFeeTierMultiSelect'},
            });

            const result = klabisFieldsFactory('MembershipFeeTierMultiSelect', mockConf);
            render(result!);

            expect(screen.getByTestId('checkboxgroup-name')).toHaveTextContent('tierIds');
            expect(screen.getByTestId('checkboxgroup-prompt')).toHaveTextContent('Výběr tarifů');
        });
    });

    describe('fallback behavior', () => {

        it('should return null for unknown field types', () => {
            const mockConf = createMockConf({
                prop: {name: 'test', type: 'unknownType'},
            });

            const result = klabisFieldsFactory('unknownType', mockConf);
            expect(result).toBeNull();
        });
    });

    describe('createMemberFilteredFactory', () => {
        it('returns klabisFieldsFactory itself when no filter is provided', () => {
            expect(createMemberFilteredFactory()).toBe(klabisFieldsFactory);
        });

        it('applies excludeIds to a single MemberId field', () => {
            const factory = createMemberFilteredFactory(['1']);
            const mockConf = createMockConf({
                prop: {name: 'memberId', prompt: 'Vedoucí', type: 'MemberId'},
            });

            const result = factory('MemberId', mockConf);
            render(result!);

            expect(screen.getByTestId('hal-forms-memberid-mock')).toBeInTheDocument();
            expect(screen.getByTestId('select-excluded')).toHaveTextContent('1');
        });

        it('applies excludeIds per row when the property is multi, via HalFormsCollectionField', () => {
            const factory = createMemberFilteredFactory(['1']);
            const mockConf = createMockConf({
                prop: {name: 'memberIds', prompt: 'Členové', type: 'MemberId', multiple: true},
            });

            const result = factory('MemberId', mockConf);
            render(
                <Formik initialValues={{memberIds: ['2', '3']}} onSubmit={vi.fn()}>
                    <Form>{result}</Form>
                </Formik>
            );

            expect(screen.getAllByTestId('hal-forms-memberid-mock')).toHaveLength(2);
        });

        it('applies the filter to a UserId field as well, so already-in-group parents are hidden', () => {
            const factory = createMemberFilteredFactory(['1']);
            const mockConf = createMockConf({
                prop: {name: 'userId', prompt: 'Rodič', type: 'UserId'},
            });

            const result = factory('UserId', mockConf);
            expect(result).not.toBeNull();
            render(result!);

            expect(screen.getByTestId('hal-forms-memberid-mock')).toBeInTheDocument();
            expect(screen.getByTestId('select-excluded')).toHaveTextContent('1');
        });
    });
});
