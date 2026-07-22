import type {Meta, StoryObj} from '@storybook/react-vite'
import {ErrorDisplay} from './ErrorDisplay'

const meta = {
    title: 'Components/ErrorDisplay',
    component: ErrorDisplay,
    tags: ['autodocs'],
    argTypes: {
        title: {control: 'text'},
        customMessage: {control: 'text'},
        subtitle: {control: 'text'},
        retryText: {control: 'text'},
        cancelText: {control: 'text'},
        isValidationError: {control: 'boolean'},
    },
    args: {
        error: new Error('Nepodařilo se načíst data člena.'),
        title: 'Chyba při načítání',
    },
} satisfies Meta<typeof ErrorDisplay>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const WithCustomMessage: Story = {
    args: {
        error: new Error('500 Internal Server Error'),
        title: 'Nepodařilo se uložit člena',
        customMessage: 'Server momentálně neodpovídá, zkuste to prosím znovu za chvíli.',
    },
}

export const WithSubtitleAndActions: Story = {
    args: {
        error: new Error('Spojení s API bylo přerušeno'),
        title: 'Chyba načtení tréninkové skupiny',
        subtitle: 'Endpoint: GET /api/training-groups/42',
        onRetry: () => alert('Retry'),
        onCancel: () => alert('Cancel'),
    },
}

export const WithValidationErrors: Story = {
    args: {
        error: Object.assign(new Error('Formulář obsahuje chyby'), {
            validationErrors: {
                email: 'Neplatný formát e-mailové adresy',
                dateOfBirth: 'Datum narození je povinné',
            },
        }),
        title: 'Odeslání formuláře selhalo',
        isValidationError: true,
        onRetry: () => alert('Retry'),
        onCancel: () => alert('Cancel'),
    },
}

export const NoError: Story = {
    args: {
        error: null,
    },
}
