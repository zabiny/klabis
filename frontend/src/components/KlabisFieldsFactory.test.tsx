import {render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import {klabisFieldsFactory} from './KlabisFieldsFactory';
import type {HalFormsInputProps} from './HalNavigator2/halforms';

vi.mock('./HalNavigator2/halforms/fields', async () => {
    const actual = await vi.importActual('./HalNavigator2/halforms/fields');
    return {
        ...(actual as object),
        HalFormsInput: ({prop}: HalFormsInputProps) => (
            <div data-testid={`hal-input-${prop.name}`}>{prop.prompt}</div>
        ),
        HalFormsMemberId: ({prop, errorText}: HalFormsInputProps) => (
            <div data-testid="hal-forms-memberid-mock">
                <span data-testid="select-name">{prop.name}</span>
                <span data-testid="select-prompt">{prop.prompt}</span>
                {prop.options?.link?.href && (
                    <span data-testid="select-href">{prop.options.link.href}</span>
                )}
                {errorText && <span data-testid="select-error">{errorText}</span>}
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
                expect((componentType as any).displayName || (componentType as any).name).toMatch(/HalFormsMemberId/i);
            }
        });

        it('should configure remote options pointing to /members/options endpoint', () => {
            const mockConf = createMockConf({
                prop: {name: 'memberId', prompt: 'Choose Member', type: 'MemberId'},
            });

            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            expect(screen.getByTestId('hal-forms-memberid-mock')).toBeInTheDocument();
            expect(screen.getByTestId('select-href')).toHaveTextContent('/members/options');
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

            expect(mockSubElementProps).toHaveBeenCalledWith('completionDate', {prompt: 'Datum absolvování', type: 'date'});
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

            expect(mockSubElementProps).toHaveBeenCalledWith('licenseNumber', {prompt: 'Číslo licence'});
            expect(mockSubElementProps).toHaveBeenCalledWith('validityDate', {prompt: 'Platnost', type: 'date'});
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
});
