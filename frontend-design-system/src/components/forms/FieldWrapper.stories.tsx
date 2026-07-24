import type {Meta, StoryObj} from '@storybook/react-vite'
import {FieldWrapper} from './FieldWrapper'

const meta = {
    title: 'Components/Forms/FieldWrapper',
    component: FieldWrapper,
    tags: ['autodocs'],
    args: {
        label: 'Registrační číslo',
        children: (
            <input
                className="w-full px-3 py-1.5 border rounded-md text-sm bg-surface-raised border-border"
                placeholder="ZBM1234"
            />
        ),
    },
} satisfies Meta<typeof FieldWrapper>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Required: Story = {
    args: {required: true},
}

export const WithHelpText: Story = {
    args: {
        helpText: 'Přiděluje se automaticky při registraci člena',
    },
}

export const WithError: Story = {
    args: {
        error: 'Registrační číslo musí mít formát ZBM1234',
    },
}
