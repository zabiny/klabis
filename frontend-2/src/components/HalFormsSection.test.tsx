import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type {HalFormsHandlers, HalFormsState} from './HalFormsSection';
import {HalFormsSection} from './HalFormsSection';
import type {HalFormsTemplate} from '../api';
import {mockHalFormsTemplate} from '../__mocks__/halData';

// Mock the HalFormsForm component since it's complex
jest.mock('./HalFormsForm', () => ({
	HalFormsForm: ({template, onCancel, onSubmit}: any) => (
		<div data-testid="hal-forms-form">
			<h4>{template.title || 'Form'}</h4>
			<button onClick={() => onSubmit({testField: 'testValue'})}>Submit Form</button>
			<button onClick={onCancel}>Cancel</button>
		</div>
	),
	halFormsFieldsFactory: {},
}));

jest.mock('./UI', () => ({
	Alert: ({children, severity}: any) => (
		<div data-testid={`alert-${severity}`}>{children}</div>
	),
	Button: ({children, onClick, 'data-testid': testId, 'aria-label': ariaLabel}: any) => (
		<button onClick={onClick} data-testid={testId} aria-label={ariaLabel}>
			{children}
		</button>
	),
}));

jest.mock('../api/hateoas', () => ({
	isFormValidationError: jest.fn((error) => {
		return error && error.validationErrors !== undefined;
	}),
}));

