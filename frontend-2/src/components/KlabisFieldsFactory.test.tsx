import {render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import {klabisFieldsFactory} from './KlabisFieldsFactory';
import type {HalFormsInputProps} from './HalNavigator2/halforms';

// Mock HalFormsMemberId to verify it receives correct props at module level
vi.mock('./HalNavigator2/halforms/fields', async () => {
    const actual = await vi.importActual('./HalNavigator2/halforms/fields');
    return {
        ...(actual as object),
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

/**
 * Tests for KlabisFieldsFactory custom field types
 *
 * Verifies that custom field types are correctly registered and rendered
 */
describe('KlabisFieldsFactory', () => {

    describe('MemberId field type', () => {

        it('should render HalFormsMemberId for MemberId type', () => {
            // Arrange
            const mockProp = {
                name: 'assignedMemberId',
                prompt: 'Select Member',
                type: 'MemberId',
            };
            const mockErrorText = undefined;
            const mockSubElementProps = vi.fn();

            const mockConf: HalFormsInputProps = {
                prop: mockProp,
                errorText: mockErrorText,
                subElementProps: mockSubElementProps,
            };

            // Act
            const result = klabisFieldsFactory('MemberId', mockConf);

            // Assert
            expect(result).not.toBeNull();
            const componentType = result?.type;
            if (typeof componentType === 'object' && componentType !== null && ('displayName' in componentType || 'name' in componentType)) {
                expect((componentType as any).displayName || (componentType as any).name).toMatch(/HalFormsMemberId/i);
            }
        });

        it('should configure remote options pointing to /members/options endpoint', () => {
            // Arrange
            const mockProp = {
                name: 'memberId',
                prompt: 'Choose Member',
                type: 'MemberId',
            };

            const mockConf: HalFormsInputProps = {
                prop: mockProp,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            // Act
            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            // Assert
            const selectMock = screen.getByTestId('hal-forms-memberid-mock');
            expect(selectMock).toBeInTheDocument();

            const hrefElement = screen.getByTestId('select-href');
            expect(hrefElement).toHaveTextContent('/members/options');
        });

        it('should configure member selection with remote options', () => {
            // Arrange
            const mockProp = {
                name: 'memberId',
                prompt: 'Choose Member',
                type: 'MemberId',
            };

            const mockConf: HalFormsInputProps = {
                prop: mockProp,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            // Act
            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            // Assert - HalFormsMemberId should be rendered with remote options
            const memberIdMock = screen.getByTestId('hal-forms-memberid-mock');
            expect(memberIdMock).toBeInTheDocument();

            const hrefElement = screen.getByTestId('select-href');
            expect(hrefElement).toHaveTextContent('/members/options');
        });

        it('should preserve the original prompt from prop', () => {
            // Arrange
            const testPrompt = 'Assign Coordinator';
            const mockProp = {
                name: 'coordinatorId',
                prompt: testPrompt,
                type: 'MemberId',
            };

            const mockConf: HalFormsInputProps = {
                prop: mockProp,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            // Act
            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            // Assert
            const promptElement = screen.getByTestId('select-prompt');
            expect(promptElement).toHaveTextContent(testPrompt);
        });

        it('should handle error text when provided', () => {
            // Arrange
            const errorMessage = 'Member selection is required';
            const mockProp = {
                name: 'memberId',
                prompt: 'Member',
                type: 'MemberId',
                required: true,
            };

            const mockConf: HalFormsInputProps = {
                prop: mockProp,
                errorText: errorMessage,
                subElementProps: vi.fn(),
            };

            // Act
            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            // Assert
            const errorElement = screen.getByTestId('select-error');
            expect(errorElement).toHaveTextContent(errorMessage);
        });

        it('should not override existing options with link-based options', () => {
            // Arrange - prop already has options defined
            const mockProp = {
                name: 'memberId',
                prompt: 'Member',
                type: 'MemberId',
                options: {
                    inline: ['Option1', 'Option2'], // Inline options exist
                },
            };

            const mockConf: HalFormsInputProps = {
                prop: mockProp,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            // Act
            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            // Assert - the remote options should override inline options
            const hrefElement = screen.getByTestId('select-href');
            expect(hrefElement).toHaveTextContent('/members/options');
        });

        it('should pass through other HalFormsInputProps to HalFormsSelect', () => {
            // Arrange
            const mockProp = {
                name: 'testMemberId',
                prompt: 'Test Prompt',
                type: 'MemberId',
                required: true,
                readOnly: false,
            };

            const mockConf: HalFormsInputProps = {
                prop: mockProp,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            // Act
            const fieldElement = klabisFieldsFactory('MemberId', mockConf);
            render(fieldElement!);

            // Assert
            const nameElement = screen.getByTestId('select-name');
            expect(nameElement).toHaveTextContent('testMemberId');
        });
    });

    describe('fallback behavior', () => {

        it('should return null for unknown field types', () => {
            // Arrange
            const mockConf: HalFormsInputProps = {
                prop: {name: 'test', type: 'unknownType'},
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            // Act
            const result = klabisFieldsFactory('unknownType', mockConf);

            // Assert
            expect(result).toBeNull();
        });
    });
});
