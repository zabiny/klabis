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
	Spinner: () => <div data-testid="spinner">Loading...</div>,
}));

jest.mock('../api/hateoas', () => ({
	isFormValidationError: jest.fn((error) => {
		return error && error.validationErrors !== undefined;
	}),
}));

jest.mock('../hooks/useHalFormData', () => ({
	useHalFormData: jest.fn(),
}));

jest.mock('../contexts/HalRouteContext', () => ({
	useHalRoute: jest.fn(),
}));

jest.mock('./KlabisFieldsFactory.tsx', () => ({
	klabisFieldsFactory: {},
}));

const {useHalFormData} = require('../hooks/useHalFormData');
const {useHalRoute} = require('../contexts/HalRouteContext');

describe('HalFormsSection Component', () => {
	const mockTemplate = (overrides = {}): HalFormsTemplate => mockHalFormsTemplate(overrides);

	beforeEach(() => {
		// Default mock implementations
		useHalRoute.mockReturnValue({
			pathname: '/members/123',
		});

		useHalFormData.mockReturnValue({
			formData: {},
			isLoadingTargetData: false,
			targetFetchError: null,
			refetchTargetData: jest.fn(),
		});

		jest.clearAllMocks();
	});

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

	describe('Target Data Fetching', () => {
		it('should show loading indicator when fetching target data', () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: true,
				targetFetchError: null,
				refetchTargetData: jest.fn(),
			});

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

			expect(screen.getByTestId('spinner')).toBeInTheDocument();
		});

		it('should hide form while loading target data', () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: true,
				targetFetchError: null,
				refetchTargetData: jest.fn(),
			});

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

			expect(screen.queryByTestId('hal-forms-form')).not.toBeInTheDocument();
		});

		it('should show error alert when target fetch fails', () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			const fetchError = new Error('Failed to load data');
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: false,
				targetFetchError: fetchError,
				refetchTargetData: jest.fn(),
			});

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

			expect(screen.getByTestId('alert-error')).toBeInTheDocument();
			expect(screen.getByText('Failed to load data')).toBeInTheDocument();
		});

		it('should hide form when target fetch fails', () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			const fetchError = new Error('Failed to load data');
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: false,
				targetFetchError: fetchError,
				refetchTargetData: jest.fn(),
			});

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

			expect(screen.queryByTestId('hal-forms-form')).not.toBeInTheDocument();
		});

		it('should show retry button on fetch error', () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			const fetchError = new Error('Network error');
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: false,
				targetFetchError: fetchError,
				refetchTargetData: jest.fn(),
			});

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

			expect(screen.getByText('Zkusit znovu')).toBeInTheDocument();
		});

		it('should call refetchTargetData when retry button is clicked', async () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			const fetchError = new Error('Network error');
			const mockRefetch = jest.fn();
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: false,
				targetFetchError: fetchError,
				refetchTargetData: mockRefetch,
			});

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

			const retryButton = screen.getByText('Zkusit znovu');
			await userEvent.click(retryButton);

			expect(mockRefetch).toHaveBeenCalled();
		});

		it('should show cancel button on fetch error', () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			const fetchError = new Error('Network error');
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: false,
				targetFetchError: fetchError,
				refetchTargetData: jest.fn(),
			});

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

			expect(screen.getByText('Zrušit')).toBeInTheDocument();
		});

		it('should call onSelectTemplate with null when cancel button is clicked on error', async () => {
			const onSelectTemplate = jest.fn();
			const selectedTemplate = mockTemplate({target: '/events/456'});
			const fetchError = new Error('Network error');
			useHalFormData.mockReturnValue({
				formData: null,
				isLoadingTargetData: false,
				targetFetchError: fetchError,
				refetchTargetData: jest.fn(),
			});

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

			const cancelButton = screen.getByText('Zrušit');
			await userEvent.click(cancelButton);

			expect(onSelectTemplate).toHaveBeenCalledWith(null);
		});

		it('should display form with target data when fetch succeeds', () => {
			const selectedTemplate = mockTemplate({target: '/events/456'});
			const targetData = {id: 2, name: 'Target Event'};
			useHalFormData.mockReturnValue({
				formData: targetData,
				isLoadingTargetData: false,
				targetFetchError: null,
				refetchTargetData: jest.fn(),
			});

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
			expect(screen.queryByTestId('spinner')).not.toBeInTheDocument();
			expect(screen.queryByTestId('alert-error')).not.toBeInTheDocument();
		});

		it('should use current resource data when target equals current path', () => {
			const selectedTemplate = mockTemplate({target: '/members/123'});
			const currentData = {id: 1, name: 'Current Member'};
			useHalFormData.mockReturnValue({
				formData: currentData,
				isLoadingTargetData: false,
				targetFetchError: null,
				refetchTargetData: jest.fn(),
			});

			render(
				<HalFormsSection
					{...createDefaultProps({
						templates: {create: selectedTemplate},
						data: currentData,
						formState: {
							selectedTemplate,
							submitError: null,
							isSubmitting: false,
						},
					})}
				/>,
			);

			expect(screen.getByTestId('hal-forms-form')).toBeInTheDocument();
			expect(screen.queryByTestId('spinner')).not.toBeInTheDocument();
		});
	});
});
