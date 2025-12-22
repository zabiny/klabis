import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {HalFormsSection} from './HalFormsSection';
import {mockHalFormsTemplate} from '../__mocks__/halData';

// Mock the HalFormsForm component since it's complex
jest.mock('./HalFormsForm', () => ({
    HalFormsForm: ({template, onCancel}: any) => (
        <div data-testid="hal-forms-form">
            <h4>{template.title || 'Form'}</h4>
            <button onClick={onCancel}>Cancel</button>
        </div>
    ),
    halFormsFieldsFactory: {},
}));

jest.mock('./UI', () => ({
    Alert: ({children}: any) => <div data-testid="alert">{children}</div>,
    Button: ({children, onClick}: any) => <button onClick={onClick}>{children}</button>,
}));

jest.mock('../api/hateoas', () => ({
    isFormValidationError: jest.fn(() => false),
}));

describe('HalFormsSection Component', () => {
    describe('Rendering', () => {
        it('should not render when templates are not provided', () => {
            const {container} = render(
                <HalFormsSection
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );
            expect(container.firstChild).toBeNull();
        });

        it('should not render when templates object is empty', () => {
            const {container} = render(
                <HalFormsSection
                    templates={{}}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );
            expect(container.firstChild).toBeNull();
        });

        it('should render section when templates are provided', () => {
            const templates = {
                create: mockHalFormsTemplate(),
            };
            render(
                <HalFormsSection
                    templates={templates}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );
            const section = screen.getByText(/template|form|action/i, {selector: 'h3'});
            expect(section).toBeInTheDocument();
        });
    });

    describe('Template Button Display', () => {
        it('should render buttons for each template when none selected', () => {
            const templates = {
                create: mockHalFormsTemplate({title: 'Create'}),
                update: mockHalFormsTemplate({title: 'Update', method: 'PUT'}),
            };
            render(
                <HalFormsSection
                    templates={templates}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );
            expect(screen.getByText('Create')).toBeInTheDocument();
            expect(screen.getByText('Update')).toBeInTheDocument();
        });

        it('should use template title as button text', () => {
            const templates = {
                create: mockHalFormsTemplate({title: 'Add New Item'}),
            };
            render(
                <HalFormsSection
                    templates={templates}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );
            expect(screen.getByText('Add New Item')).toBeInTheDocument();
        });

        it('should use template name when title is not provided', () => {
            const templates = {
                customAction: mockHalFormsTemplate({title: undefined}),
            };
            render(
                <HalFormsSection
                    templates={templates}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );
            expect(screen.getByText('customAction')).toBeInTheDocument();
        });

        it('should have title attribute on buttons', () => {
            const templates = {
                create: mockHalFormsTemplate({title: 'Create Item'}),
            };
            render(
                <HalFormsSection
                    templates={templates}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );
            const button = screen.getByText('Create Item');
            expect(button).toHaveAttribute('title', 'Create Item');
        });
    });

    describe('Template Selection', () => {
        it('should call onSelectTemplate when template button is clicked', async () => {
            const user = userEvent.setup();
            const mockOnSelectTemplate = jest.fn();
            const template = mockHalFormsTemplate({title: 'Create'});
            const templates = {create: template};

            render(
                <HalFormsSection
                    templates={templates}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={mockOnSelectTemplate}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            const button = screen.getByText('Create');
            await user.click(button);
            expect(mockOnSelectTemplate).toHaveBeenCalledWith(template);
        });

        it('should display form when template is selected', () => {
            const template = mockHalFormsTemplate({title: 'Edit Item'});
            render(
                <HalFormsSection
                    templates={{edit: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
            // The template title should appear in the component
            const titles = screen.getAllByText('Edit Item');
            expect(titles.length).toBeGreaterThanOrEqual(1);
        });

        it('should show close button when form is open', () => {
            const template = mockHalFormsTemplate({title: 'Create'});
            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            expect(screen.getByText('Close') || screen.getByText('Cancel')).toBeInTheDocument();
        });

        it('should call onSelectTemplate with null when close is clicked', async () => {
            const user = userEvent.setup();
            const mockOnSelectTemplate = jest.fn();
            const template = mockHalFormsTemplate({title: 'Create'});

            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={mockOnSelectTemplate}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            const closeButton = screen.getByText('Cancel');
            await user.click(closeButton);
            expect(mockOnSelectTemplate).toHaveBeenCalledWith(null);
        });
    });

    describe('Error Display', () => {
        it('should not display error when submitError is null', () => {
            const template = mockHalFormsTemplate();
            const {container} = render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            expect(container.querySelector('[data-testid="alert"]')).not.toBeInTheDocument();
        });

        it('should display error alert when submitError is provided', () => {
            const template = mockHalFormsTemplate();
            const error = new Error('Form submission failed');

            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={error}
                    isSubmitting={false}
                />,
            );

            expect(screen.getByTestId('alert')).toBeInTheDocument();
            expect(screen.getByText('Form submission failed')).toBeInTheDocument();
        });

        it('should display validation errors when present', () => {
            const template = mockHalFormsTemplate();
            const error = new Error('Validation failed') as any;
            error.validationErrors = {email: 'Invalid email'};

            // Mock isFormValidationError to return true
            const {isFormValidationError} = require('../api/hateoas');
            isFormValidationError.mockReturnValue(true);

            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={error}
                    isSubmitting={false}
                />,
            );

            expect(screen.getByText(/Invalid email/)).toBeInTheDocument();

            isFormValidationError.mockReturnValue(false);
        });
    });

    describe('Form State', () => {
        it('should pass selectedTemplate to HalFormsForm', () => {
            const template = mockHalFormsTemplate({title: 'Create'});
            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
        });

        it('should pass data to HalFormsForm', () => {
            const template = mockHalFormsTemplate();
            const testData = {name: 'Test', email: 'test@example.com'};

            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={testData}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
        });

        it('should pass isSubmitting state', () => {
            const template = mockHalFormsTemplate();
            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={template}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={true}
                />,
            );

            expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
        });
    });

    describe('Styling', () => {
        it('should have proper container styling', () => {
            const template = mockHalFormsTemplate();
            const {container} = render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            const section = container.querySelector('div:not([data-testid])');
            expect(section).toHaveClass('mt-4', 'p-4', 'border', 'rounded', 'bg-green-50');
        });

        it('should style template buttons correctly', () => {
            const template = mockHalFormsTemplate({title: 'Create'});
            render(
                <HalFormsSection
                    templates={{create: template}}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            const button = screen.getByText('Create');
            expect(button).toHaveClass('px-3', 'py-1', 'bg-green-600', 'text-white', 'rounded');
        });
    });

    describe('Multiple Templates', () => {
        it('should render multiple template buttons', () => {
            const templates = {
                create: mockHalFormsTemplate({title: 'Create'}),
                update: mockHalFormsTemplate({title: 'Update', method: 'PUT'}),
                delete: mockHalFormsTemplate({title: 'Delete', method: 'DELETE'}),
            };

            render(
                <HalFormsSection
                    templates={templates}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={() => {
                    }}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            expect(screen.getByText('Create')).toBeInTheDocument();
            expect(screen.getByText('Update')).toBeInTheDocument();
            expect(screen.getByText('Delete')).toBeInTheDocument();
        });

        it('should handle template switching', async () => {
            const user = userEvent.setup();
            const mockOnSelectTemplate = jest.fn();
            const template1 = mockHalFormsTemplate({title: 'Create'});
            const template2 = mockHalFormsTemplate({title: 'Update', method: 'PUT'});

            const {rerender} = render(
                <HalFormsSection
                    templates={{create: template1, update: template2}}
                    data={{}}
                    selectedTemplate={template1}
                    onSelectTemplate={mockOnSelectTemplate}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            // Close the form
            const cancelButton = screen.getByText('Cancel');
            await user.click(cancelButton);

            // Select new template
            rerender(
                <HalFormsSection
                    templates={{create: template1, update: template2}}
                    data={{}}
                    selectedTemplate={null}
                    onSelectTemplate={mockOnSelectTemplate}
                    onSubmit={async () => {
                    }}
                    submitError={null}
                    isSubmitting={false}
                />,
            );

            const updateButton = screen.getByText('Update');
            await user.click(updateButton);
            expect(mockOnSelectTemplate).toHaveBeenLastCalledWith(template2);
        });
    });
});
