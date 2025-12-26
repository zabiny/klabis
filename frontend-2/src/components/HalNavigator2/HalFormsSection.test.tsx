import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {HalFormsSection} from './HalFormsSection.tsx';
import type {HalFormsTemplate} from '../../api';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import * as HalRouteContext from '../../contexts/HalRouteContext';

// Mock the HalFormButton component since we're testing HalFormsSection in isolation
jest.mock('./HalFormButton.tsx', () => ({
	HalFormButton: ({name}: { name: string; modal: boolean }) => (
		<button data-testid={`hal-form-button-${name}`}>
			{name}
		</button>
	),
}));

describe('HalFormsSection Component', () => {
	const mockTemplate = (overrides = {}): HalFormsTemplate => mockHalFormsTemplate(overrides);

	beforeEach(() => {
		jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
			resourceData: null,
			isLoading: false,
			error: null,
			refetch: async () => {
			},
			pathname: '/test',
			queryState: 'success',
		});
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	describe('Rendering', () => {
		it('should not render when templates are not provided (no resourceData._templates)', () => {
			const {container} = render(<HalFormsSection/>);
			expect(container.firstChild).toBeNull();
		});

		it('should render when resourceData._templates is provided automatically', () => {
			jest.spyOn(HalRouteContext, 'useHalRoute').mockReturnValue({
				resourceData: {
					_templates: {
						create: mockTemplate({title: 'Create'}),
					},
				} as any,
				isLoading: false,
				error: null,
				refetch: async () => {
				},
				pathname: '/test',
				queryState: 'success',
			});

			render(<HalFormsSection/>);

			const heading = screen.getByRole('heading', {level: 3});
			expect(heading).toBeInTheDocument();
		});

		it('should not render when templates object is empty', () => {
			const {container} = render(<HalFormsSection templates={{}}/>);
			expect(container.firstChild).toBeNull();
		});

		it('should render section when templates are provided', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
			};
			render(<HalFormsSection templates={templates}/>);

			const heading = screen.getByRole('heading', {level: 3});
			expect(heading).toBeInTheDocument();
			expect(heading).toHaveTextContent('Dostupné formuláře');
		});

		it('should render HalFormButton for each template', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
				update: mockTemplate({title: 'Update', method: 'PUT'}),
				delete: mockTemplate({title: 'Delete', method: 'DELETE'}),
			};
			render(<HalFormsSection templates={templates}/>);

			expect(screen.getByTestId('hal-form-button-create')).toBeInTheDocument();
			expect(screen.getByTestId('hal-form-button-update')).toBeInTheDocument();
			expect(screen.getByTestId('hal-form-button-delete')).toBeInTheDocument();
		});

		it('should render section with proper structure and styling', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
			};
			const {container} = render(<HalFormsSection templates={templates}/>);

			const section = container.querySelector('div.mt-4.p-4.border.border-border.rounded.bg-surface-raised');
			expect(section).toBeInTheDocument();
		});

		it('should render buttons in a flex wrap gap layout', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
				update: mockTemplate({title: 'Update'}),
			};
			const {container} = render(<HalFormsSection templates={templates}/>);

			const buttonContainer = container.querySelector('div.flex.flex-wrap.gap-2');
			expect(buttonContainer).toBeInTheDocument();
		});
	});

	describe('Multiple Templates', () => {
		it('should handle single template', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
			};
			render(<HalFormsSection templates={templates}/>);

			expect(screen.getByTestId('hal-form-button-create')).toBeInTheDocument();
			expect(screen.queryByTestId('hal-form-button-update')).not.toBeInTheDocument();
		});

		it('should handle multiple templates', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
				update: mockTemplate({title: 'Update'}),
				customAction: mockTemplate({title: 'Custom'}),
			};
			render(<HalFormsSection templates={templates}/>);

			expect(screen.getByTestId('hal-form-button-create')).toBeInTheDocument();
			expect(screen.getByTestId('hal-form-button-update')).toBeInTheDocument();
			expect(screen.getByTestId('hal-form-button-customAction')).toBeInTheDocument();
		});
	});

	describe('Accessibility', () => {
		it('should have proper heading structure', () => {
			const templates = {
				create: mockTemplate({title: 'Create'}),
			};
			render(<HalFormsSection templates={templates}/>);

			const heading = screen.getByRole('heading', {level: 3});
			expect(heading).toBeInTheDocument();
			expect(heading).toHaveClass('font-semibold', 'mb-2');
		});
	});
});