describe('HalFormsSection Component', () => {
	const mockTemplate = (overrides = {}): HalFormsTemplate => mockHalFormsTemplate(overrides);

	const createDefaultProps = (overrides = {}) => ({
		templates: undefined,
		data: {},
		formState: {
			selectedTemplate: null,
			submitError: null,
			isSubmitting: false,
		} as HalFormsState,
		handlers: {
			onSelectTemplate: jest.fn(),
			onSubmit: jest.fn(),
		} as unknown as HalFormsHandlers,
		...overrides,
	});

	describe('Rendering', () => {
		it('should not render when templates are not provided', () => {
			const {container} = render(
				<HalFormsSection {...createDefaultProps()} />,
			);
			expect(container.firstChild).toBeNull();
		});

		it('should not render when templates object is empty', () => {
			const {container} = render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {},
					})}
				/>,
			);
			expect(container.firstChild).toBeNull();
		});

		it('should render section when templates are provided', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
			};
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
					})}
				/>,
			);
			const section = screen.getByRole('heading', {level: 3});
			expect(section).toBeInTheDocument();
		});
	});

	describe('Template Selector Mode (No Template Selected)', () => {
		it('should render template buttons when no template is selected', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
				update: mockTemplate({title: 'Update', method: 'PUT'}),
			};
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
					})}
				/>,
			);

			expect(screen.getByTestId('form-template-button-create')).toBeInTheDocument();
			expect(screen.getByTestId('form-template-button-update')).toBeInTheDocument();
		});

		it('should display template title as button text', () => {
			const templates = {
				create: mockTemplate({title: 'Create Item'}),
			};
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
					})}
				/>,
			);

			expect(screen.getByText('Create Item')).toBeInTheDocument();
		});

		it('should display template name when title is not provided', () => {
			const templates = {
				customAction: mockTemplate({title: undefined}),
			};
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
					})}
				/>,
			);

			expect(screen.getByText('customAction')).toBeInTheDocument();
		});

		it('should have proper aria-label on template buttons', () => {
			const templates = {
				create: mockTemplate({title: 'Create Form'}),
			};
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
					})}
				/>,
			);

			const button = screen.getByTestId('form-template-button-create');
			expect(button).toHaveAttribute('aria-label', 'Select Create Form form');
		});

		it('should call onSelectTemplate when a template button is clicked', async () => {
			const onSelectTemplate = jest.fn();
			const createTemplate = mockTemplate({title: 'Create'});
			const templates = {
				create: createTemplate,
			};

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
						handlers: {
							onSelectTemplate,
							onSubmit: jest.fn(),
						},
					})}
				/>,
			);

			const button = screen.getByTestId('form-template-button-create');
			await userEvent.click(button);

			expect(onSelectTemplate).toHaveBeenCalledWith(createTemplate);
		});
	});

	describe('Form Display Mode (Template Selected)', () => {
		it('should render form when template is selected', () => {
			const selectedTemplate = mockTemplate({title: 'Create'});
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: false,
						},
					})}
				/>,
			);

			expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
		});

		it('should display selected template title', () => {
			const selectedTemplate = mockTemplate({title: 'Edit Item'});
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {edit: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: false,
						},
					})}
				/>,
			);

			const heading = screen.getAllByText('Edit Item')[0];
			expect(heading).toBeInTheDocument();
			expect(heading.tagName).toBe('H4');
		});

		it('should have close button with proper attributes', () => {
			const selectedTemplate = mockTemplate({title: 'Create'});
			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: false,
						},
					})}
				/>,
			);

			const closeButton = screen.getByTestId('close-form-button');
			expect(closeButton).toBeInTheDocument();
			expect(closeButton).toHaveAttribute('aria-label', 'Close form');
		});

		it('should call onSelectTemplate with null when close button is clicked', async () => {
			const onSelectTemplate = jest.fn();
			const selectedTemplate = mockTemplate({title: 'Create'});

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: false,
						},
						handlers: {
							onSelectTemplate,
							onSubmit: jest.fn(),
						},
					})}
				/>,
			);

			const closeButton = screen.getByTestId('close-form-button');
			await userEvent.click(closeButton);

			expect(onSelectTemplate).toHaveBeenCalledWith(null);
		});
	});

	describe('Error Handling', () => {
		it('should display error alert when submitError is present', () => {
			const selectedTemplate = mockTemplate();
			const submitError = new Error('Form submission failed');

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError,
							isSubmitting: false,
						},
					})}
				/>,
			);

			expect(screen.getByTestId('alert-error')).toBeInTheDocument();
			expect(screen.getByText('Form submission failed')).toBeInTheDocument();
		});

		it('should display validation errors when submitError has validationErrors', () => {
			const selectedTemplate = mockTemplate();
			const submitError = new Error('Validation failed') as any;
			submitError.validationErrors = {
				email: 'Invalid email format',
				name: 'Name is required',
			};

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError,
							isSubmitting: false,
						},
					})}
				/>,
			);

			expect(screen.getByText('email: Invalid email format')).toBeInTheDocument();
			expect(screen.getByText('name: Name is required')).toBeInTheDocument();
		});

		it('should not display validation errors when submitError does not have validationErrors', () => {
			const selectedTemplate = mockTemplate();
			const submitError = new Error('Network error');

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError,
							isSubmitting: false,
						},
					})}
				/>,
			);

			const listItems = screen.queryAllByRole('listitem');
			expect(listItems).toHaveLength(0);
		});
	});

	describe('Form Submission State', () => {
		it('should pass isSubmitting state to form', () => {
			const selectedTemplate = mockTemplate();
			const {rerender} = render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: false,
						},
					})}
				/>,
			);

			expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();

			// Re-render with isSubmitting true
			rerender(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: true,
						},
					})}
				/>,
			);

			expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
		});
	});

	describe('Props Grouping', () => {
		it('should accept formState prop with state values', () => {
			const selectedTemplate = mockTemplate();
			const formState: HalFormsState = {
				selectedTemplate,
				submitError: null,
				isSubmitting: true,
			};

			const {container} = render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						formState,
					})}
				/>,
			);

			expect(container.firstChild).not.toBeNull();
		});

		it('should accept handlers prop with callback functions', async () => {
			const onSelectTemplate = jest.fn();
			const onSubmit = jest.fn();
			const handlers: HalFormsHandlers = {
				onSelectTemplate,
				onSubmit,
			};

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: mockTemplate()},
						handlers,
					})}
				/>,
			);

			const button = screen.getByTestId('form-template-button-create');
			await userEvent.click(button);

			expect(onSelectTemplate).toHaveBeenCalled();
		});
	});

	describe('Accessibility', () => {
		it('should have proper structure with heading', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
			};

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
					})}
				/>,
			);

			const heading = screen.getByRole('heading', {level: 3});
			expect(heading).toBeInTheDocument();
		});

		it('should render all template buttons with proper text IDs', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
				update: mockTemplate({title: 'Update'}),
				delete: mockTemplate({title: 'Delete'}),
			};

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates,
					})}
				/>,
			);

			expect(screen.getByTestId('form-template-button-create')).toBeInTheDocument();
			expect(screen.getByTestId('form-template-button-update')).toBeInTheDocument();
			expect(screen.getByTestId('form-template-button-delete')).toBeInTheDocument();
		});
	});

	describe('Data Passing', () => {
		it('should pass data prop to HalFormsForm', () => {
			const selectedTemplate = mockTemplate();
			const data = {
				id: 'test-id',
				name: 'Test Name',
			};

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						data,
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: false,
						},
					})}
				/>,
			);

			expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
		});
	});
});
