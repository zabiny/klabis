import type {Meta, StoryObj} from '@storybook/react-vite'
import {FormHelperText} from './FormControl'

const meta = {
    title: 'Components/Forms/FormHelperText',
    component: FormHelperText,
    tags: ['autodocs'],
    argTypes: {
        error: {control: 'boolean'},
    },
    args: {
        children: 'Registrační číslo přidělí klub při registraci',
    },
} satisfies Meta<typeof FormHelperText>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Error: Story = {
    args: {
        error: true,
        children: 'Registrační číslo musí mít formát ZBM1234',
    },
}
